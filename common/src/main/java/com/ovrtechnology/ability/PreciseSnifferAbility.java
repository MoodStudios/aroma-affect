package com.ovrtechnology.ability;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Implementation of the Precise Sniffer ability.
 * 
 * <p>
 * This ability allows players to "sniff out" loot from Suspicious Sand
 * with a progressive animation similar to brushing, but with an increased
 * chance to obtain Sniffer Eggs.
 * </p>
 * 
 * <p>
 * Configuration is loaded from {@code abilities.json}. Default values are
 * used if the config is not available.
 * </p>
 * 
 * <h2>Mechanics:</h2>
 * <ul>
 * <li>Player must hold right-click on Suspicious Sand</li>
 * <li>Progress builds up over time (like brushing)</li>
 * <li>When complete, loot is generated with configurable Sniffer Egg chance</li>
 * <li>Cooldown prevents spam after successful sniff</li>
 * <li>Nose durability is consumed on success</li>
 * </ul>
 * 
 * @see SnifferLootTable
 * @see AbilityHandler
 * @see BlockInteractionAbility
 * @see AbilityDefinition
 */
public final class PreciseSnifferAbility implements BlockInteractionAbility {

    /**
     * Singleton instance for registration with AbilityRegistry.
     */
    public static final PreciseSnifferAbility INSTANCE = new PreciseSnifferAbility();

    // ========== Default Config Values ==========

    private static final int DEFAULT_DURATION_TICKS = 40;
    private static final int DEFAULT_COOLDOWN_TICKS = 40;
    private static final int DEFAULT_DURABILITY_COST = 5;
    private static final double DEFAULT_SNIFFER_EGG_CHANCE = 0.4;

    /**
     * Maximum distance player can move before sniffing is cancelled.
     */
    private static final double MAX_MOVEMENT_DISTANCE = 1.5;

    // ========== State Tracking ==========

    /**
     * Tracks active sniffing sessions.
     * Key: Player UUID, Value: Sniffing session data.
     */
    private final Map<UUID, SniffingSession> activeSessions = new HashMap<>();

    /**
     * Tracks cooldowns per player.
     * Key: Player UUID, Value: Game tick when cooldown expires.
     */
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * Cached valid blocks from config.
     */
    private Set<Block> validBlocks;

    /**
     * Private constructor - use {@link #INSTANCE} instead.
     */
    private PreciseSnifferAbility() {}

    // ========== Config Accessors ==========

    /**
     * Gets the ability definition from the loader.
     * 
     * @return the definition or null if not loaded
     */
    private AbilityDefinition getDefinition() {
        return AbilityDefinitionLoader.getAbilityById(AbilityConstants.PRECISE_SNIFFER);
    }

    /**
     * Gets the duration in ticks from config.
     */
    public int getDurationTicks() {
        AbilityDefinition def = getDefinition();
        return def != null ? def.getConfigInt("duration_ticks", DEFAULT_DURATION_TICKS) : DEFAULT_DURATION_TICKS;
    }

    /**
     * Gets the cooldown in ticks from config.
     */
    public int getCooldownTicks() {
        AbilityDefinition def = getDefinition();
        return def != null ? def.getConfigInt("cooldown_ticks", DEFAULT_COOLDOWN_TICKS) : DEFAULT_COOLDOWN_TICKS;
    }

    /**
     * Gets the durability cost from config.
     */
    public int getDurabilityCost() {
        AbilityDefinition def = getDefinition();
        return def != null ? def.getConfigInt("durability_cost", DEFAULT_DURABILITY_COST) : DEFAULT_DURABILITY_COST;
    }

    /**
     * Gets the Sniffer Egg chance from config.
     */
    public double getSnifferEggChance() {
        AbilityDefinition def = getDefinition();
        return def != null ? def.getConfigDouble("sniffer_egg_chance", DEFAULT_SNIFFER_EGG_CHANCE) : DEFAULT_SNIFFER_EGG_CHANCE;
    }

    /**
     * Gets and caches the valid blocks from config.
     */
    private Set<Block> getValidBlocks() {
        if (validBlocks == null) {
            validBlocks = new HashSet<>();
            AbilityDefinition def = getDefinition();
            if (def != null) {
                List<String> blockIds = def.getConfigStringList("valid_blocks");
                for (String blockId : blockIds) {
                    Identifier loc = Identifier.tryParse(blockId);
                    if (loc != null) {
                        Block block = BuiltInRegistries.BLOCK.getOptional(loc).orElse(null);
                        if (block != null && block != Blocks.AIR) {
                            validBlocks.add(block);
                        }
                    }
                }
            }
            // Fallback if no blocks configured
            if (validBlocks.isEmpty()) {
                validBlocks.add(Blocks.SUSPICIOUS_SAND);
                validBlocks.add(Blocks.SUSPICIOUS_GRAVEL);
            }
        }
        return validBlocks;
    }

    // ========== Ability Interface Implementation ==========

    @Override
    public String getId() {
        return AbilityConstants.PRECISE_SNIFFER;
    }

    @Override
    public boolean canUse(ServerPlayer player) {
        Long cooldownEnd = cooldowns.get(player.getUUID());
        if (cooldownEnd != null && player.level().getGameTime() < cooldownEnd) {
            return false;
        }

        // Check if player has a nose with the ability equipped
        ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        if (headItem.isEmpty() || !(headItem.getItem() instanceof NoseItem noseItem)) {
            return false;
        }

        return noseItem.hasAbility(AbilityConstants.PRECISE_SNIFFER);
    }

    @Override
    public boolean isValidTarget(Block block) {
        return getValidBlocks().contains(block);
    }

    @Override
    public boolean onInteract(ServerPlayer player, BlockPos pos) {
        return tickSniffingInternal(player, pos);
    }

    @Override
    public void onCancel(ServerPlayer player) {
        SniffingSession removed = activeSessions.remove(player.getUUID());
        if (removed != null) {
            AromaAffect.LOGGER.debug("Cancelled sniffing session for {}", player.getName().getString());
        }
    }

    @Override
    public boolean onTick(ServerPlayer player, BlockPos pos) {
        return tickSniffingInternal(player, pos);
    }

    @Override
    public boolean isActive(ServerPlayer player) {
        return activeSessions.containsKey(player.getUUID());
    }

    @Override
    public float getProgress(ServerPlayer player) {
        SniffingSession session = activeSessions.get(player.getUUID());
        if (session == null) {
            return 0.0f;
        }
        return Math.min(1.0f, (float) session.progress / getDurationTicks());
    }

    @Override
    public long getRemainingCooldown(ServerPlayer player) {
        Long cooldownEnd = cooldowns.get(player.getUUID());
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = cooldownEnd - player.level().getGameTime();
        return Math.max(0, remaining);
    }

    // ========== Internal Methods ==========

    /**
     * Processes a tick of sniffing, starting or continuing a session.
     * 
     * @param player the player
     * @param pos    the block position
     * @return true if the sniff completed this tick, false otherwise
     */
    private boolean tickSniffingInternal(ServerPlayer player, BlockPos pos) {
        UUID playerId = player.getUUID();
        long currentTick = player.level().getGameTime();
        Vec3 playerPos = player.position();

        // Get or create session
        SniffingSession session = activeSessions.get(playerId);

        if (session == null || !session.targetPos.equals(pos)) {
            // Start new session
            session = new SniffingSession(pos, playerPos, currentTick, 0);
            activeSessions.put(playerId, session);
            AromaAffect.LOGGER.debug("Started sniffing session for {} at {}", player.getName().getString(), pos);
        }

        // Check if player moved too far
        if (playerPos.distanceTo(session.startPlayerPos) > MAX_MOVEMENT_DISTANCE) {
            onCancel(player);
            return false;
        }

        // Increment progress
        session = new SniffingSession(
                session.targetPos,
                session.startPlayerPos,
                session.startTick,
                session.progress + 1);
        activeSessions.put(playerId, session);

        // Play sniffing sound periodically (every 10 ticks)
        if (session.progress % 10 == 0) {
            playSniffSound(player, pos);
        }

        // Spawn particles
        spawnSniffParticles(player, pos);

        // Check if complete
        if (session.progress >= getDurationTicks()) {
            completeSniffing(player, pos);
            return true;
        }

        return false;
    }

    /**
     * Completes a sniffing session, generating loot and applying effects.
     */
    private void completeSniffing(ServerPlayer player, BlockPos pos) {
        ServerLevel level = (ServerLevel) player.level();

        activeSessions.remove(player.getUUID());

        cooldowns.put(player.getUUID(), level.getGameTime() + getCooldownTicks());

        // Verify the block is still a brushable block
        if (!(level.getBlockEntity(pos) instanceof BrushableBlockEntity)) {
            AromaAffect.LOGGER.warn("No BrushableBlockEntity at {} during sniff completion", pos);
            return;
        }

        // Determine loot table and egg chance based on block type
        Block currentBlock = level.getBlockState(pos).getBlock();
        ResourceKey<LootTable> lootTableKey;
        double eggChance;

        if (currentBlock == Blocks.SUSPICIOUS_GRAVEL) {
            // Suspicious gravel uses vanilla behavior — no Sniffer Egg boost
            lootTableKey = net.minecraft.world.level.storage.loot.BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON;
            eggChance = 0.0;
        } else {
            // Suspicious sand uses the Precise Sniffer bonus
            lootTableKey = net.minecraft.world.level.storage.loot.BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY;
            eggChance = getSnifferEggChance();
        }

        // Roll loot (vanilla for gravel, Sniffer Egg bias for sand)
        ItemStack loot = SnifferLootTable.rollLoot(
                level,
                pos,
                lootTableKey,
                player,
                level.getRandom(),
                eggChance);

        if (!loot.isEmpty()) {
            // Spawn item entity at block position
            Vec3 spawnPos = Vec3.atCenterOf(pos).add(0, 0.5, 0);
            ItemEntity itemEntity = new ItemEntity(
                    level,
                    spawnPos.x,
                    spawnPos.y,
                    spawnPos.z,
                    loot);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);

            playSuccessSound(player, pos, loot);

            // Spawn celebration particles if Sniffer Egg
            if (loot.is(net.minecraft.world.item.Items.SNIFFER_EGG)) {
                spawnCelebrationParticles(level, pos);
                AromaAffect.LOGGER.info("{} found a Sniffer Egg using Precise Sniffer!",
                        player.getName().getString());
            }
        }

        damageNose(player);

        // Replace suspicious sand with regular sand
        Block originalBlock = level.getBlockState(pos).getBlock();
        if (originalBlock instanceof BrushableBlock brushableBlock) {
            level.setBlock(pos, brushableBlock.getTurnsInto().defaultBlockState(), 3);
        } else {
            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
        }

        AromaAffect.LOGGER.debug("Completed sniffing for {} at {}", player.getName().getString(), pos);
    }

    /**
     * Damages the player's equipped nose.
     */
    private void damageNose(ServerPlayer player) {
        ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headItem.isEmpty()) {
            headItem.hurtAndBreak(getDurabilityCost(), player, EquipmentSlot.HEAD);
        }
    }

    /**
     * Plays sniffing sound effect.
     */
    private void playSniffSound(ServerPlayer player, BlockPos pos) {
        player.level().playSound(
                null,
                pos,
                SoundEvents.SNIFFER_SNIFFING,
                SoundSource.PLAYERS,
                0.6f,
                1.0f + (player.level().getRandom().nextFloat() * 0.2f - 0.1f));
    }

    /**
     * Plays success sound effect based on loot obtained.
     */
    private void playSuccessSound(ServerPlayer player, BlockPos pos, ItemStack loot) {
        if (loot.is(net.minecraft.world.item.Items.SNIFFER_EGG)) {
            // Special sound for Sniffer Egg
            player.level().playSound(
                    null,
                    pos,
                    SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.2f);
        } else {
            // Normal success sound
            player.level().playSound(
                    null,
                    pos,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    0.8f,
                    1.0f);
        }
    }

    /**
     * Spawns sniffing particles during the sniffing animation.
     */
    private void spawnSniffParticles(ServerPlayer player, BlockPos pos) {
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 particlePos = Vec3.atCenterOf(pos);
            serverLevel.sendParticles(
                    ParticleTypes.WAX_ON,
                    particlePos.x,
                    particlePos.y + 0.5,
                    particlePos.z,
                    1, // count
                    0.2, 0.1, 0.2, // spread
                    0.0 // speed
            );
        }
    }

    /**
     * Spawns celebration particles when a Sniffer Egg is found.
     */
    private void spawnCelebrationParticles(ServerLevel level, BlockPos pos) {
        Vec3 particlePos = Vec3.atCenterOf(pos);
        level.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                particlePos.x,
                particlePos.y + 0.5,
                particlePos.z,
                30, // count
                0.5, 0.5, 0.5, // spread
                0.1 // speed
        );
    }

    /**
     * Cleans up expired sessions and cooldowns (call periodically).
     */
    public void cleanup() {
        // Could implement session timeout here if needed
        // For now, sessions are cleaned up on cancel/complete
    }

    /**
     * Clears the cached valid blocks (for reloading).
     */
    public void clearCache() {
        validBlocks = null;
    }

    // ========== Data Classes ==========

    /**
     * Represents an active sniffing session.
     * 
     * @param targetPos      the block being sniffed
     * @param startPlayerPos the player's position when sniffing started
     * @param startTick      the game tick when sniffing started
     * @param progress       current progress in ticks
     */
    private record SniffingSession(
            BlockPos targetPos,
            Vec3 startPlayerPos,
            long startTick,
            int progress) {
    }
}
