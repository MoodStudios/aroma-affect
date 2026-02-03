package com.ovrtechnology.entity.nosesmith;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.scentitem.ScentItem;
import com.ovrtechnology.scentitem.ScentItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Nose Smith is an NPC that helps players acquire and unlock Scent Masks.
 * This character is central to the Aroma Affect progression system, guiding players
 * through the mask tier system and providing access to nose equipment.
 */
public class NoseSmithEntity extends Villager {

    private static final EntityDataAccessor<Boolean> HAS_NOSE =
            SynchedEntityData.defineId(NoseSmithEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<String> REQUESTED_FLOWER_ID =
            SynchedEntityData.defineId(NoseSmithEntity.class, EntityDataSerializers.STRING);

    private static final String TAG_HAS_NOSE = "HasNose";
    private static final String TAG_REQUESTED_FLOWER = "RequestedFlower";
    private static final String TAG_HOUSE_DECORATED = "HouseDecorated";

    private static final double FLOWER_TURN_IN_RADIUS = 1.75D;
    private static final int FLOWER_PLACEMENT_ATTEMPTS = 32;
    private static final int HOUSE_DECOR_SEARCH_RADIUS_XZ = 12;
    private static final int HOUSE_DECOR_SEARCH_RADIUS_Y = 8;

    private static final int DIALOGUE_KEEPALIVE_TIMEOUT_TICKS = 100;
    private static final double DIALOGUE_MAX_DISTANCE_SQR = 8.0D * 8.0D;

    private static volatile List<FlowerVariant> cachedPottableSmallFlowers;

    @Nullable
    private ResourceLocation requestedFlower;

    private boolean houseDecorated = false;

    @Nullable
    private UUID dialoguePlayerId;
    private int dialogueKeepAliveTicks = 0;

    private int headNodTicks = 0;
    int smellCooldownTicks = 0;
    int sniffEntityCooldownTicks = 0;

    public NoseSmithEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.translatable("entity.aromaaffect.nose_smith"));
        this.setCustomNameVisible(true);
        this.setVillagerData(this.getVillagerData().withProfession(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NITWIT)).withLevel(5));
    }

    @Override
    public void setVillagerData(VillagerData villagerData) {
        super.setVillagerData(villagerData.withProfession(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NITWIT)).withLevel(5));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_NOSE, true);
        builder.define(REQUESTED_FLOWER_ID, "");
    }

    public boolean hasNose() {
        return this.entityData.get(HAS_NOSE);
    }

    public void setHasNose(boolean hasNose) {
        this.entityData.set(HAS_NOSE, hasNose);
    }

    @Nullable
    public ResourceLocation getRequestedFlowerId() {
        String value = this.entityData.get(REQUESTED_FLOWER_ID);
        if (value == null || value.isBlank()) {
            return null;
        }

        return ResourceLocation.tryParse(value);
    }
    
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean(TAG_HAS_NOSE, hasNose());
        if (requestedFlower != null) {
            output.putString(TAG_REQUESTED_FLOWER, requestedFlower.toString());
        }
        output.putBoolean(TAG_HOUSE_DECORATED, houseDecorated);
    }
    
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setHasNose(input.getBooleanOr(TAG_HAS_NOSE, true));

        requestedFlower = input.getString(TAG_REQUESTED_FLOWER)
                .map(ResourceLocation::parse)
                .orElse(null);

        this.entityData.set(REQUESTED_FLOWER_ID, requestedFlower != null ? requestedFlower.toString() : "");

        houseDecorated = input.getBooleanOr(TAG_HOUSE_DECORATED, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getCustomName() == null) {
            this.setCustomName(Component.translatable("entity.aromaaffect.nose_smith"));
            this.setCustomNameVisible(true);
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (headNodTicks > 0) {
            headNodTicks--;
            this.setXRot((float) (Math.sin(headNodTicks * 0.5) * 20));
            if (headNodTicks == 0) {
                this.setXRot(0);
            }
        }

        if (smellCooldownTicks > 0) {
            smellCooldownTicks--;
        }

        if (sniffEntityCooldownTicks > 0) {
            sniffEntityCooldownTicks--;
        }

        // Passive regeneration: heal 1 HP every 80 ticks if not recently hurt
        if (this.tickCount % 80 == 0 && this.getHealth() < this.getMaxHealth()) {
            int lastHurtTime = this.getLastHurtByMobTimestamp();
            if (this.tickCount - lastHurtTime > 100) {
                this.heal(1.0F);
            }
        }

        tickDialogueSession(serverLevel);
        ensureRequestedFlowerInitialized(serverLevel);
        decorateHouseOnce(serverLevel);
        tryConsumeRequestedFlowerAndReward(serverLevel);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        // Never run Villager AI (profession assignment, trading, gossip, etc.)
        if (isInDialogue()) {
            this.getNavigation().stop();
            this.getMoveControl().setWait();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));

            Player player = getDialoguePlayer();
            if (player != null) {
                this.getLookControl().setLookAt(player, 60.0F, 60.0F);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide()) {
            openDialogueUiClient();
            return InteractionResult.SUCCESS;
        }

        if (this.level() instanceof ServerLevel) {
            keepDialogueAlive(player);
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.CONSUME;
    }

    public void keepDialogueAlive(Player player) {
        dialoguePlayerId = player.getUUID();
        dialogueKeepAliveTicks = DIALOGUE_KEEPALIVE_TIMEOUT_TICKS;
        this.getNavigation().stop();
    }

    public void endDialogue(Player player) {
        if (dialoguePlayerId == null || !dialoguePlayerId.equals(player.getUUID())) {
            return;
        }

        clearDialogueSession();
    }

    private void clearDialogueSession() {
        dialoguePlayerId = null;
        dialogueKeepAliveTicks = 0;
    }

    private void tickDialogueSession(ServerLevel serverLevel) {
        if (dialogueKeepAliveTicks <= 0 || dialoguePlayerId == null) {
            dialogueKeepAliveTicks = 0;
            dialoguePlayerId = null;
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

        dialogueKeepAliveTicks--;
        if (dialogueKeepAliveTicks <= 0) {
            clearDialogueSession();
        }
    }

    boolean isInDialogue() {
        return dialogueKeepAliveTicks > 0 && dialoguePlayerId != null;
    }

    @Nullable
    private Player getDialoguePlayer() {
        if (dialoguePlayerId == null) {
            return null;
        }

        Player player = this.level().getPlayerInAnyDimension(dialoguePlayerId);
        if (player == null || player.level() != this.level()) {
            return null;
        }

        return player;
    }

    private void openDialogueUiClient() {
        try {
            Class<?> clazz = Class.forName("com.ovrtechnology.entity.nosesmith.client.dialogue.NoseSmithDialogueClient");
            clazz.getMethod("open", NoseSmithEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to open Nose Smith dialogue UI", e);
        }
    }

    private void ensureRequestedFlowerInitialized(ServerLevel serverLevel) {
        if (requestedFlower != null) {
            return;
        }

        List<FlowerVariant> candidates = getPottableSmallFlowers();
        if (candidates.isEmpty()) {
            AromaAffect.LOGGER.warn("No pottable small flowers found; Nose Smith quest cannot initialize.");
            return;
        }

        FlowerVariant chosen = candidates.get(this.getRandom().nextInt(candidates.size()));
        requestedFlower = BuiltInRegistries.BLOCK.getKey(chosen.flower());
        this.entityData.set(REQUESTED_FLOWER_ID, requestedFlower.toString());
    }

    private void decorateHouseOnce(ServerLevel serverLevel) {
        if (houseDecorated) {
            return;
        }

        Block requested = getRequestedFlowerBlock(serverLevel);
        if (requested == null) {
            return;
        }

        List<FlowerVariant> variants = getPottableSmallFlowers();
        if (variants.isEmpty()) {
            return;
        }

        List<FlowerVariant> fillVariants = new ArrayList<>(variants.size());
        for (FlowerVariant variant : variants) {
            if (variant.flower() != requested) {
                fillVariants.add(variant);
            }
        }

        if (fillVariants.isEmpty()) {
            return;
        }

        RandomSource random = this.getRandom();

        BlockPos origin = this.blockPosition();
        BlockPos min = origin.offset(-HOUSE_DECOR_SEARCH_RADIUS_XZ, -HOUSE_DECOR_SEARCH_RADIUS_Y, -HOUSE_DECOR_SEARCH_RADIUS_XZ);
        BlockPos max = origin.offset(HOUSE_DECOR_SEARCH_RADIUS_XZ, HOUSE_DECOR_SEARCH_RADIUS_Y, HOUSE_DECOR_SEARCH_RADIUS_XZ);

        int potsFilled = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!(serverLevel.getBlockState(pos).getBlock() instanceof FlowerPotBlock)) {
                continue;
            }

            FlowerVariant variant = fillVariants.get(random.nextInt(fillVariants.size()));
            serverLevel.setBlock(pos, variant.pottedFlowerPot().defaultBlockState(), Block.UPDATE_ALL);
            potsFilled++;
        }

        int flowersToPlace = 1 + random.nextInt(2);
        int flowersPlaced = 0;
        BlockState flowerState = requested.defaultBlockState();
        for (int attempt = 0; attempt < FLOWER_PLACEMENT_ATTEMPTS && flowersPlaced < flowersToPlace; attempt++) {
            int dx = random.nextIntBetweenInclusive(-6, 6);
            int dz = random.nextIntBetweenInclusive(-6, 6);
            if (dx == 0 && dz == 0) {
                continue;
            }

            BlockPos candidate = origin.offset(dx, 0, dz);
            BlockPos surface = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);

            if (clearReplaceable(serverLevel, surface)) {
                if (!flowerState.canSurvive(serverLevel, surface)) {
                    serverLevel.setBlock(surface.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                }
                serverLevel.setBlock(surface, flowerState, Block.UPDATE_ALL);
                flowersPlaced++;
            }
        }

        if (flowersPlaced == 0) {
            BlockPos fallback = origin.above();
            clearReplaceable(serverLevel, fallback);
            if (!flowerState.canSurvive(serverLevel, fallback)) {
                serverLevel.setBlock(fallback.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            }
            serverLevel.setBlock(fallback, flowerState, Block.UPDATE_ALL);
            flowersPlaced = 1;
        }

        fillChestsWithScents(serverLevel, origin, min, max, random);

        houseDecorated = true;
        AromaAffect.LOGGER.debug("Decorated Nose Smith house: filled {} pots, placed {} quest flowers", potsFilled, flowersPlaced);
    }

    private void tryConsumeRequestedFlowerAndReward(ServerLevel serverLevel) {
        if (!hasNose()) {
            return;
        }

        Block requested = getRequestedFlowerBlock(serverLevel);
        if (requested == null) {
            return;
        }

        Item requestedItem = requested.asItem();
        if (requestedItem == Items.AIR) {
            return;
        }

        List<ItemEntity> nearbyItems = serverLevel.getEntitiesOfClass(
                ItemEntity.class,
                this.getBoundingBox().inflate(FLOWER_TURN_IN_RADIUS, 0.75D, FLOWER_TURN_IN_RADIUS)
        );

        for (ItemEntity itemEntity : nearbyItems) {
            ItemStack stack = itemEntity.getItem();
            if (!stack.is(requestedItem)) {
                continue;
            }

            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }

            Player thrower = itemEntity.getOwner() instanceof Player p ? p
                    : serverLevel.getNearestPlayer(this, FLOWER_TURN_IN_RADIUS + 2.0D);
            giveNoseReward(serverLevel, thrower);
            return;
        }
    }

    private void giveNoseReward(ServerLevel serverLevel, @Nullable Player receiver) {
        setHasNose(false);

        ItemStack noseItem = NoseRegistry.getNoseSupplier("basic_nose")
                .map(supplier -> new ItemStack(supplier.get()))
                .orElse(ItemStack.EMPTY);

        if (!noseItem.isEmpty()) {
            double spawnX = this.getX();
            double spawnY = this.getEyeY() - 0.3D;
            double spawnZ = this.getZ();
            ItemEntity drop = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, noseItem);
            drop.setPickUpDelay(10);

            if (receiver != null) {
                double dx = receiver.getX() - spawnX;
                double dy = receiver.getEyeY() - spawnY;
                double dz = receiver.getZ() - spawnZ;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 0.01D) {
                    double speed = 0.3D;
                    drop.setDeltaMovement(dx / dist * speed, dy / dist * speed + 0.15D, dz / dist * speed);
                }
            }

            serverLevel.addFreshEntity(drop);
        } else {
            AromaAffect.LOGGER.warn("Failed to drop basic_nose: item not registered");
        }

        headNodTicks = 40;

        serverLevel.playSound(null, this.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75F, 1.2F);
    }

    private static void fillChestsWithScents(ServerLevel serverLevel, BlockPos origin, BlockPos min, BlockPos max, RandomSource random) {
        List<ScentItem> scents = ScentItemRegistry.getCapsuleItems();
        if (scents.isEmpty()) {
            return;
        }

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!(serverLevel.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
                continue;
            }

            int slotsToFill = 1 + random.nextInt(4);
            int slotsFilled = 0;
            int containerSize = chest.getContainerSize();

            for (int attempt = 0; attempt < 16 && slotsFilled < slotsToFill; attempt++) {
                int slot = random.nextInt(containerSize);
                if (!chest.getItem(slot).isEmpty()) {
                    continue;
                }

                ScentItem scent = scents.get(random.nextInt(scents.size()));
                int count = 1 + random.nextInt(2);
                chest.setItem(slot, new ItemStack(scent, count));
                slotsFilled++;
            }
        }
    }

    private static boolean clearReplaceable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) {
            level.removeBlock(pos, false);
            return true;
        }
        return false;
    }

    @Nullable
    private Block getRequestedFlowerBlock(ServerLevel serverLevel) {
        if (requestedFlower == null) {
            return null;
        }

        Block block = BuiltInRegistries.BLOCK.getOptional(requestedFlower).orElse(null);
        if (block == null || block == Blocks.AIR || !block.defaultBlockState().is(BlockTags.SMALL_FLOWERS)) {
            AromaAffect.LOGGER.warn("Invalid requested flower id {} for Nose Smith; resetting quest.", requestedFlower);
            requestedFlower = null;
            this.entityData.set(REQUESTED_FLOWER_ID, "");
            houseDecorated = false;
            return null;
        }

        return block;
    }

    private static List<FlowerVariant> getPottableSmallFlowers() {
        List<FlowerVariant> cached = cachedPottableSmallFlowers;
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        Map<Block, Block> pottedByContent = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof FlowerPotBlock flowerPotBlock)) {
                continue;
            }

            Block content = flowerPotBlock.getPotted();
            if (content == Blocks.AIR) {
                continue;
            }

            if (!content.defaultBlockState().is(BlockTags.SMALL_FLOWERS)) {
                continue;
            }

            Item contentItem = content.asItem();
            if (contentItem == Items.AIR) {
                continue;
            }

            pottedByContent.put(content, block);
        }

        List<FlowerVariant> variants = new ArrayList<>(pottedByContent.size());
        for (Map.Entry<Block, Block> entry : pottedByContent.entrySet()) {
            variants.add(new FlowerVariant(entry.getKey(), entry.getValue()));
        }

        cachedPottableSmallFlowers = List.copyOf(variants);
        return cachedPottableSmallFlowers;
    }

    private record FlowerVariant(Block flower, Block pottedFlowerPot) {
    }
    
    /**
     * Create the attribute builder for the Nose Smith.
     * Based on standard Villager attributes with slightly higher health.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DialogueHoldGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 0.5D));
        this.goalSelector.addGoal(3, new NoseSmithSleepGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 0.6D, true));
        this.goalSelector.addGoal(5, new NoseSmithSmellFlowerGoal(this));
        this.goalSelector.addGoal(5, new NoseSmithSniffEntityGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35D));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    private static final class DialogueHoldGoal extends Goal {
        private final NoseSmithEntity noseSmith;

        private DialogueHoldGoal(NoseSmithEntity noseSmith) {
            this.noseSmith = noseSmith;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return noseSmith.isInDialogue();
        }

        @Override
        public boolean canContinueToUse() {
            return noseSmith.isInDialogue();
        }

        @Override
        public void start() {
            noseSmith.getNavigation().stop();
        }

        @Override
        public void tick() {
            noseSmith.getNavigation().stop();
            noseSmith.setDeltaMovement(noseSmith.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));

            Player player = noseSmith.getDialoguePlayer();
            if (player != null) {
                noseSmith.getLookControl().setLookAt(player, 60.0F, 60.0F);
            }
        }

        @Override
        public void stop() {
            noseSmith.getNavigation().stop();
        }
    }
    
    @Override
    @Nullable
    public Villager getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        // Nose Smiths cannot breed - they are unique NPCs
        return null;
    }
    
    @Override
    public boolean canBreed() {
        // Nose Smiths cannot breed
        return false;
    }
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        // Never despawn the Nose Smith
        return false;
    }
    
    @Override
    public boolean isPersistenceRequired() {
        // Always persist
        return true;
    }
    
    // TODO: Add custom trading logic for nose items and scent masks
    // This will be implemented when the trading system is designed
}
