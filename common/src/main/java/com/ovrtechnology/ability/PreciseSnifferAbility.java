package com.ovrtechnology.ability;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of the Precise Sniffer ability
 * 
 * <p>
 * This ability allows players to "sniff out" loot from Suspicious Sand
 * with a progressive animation similar to brushing, but with an increased
 * chance to obtain Sniffer Eggs.
 * </p>
 * 
 * <h2>Mechanics:</h2>
 * <ul>
 * <li>Player must hold right-click on Suspicious Sand</li>
 * <li>Progress builds up over time (like brushing)</li>
 * <li>When complete, loot is generated with 40% Sniffer Egg chance</li>
 * <li>Cooldown prevents spam after successful sniff</li>
 * <li>Nose durability is consumed on success</li>
 * </ul>
 * 
 * @see SnifferLootTable
 * @see AbilityHandler
 */
public final class PreciseSnifferAbility {

    /**
     * Total ticks required to complete a sniff (similar to brush duration).
     * 40 ticks = 2 seconds.
     */
    public static final int SNIFF_DURATION_TICKS = 40;

    /**
     * Cooldown in ticks after a successful sniff.
     * 40 ticks = 2 seconds.
     */
    public static final int COOLDOWN_TICKS = 40;

    /**
     * Durability cost per successful sniff.
     */
    public static final int DURABILITY_COST = 5;

    /**
     * Maximum distance player can move before sniffing is cancelled.
     */
    private static final double MAX_MOVEMENT_DISTANCE = 1.5;

    // ========== State Tracking ==========

    /**
     * Tracks active sniffing sessions.
     * Key: Player UUID, Value: Sniffing session data.
     */
    private static final Map<UUID, SniffingSession> ACTIVE_SESSIONS = new HashMap<>();

    /**
     * Tracks cooldowns per player.
     * Key: Player UUID, Value: Game tick when cooldown expires.
     */
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private PreciseSnifferAbility() {}

    // ========== Public API ==========

    /**
     * Checks if a block is valid for the Precise Sniffer ability.
     * 
     * @param block the block to check
     * @return true if the block is Suspicious Sand
     */
    public static boolean isValidTarget(Block block) {
        return block == Blocks.SUSPICIOUS_SAND;
    }

    /**
     * Checks if a player can use the Precise Sniffer ability.
     * 
     * @param player the player
     * @return true if the player can use the ability
     */
    public static boolean canUse(ServerPlayer player) {
        Long cooldownEnd = COOLDOWNS.get(player.getUUID());
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

    /**
     * Starts or continues a sniffing session for a player.
     * Should be called every tick while the player is interacting.
     * 
     * @param player the player
     * @param pos    the block position
     * @return true if the sniff completed this tick, false otherwise
     */
    public static boolean tickSniffing(ServerPlayer player, BlockPos pos) {
        UUID playerId = player.getUUID();
        long currentTick = player.level().getGameTime();
        Vec3 playerPos = player.position();

        // Get or create session
        SniffingSession session = ACTIVE_SESSIONS.get(playerId);

        if (session == null || !session.targetPos.equals(pos)) {
            // Start new session
            session = new SniffingSession(pos, playerPos, currentTick, 0);
            ACTIVE_SESSIONS.put(playerId, session);
            AromaAffect.LOGGER.debug("Started sniffing session for {} at {}", player.getName().getString(), pos);
        }

        // Check if player moved too far
        if (playerPos.distanceTo(session.startPlayerPos) > MAX_MOVEMENT_DISTANCE) {
            cancelSniffing(player);
            return false;
        }

        // Increment progress
        session = new SniffingSession(
                session.targetPos,
                session.startPlayerPos,
                session.startTick,
                session.progress + 1);
        ACTIVE_SESSIONS.put(playerId, session);

        // Play sniffing sound periodically (every 10 ticks)
        if (session.progress % 10 == 0) {
            playSniffSound(player, pos);
        }

        // Spawn particles
        spawnSniffParticles(player, pos);

        // Check if complete
        if (session.progress >= SNIFF_DURATION_TICKS) {
            completeSniffing(player, pos);
            return true;
        }

        return false;
    }

    /**
     * Cancels an active sniffing session for a player.
     * 
     * @param player the player
     */
    public static void cancelSniffing(ServerPlayer player) {
        SniffingSession removed = ACTIVE_SESSIONS.remove(player.getUUID());
        if (removed != null) {
            AromaAffect.LOGGER.debug("Cancelled sniffing session for {}", player.getName().getString());
        }
    }

    /**
     * Checks if a player has an active sniffing session.
     * 
     * @param player the player
     * @return true if sniffing is in progress
     */
    public static boolean isSniffing(ServerPlayer player) {
        return ACTIVE_SESSIONS.containsKey(player.getUUID());
    }

    /**
     * Gets the sniffing progress for a player (0.0 to 1.0).
     * 
     * @param player the player
     * @return progress from 0.0 (just started) to 1.0 (complete)
     */
    public static float getProgress(ServerPlayer player) {
        SniffingSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null) {
            return 0.0f;
        }
        return Math.min(1.0f, (float) session.progress / SNIFF_DURATION_TICKS);
    }

    /**
     * Gets remaining cooldown in ticks for a player.
     * 
     * @param player the player
     * @return remaining ticks, or 0 if not on cooldown
     */
    public static long getRemainingCooldown(ServerPlayer player) {
        Long cooldownEnd = COOLDOWNS.get(player.getUUID());
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = cooldownEnd - player.level().getGameTime();
        return Math.max(0, remaining);
    }

    // ========== Internal Methods ==========

    /**
     * Completes a sniffing session, generating loot and applying effects.
     */
    private static void completeSniffing(ServerPlayer player, BlockPos pos) {
        ServerLevel level = (ServerLevel) player.level();

        ACTIVE_SESSIONS.remove(player.getUUID());

        COOLDOWNS.put(player.getUUID(), level.getGameTime() + COOLDOWN_TICKS);

        // Verify the block is still a brushable block
        if (!(level.getBlockEntity(pos) instanceof BrushableBlockEntity)) {
            AromaAffect.LOGGER.warn("No BrushableBlockEntity at {} during sniff completion", pos);
            return;
        }

        // Use the standard desert pyramid archaeology loot table
        // This is the most common loot table for Suspicious Sand in vanilla
        // TODO: In the future, we could use mixins/access transformers to get the
        // actual loot table
        ResourceKey<LootTable> lootTableKey = net.minecraft.world.level.storage.loot.BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY;

        // Roll loot with Sniffer Egg bias
        ItemStack loot = SnifferLootTable.rollLoot(
                level,
                pos,
                lootTableKey,
                player,
                level.getRandom());

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
    private static void damageNose(ServerPlayer player) {
        ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headItem.isEmpty()) {
            headItem.hurtAndBreak(DURABILITY_COST, player, EquipmentSlot.HEAD);
        }
    }

    /**
     * Plays sniffing sound effect.
     */
    private static void playSniffSound(ServerPlayer player, BlockPos pos) {
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
    private static void playSuccessSound(ServerPlayer player, BlockPos pos, ItemStack loot) {
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
    private static void spawnSniffParticles(ServerPlayer player, BlockPos pos) {
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
    private static void spawnCelebrationParticles(ServerLevel level, BlockPos pos) {
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
    public static void cleanup() {
        // Could implement session timeout here if needed
        // For now, sessions are cleaned up on cancel/complete
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
