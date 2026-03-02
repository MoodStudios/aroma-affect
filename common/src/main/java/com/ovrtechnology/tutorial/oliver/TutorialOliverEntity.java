package com.ovrtechnology.tutorial.oliver;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.tutorial.animation.TutorialAnimationHandler;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Tutorial Oliver - A special NPC for the tutorial experience.
 * <p>
 * Oliver can operate in different modes:
 * <ul>
 *   <li>STATIONARY - Stays in place (default)</li>
 *   <li>FOLLOWING - Follows a specific player</li>
 *   <li>WALKING_TO - Walks to a specific position</li>
 * </ul>
 * <p>
 * Characteristics:
 * <ul>
 *   <li>Invincible (ignores all damage)</li>
 *   <li>Always looks at nearby players</li>
 *   <li>Displays tutorial dialogue on interaction</li>
 *   <li>Persists forever (never despawns)</li>
 * </ul>
 */
public class TutorialOliverEntity extends Villager {

    /**
     * Oliver's behavior modes.
     */
    public enum Mode {
        STATIONARY(0),
        FOLLOWING(1),
        WALKING_TO(2);

        private final int id;

        Mode(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Mode fromId(int id) {
            for (Mode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            return STATIONARY;
        }
    }

    // NBT Tags
    private static final String TAG_DIALOGUE_ID = "DialogueId";
    private static final String TAG_TRADE_ID = "TradeId";
    private static final String TAG_MODE = "OliverMode";
    private static final String TAG_FOLLOW_TARGET = "FollowTarget";
    private static final String TAG_WALK_TARGET_X = "WalkTargetX";
    private static final String TAG_WALK_TARGET_Y = "WalkTargetY";
    private static final String TAG_WALK_TARGET_Z = "WalkTargetZ";
    private static final String TAG_HOME_X = "HomeX";
    private static final String TAG_HOME_Y = "HomeY";
    private static final String TAG_HOME_Z = "HomeZ";
    private static final String TAG_HOME_YAW = "HomeYaw";

    // Constants
    private static final int DIALOGUE_KEEPALIVE_TIMEOUT_TICKS = 100;
    private static final double DIALOGUE_MAX_DISTANCE_SQR = 8.0 * 8.0;
    private static final double FOLLOW_DISTANCE = 3.0;
    private static final double FOLLOW_START_DISTANCE = 5.0;
    private static final double WALK_ARRIVAL_DISTANCE = 1.5;
    private static final double FOLLOW_TELEPORT_DISTANCE = 15.0; // Teleport if further than this
    private static final double FOLLOW_TELEPORT_OFFSET = 2.0; // Offset behind player after teleport

    // Synced entity data
    private static final EntityDataAccessor<String> DATA_DIALOGUE_ID =
            SynchedEntityData.defineId(TutorialOliverEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TRADE_ID =
            SynchedEntityData.defineId(TutorialOliverEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(TutorialOliverEntity.class, EntityDataSerializers.INT);

    // Server-side state
    @Nullable
    private UUID dialoguePlayerId;
    private int dialogueKeepAliveTicks = 0;

    @Nullable
    private UUID followTargetId;
    @Nullable
    private BlockPos walkTarget;
    @Nullable
    private BlockPos homePos;
    private float homeYaw = 0f;

    private int pathfindCooldown = 0;

    public TutorialOliverEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal("§dOliver"));
        this.setCustomNameVisible(true);
        this.setVillagerData(this.getVillagerData()
                .withProfession(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NITWIT))
                .withLevel(5));
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DIALOGUE_ID, "default");
        builder.define(DATA_TRADE_ID, "");
        builder.define(DATA_MODE, Mode.STATIONARY.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Mode Management
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Gets Oliver's current mode.
     */
    public Mode getMode() {
        return Mode.fromId(this.entityData.get(DATA_MODE));
    }

    /**
     * Sets Oliver to stationary mode.
     */
    public void setStationary() {
        this.entityData.set(DATA_MODE, Mode.STATIONARY.getId());
        this.followTargetId = null;
        this.walkTarget = null;
        this.getNavigation().stop();
        AromaAffect.LOGGER.debug("Oliver set to STATIONARY mode");
    }

    /**
     * Sets Oliver to follow a specific player.
     *
     * @param player the player to follow
     */
    public void setFollowing(Player player) {
        this.entityData.set(DATA_MODE, Mode.FOLLOWING.getId());
        this.followTargetId = player.getUUID();
        this.walkTarget = null;
        AromaAffect.LOGGER.debug("Oliver set to FOLLOWING mode, target: {}", player.getName().getString());
    }

    /**
     * Sets Oliver to follow the nearest player.
     */
    public void setFollowingNearest() {
        Player nearest = this.level().getNearestPlayer(this, 32.0);
        if (nearest != null) {
            setFollowing(nearest);
        }
    }

    /**
     * Sets Oliver to walk to a specific position.
     *
     * @param target the target position
     */
    public void setWalkingTo(BlockPos target) {
        this.entityData.set(DATA_MODE, Mode.WALKING_TO.getId());
        this.walkTarget = target;
        this.followTargetId = null;
        AromaAffect.LOGGER.debug("Oliver set to WALKING_TO mode, target: {}", target);
    }

    /**
     * Gets the player Oliver is following.
     */
    @Nullable
    public Player getFollowTarget() {
        if (followTargetId == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getPlayerInAnyDimension(followTargetId);
        }
        return null;
    }

    /**
     * Gets the position Oliver is walking to.
     */
    @Nullable
    public BlockPos getWalkTarget() {
        return walkTarget;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Dialogue ID (synced to client)
    // ─────────────────────────────────────────────────────────────────────────────

    public String getDialogueId() {
        return this.entityData.get(DATA_DIALOGUE_ID);
    }

    public void setDialogueId(String dialogueId) {
        this.entityData.set(DATA_DIALOGUE_ID, dialogueId != null ? dialogueId : "default");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Trade ID (synced to client)
    // ─────────────────────────────────────────────────────────────────────────────

    public String getTradeId() {
        return this.entityData.get(DATA_TRADE_ID);
    }

    public void setTradeId(String tradeId) {
        this.entityData.set(DATA_TRADE_ID, tradeId != null ? tradeId : "");
    }

    public boolean hasTrade() {
        String tradeId = getTradeId();
        return tradeId != null && !tradeId.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Home Position
    // ─────────────────────────────────────────────────────────────────────────────

    public void setHomePos(BlockPos pos, float yaw) {
        this.homePos = pos;
        this.homeYaw = yaw;
    }

    @Nullable
    public BlockPos getHomePos() {
        return homePos;
    }

    /**
     * Resets Oliver to his home position and STATIONARY mode.
     * Stops navigation, clears follow target, and teleports home.
     */
    public void resetToHome() {
        setStationary();
        this.getNavigation().stop();
        if (homePos != null) {
            this.teleportTo(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5);
            this.setYRot(homeYaw);
            this.setYHeadRot(homeYaw);
            this.setYBodyRot(homeYaw);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Persistence
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString(TAG_DIALOGUE_ID, getDialogueId());
        output.putString(TAG_TRADE_ID, getTradeId());
        output.putInt(TAG_MODE, getMode().getId());

        if (followTargetId != null) {
            output.putString(TAG_FOLLOW_TARGET, followTargetId.toString());
        }
        if (walkTarget != null) {
            output.putInt(TAG_WALK_TARGET_X, walkTarget.getX());
            output.putInt(TAG_WALK_TARGET_Y, walkTarget.getY());
            output.putInt(TAG_WALK_TARGET_Z, walkTarget.getZ());
        }
        if (homePos != null) {
            output.putInt(TAG_HOME_X, homePos.getX());
            output.putInt(TAG_HOME_Y, homePos.getY());
            output.putInt(TAG_HOME_Z, homePos.getZ());
            output.putInt(TAG_HOME_YAW, (int) (homeYaw * 10));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setDialogueId(input.getStringOr(TAG_DIALOGUE_ID, "default"));
        setTradeId(input.getStringOr(TAG_TRADE_ID, ""));
        this.entityData.set(DATA_MODE, input.getIntOr(TAG_MODE, Mode.STATIONARY.getId()));

        String followTargetStr = input.getStringOr(TAG_FOLLOW_TARGET, null);
        if (followTargetStr != null) {
            try {
                this.followTargetId = UUID.fromString(followTargetStr);
            } catch (IllegalArgumentException ignored) {
            }
        }

        int walkX = input.getIntOr(TAG_WALK_TARGET_X, Integer.MIN_VALUE);
        int walkY = input.getIntOr(TAG_WALK_TARGET_Y, Integer.MIN_VALUE);
        int walkZ = input.getIntOr(TAG_WALK_TARGET_Z, Integer.MIN_VALUE);
        if (walkX != Integer.MIN_VALUE && walkY != Integer.MIN_VALUE && walkZ != Integer.MIN_VALUE) {
            this.walkTarget = new BlockPos(walkX, walkY, walkZ);
        }

        int homeX = input.getIntOr(TAG_HOME_X, Integer.MIN_VALUE);
        int homeY = input.getIntOr(TAG_HOME_Y, Integer.MIN_VALUE);
        int homeZ = input.getIntOr(TAG_HOME_Z, Integer.MIN_VALUE);
        if (homeX != Integer.MIN_VALUE && homeY != Integer.MIN_VALUE && homeZ != Integer.MIN_VALUE) {
            this.homePos = new BlockPos(homeX, homeY, homeZ);
            this.homeYaw = input.getIntOr(TAG_HOME_YAW, 0) / 10f;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Invincibility
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Tick & Movement
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        // Use super.tick() to get full Mob tick chain (movement physics, navigation, gravity)
        // customServerAiStep() is overridden to prevent Villager brain AI
        super.tick();

        // Server-side logic
        if (this.level() instanceof ServerLevel serverLevel) {
            // Tick dialogue session
            tickDialogueSession(serverLevel);

            // Handle mode-specific behavior (follow, walkto, stationary)
            tickMode(serverLevel);

            // Ensure custom name is always present
            if (this.getCustomName() == null) {
                this.setCustomName(Component.literal("§dOliver"));
                this.setCustomNameVisible(true);
            }
        }

        // In STATIONARY mode, prevent all movement
        if (getMode() == Mode.STATIONARY) {
            this.setDeltaMovement(0, 0, 0);
            this.hurtMarked = false;
        }
    }

    private void tickMode(ServerLevel level) {
        Mode mode = getMode();

        if (pathfindCooldown > 0) {
            pathfindCooldown--;
        }

        switch (mode) {
            case STATIONARY -> {
                this.getNavigation().stop();
            }
            case FOLLOWING -> {
                tickFollowing(level);
            }
            case WALKING_TO -> {
                tickWalkingTo(level);
            }
        }
    }

    private void tickFollowing(ServerLevel level) {
        Player target = getFollowTarget();
        if (target == null || target.isRemoved() || target.level() != this.level()) {
            // Target lost or in different dimension, become stationary
            setStationary();
            return;
        }

        double distSqr = this.distanceToSqr(target);
        double dist = Math.sqrt(distSqr);

        // If too far, teleport to player with particles
        // But NOT during animations or cinematics (looks bad)
        if (dist > FOLLOW_TELEPORT_DISTANCE) {
            // Check if any animation is playing
            if (TutorialAnimationHandler.isAnyAnimationActive()) {
                return; // Don't teleport during animations
            }
            // Check if player is in a cinematic
            if (target instanceof ServerPlayer sp && TutorialCinematicHandler.isInCinematic(sp)) {
                return; // Don't teleport during cinematics
            }
            teleportToPlayer(level, target);
            return;
        }

        // Only pathfind if player is far enough and cooldown is ready
        if (distSqr > FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE && pathfindCooldown <= 0) {
            this.getNavigation().moveTo(target, 0.6);
            pathfindCooldown = 10;
        } else if (distSqr <= FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
            // Close enough, stop moving
            this.getNavigation().stop();
        }

        // Always look at the target
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    /**
     * Teleports Oliver to the player with particle effects.
     * Places Oliver 2 blocks behind/offset from the player based on their look direction.
     */
    private void teleportToPlayer(ServerLevel level, Player target) {
        // Spawn particles at current position (disappear effect)
        level.sendParticles(
                ParticleTypes.PORTAL,
                this.getX(), this.getY() + 1.0, this.getZ(),
                20, 0.3, 0.5, 0.3, 0.1
        );
        level.sendParticles(
                ParticleTypes.WITCH,
                this.getX(), this.getY() + 1.0, this.getZ(),
                10, 0.2, 0.4, 0.2, 0.0
        );

        // Calculate position behind/beside the player
        float yaw = target.getYRot();
        double offsetX = -Math.sin(Math.toRadians(yaw)) * FOLLOW_TELEPORT_OFFSET;
        double offsetZ = Math.cos(Math.toRadians(yaw)) * FOLLOW_TELEPORT_OFFSET;

        double newX = target.getX() + offsetX;
        double newY = target.getY();
        double newZ = target.getZ() + offsetZ;

        // Teleport
        this.teleportTo(newX, newY, newZ);
        this.getNavigation().stop();

        // Make Oliver face the player
        this.setYRot(yaw + 180);
        this.setYHeadRot(yaw + 180);
        this.setYBodyRot(yaw + 180);

        // Spawn particles at new position (appear effect)
        level.sendParticles(
                ParticleTypes.PORTAL,
                newX, newY + 1.0, newZ,
                20, 0.3, 0.5, 0.3, 0.1
        );
        level.sendParticles(
                ParticleTypes.WITCH,
                newX, newY + 1.0, newZ,
                10, 0.2, 0.4, 0.2, 0.0
        );

        // Play teleport sound
        level.playSound(
                null,
                newX, newY, newZ,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.NEUTRAL,
                0.5F,
                1.2F
        );

        AromaAffect.LOGGER.debug("Oliver teleported to player {} at {}, {}, {}",
                target.getName().getString(), newX, newY, newZ);
    }

    private void tickWalkingTo(ServerLevel level) {
        if (walkTarget == null) {
            setStationary();
            return;
        }

        double distSqr = this.blockPosition().distSqr(walkTarget);

        // Arrived at destination
        if (distSqr <= WALK_ARRIVAL_DISTANCE * WALK_ARRIVAL_DISTANCE) {
            setStationary();
            AromaAffect.LOGGER.debug("Oliver arrived at destination");
            return;
        }

        // Pathfind to target
        if (pathfindCooldown <= 0) {
            this.getNavigation().moveTo(walkTarget.getX() + 0.5, walkTarget.getY(), walkTarget.getZ() + 0.5, 0.6);
            pathfindCooldown = 20;
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        // Only stop navigation in stationary mode
        if (getMode() == Mode.STATIONARY) {
            this.getNavigation().stop();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Interaction & Dialogue
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        // Client side: just consume the interaction, server will send the dialogue packet
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (this.level() instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            keepDialogueAlive(player);

            // Debug log to diagnose trade issues
            com.ovrtechnology.AromaAffect.LOGGER.info("[Oliver Debug] Player {} clicked Oliver. dialogueId='{}', hasTrade={}, tradeId='{}'",
                    player.getName().getString(), getDialogueId(), hasTrade(), getTradeId());

            // Send S2C packet to open dialogue with trade context
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    serverPlayer,
                    this.getId(),
                    getDialogueId(),
                    hasTrade(),
                    getTradeId()
            );

            this.level().playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.VILLAGER_AMBIENT,
                    SoundSource.NEUTRAL,
                    1.0F,
                    1.0F
            );

            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.CONSUME;
    }

    public void keepDialogueAlive(Player player) {
        dialoguePlayerId = player.getUUID();
        dialogueKeepAliveTicks = DIALOGUE_KEEPALIVE_TIMEOUT_TICKS;
    }

    public void endDialogue(Player player) {
        if (dialoguePlayerId != null && dialoguePlayerId.equals(player.getUUID())) {
            clearDialogueSession();
        }
    }

    private void clearDialogueSession() {
        dialoguePlayerId = null;
        dialogueKeepAliveTicks = 0;
    }

    private void tickDialogueSession(ServerLevel serverLevel) {
        if (dialogueKeepAliveTicks <= 0 || dialoguePlayerId == null) {
            clearDialogueSession();
            return;
        }

        Player player = serverLevel.getPlayerInAnyDimension(dialoguePlayerId);
        if (player == null || player.isRemoved() || player.level() != this.level()) {
            clearDialogueSession();
            return;
        }

        if (player.distanceToSqr(this) > DIALOGUE_MAX_DISTANCE_SQR) {
            clearDialogueSession();
            return;
        }

        this.getLookControl().setLookAt(player, 60.0F, 60.0F);
        dialogueKeepAliveTicks--;
    }

    public boolean isInDialogue() {
        return dialogueKeepAliveTicks > 0 && dialoguePlayerId != null;
    }

    private void openDialogueUiClient() {
        try {
            Class<?> dialogueClass = Class.forName(
                    "com.ovrtechnology.tutorial.oliver.client.dialogue.TutorialOliverDialogueClient"
            );
            dialogueClass.getMethod("open", TutorialOliverEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to open Tutorial Oliver dialogue UI", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // AI Goals
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Attributes
    // ─────────────────────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)  // Can move now
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void handleEntityEvent(byte id) {
        // Suppress Villager happy/angry/heart particles (events 12, 13, 14)
        // These are noisy green HAPPY_VILLAGER particles from leveling up / trades
        if (id == 12 || id == 13 || id == 14) {
            return;
        }
        super.handleEntityEvent(id);
    }
}
