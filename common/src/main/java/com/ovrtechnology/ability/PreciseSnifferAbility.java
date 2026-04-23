package com.ovrtechnology.ability;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.nose.NoseTags;
import com.ovrtechnology.util.Ids;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

public final class PreciseSnifferAbility implements BlockInteractionAbility {

    public static final PreciseSnifferAbility INSTANCE = new PreciseSnifferAbility();

    private static final int DEFAULT_DURATION_TICKS = 40;
    private static final int DEFAULT_COOLDOWN_TICKS = 40;
    private static final int DEFAULT_DURABILITY_COST = 5;
    private static final double DEFAULT_SNIFFER_EGG_CHANCE = 0.4;

    private static final double MAX_MOVEMENT_DISTANCE = 1.5;

    private final Map<UUID, SniffingSession> activeSessions = new HashMap<>();

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private Set<Block> validBlocks;

    private PreciseSnifferAbility() {}

    private AbilityDefinition getDefinition() {
        return AbilityDefinitionLoader.getAbilityById(AbilityConstants.PRECISE_SNIFFER);
    }

    public int getDurationTicks() {
        AbilityDefinition def = getDefinition();
        return def != null
                ? def.getConfigInt("duration_ticks", DEFAULT_DURATION_TICKS)
                : DEFAULT_DURATION_TICKS;
    }

    public int getCooldownTicks() {
        AbilityDefinition def = getDefinition();
        return def != null
                ? def.getConfigInt("cooldown_ticks", DEFAULT_COOLDOWN_TICKS)
                : DEFAULT_COOLDOWN_TICKS;
    }

    public int getDurabilityCost() {
        AbilityDefinition def = getDefinition();
        return def != null
                ? def.getConfigInt("durability_cost", DEFAULT_DURABILITY_COST)
                : DEFAULT_DURABILITY_COST;
    }

    public double getSnifferEggChance() {
        AbilityDefinition def = getDefinition();
        return def != null
                ? def.getConfigDouble("sniffer_egg_chance", DEFAULT_SNIFFER_EGG_CHANCE)
                : DEFAULT_SNIFFER_EGG_CHANCE;
    }

    private Set<Block> getValidBlocks() {
        if (validBlocks == null) {
            validBlocks = new HashSet<>();
            AbilityDefinition def = getDefinition();
            if (def != null) {
                List<String> blockIds = def.getConfigStringList("valid_blocks");
                for (String blockId : blockIds) {
                    ResourceLocation loc = Ids.tryParse(blockId);
                    if (loc != null) {
                        Block block = BuiltInRegistries.BLOCK.getOptional(loc).orElse(null);
                        if (block != null && block != Blocks.AIR) {
                            validBlocks.add(block);
                        }
                    }
                }
            }

            if (validBlocks.isEmpty()) {
                validBlocks.add(Blocks.SUSPICIOUS_SAND);
                validBlocks.add(Blocks.SUSPICIOUS_GRAVEL);
            }
        }
        return validBlocks;
    }

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

        ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        if (headItem.isEmpty() || !headItem.is(NoseTags.NOSES)) {
            return false;
        }
        return EquippedNoseHelper.getEquippedAbilities(player)
                .hasAbility(AbilityConstants.PRECISE_SNIFFER);
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
            AromaAffect.LOGGER.debug(
                    "Cancelled sniffing session for {}", player.getName().getString());
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

    private boolean tickSniffingInternal(ServerPlayer player, BlockPos pos) {
        UUID playerId = player.getUUID();
        long currentTick = player.level().getGameTime();
        Vec3 playerPos = player.position();

        SniffingSession session = activeSessions.get(playerId);

        if (session == null || !session.targetPos.equals(pos)) {

            session = new SniffingSession(pos, playerPos, currentTick, 0);
            activeSessions.put(playerId, session);
            AromaAffect.LOGGER.debug(
                    "Started sniffing session for {} at {}", player.getName().getString(), pos);
        }

        if (playerPos.distanceTo(session.startPlayerPos) > MAX_MOVEMENT_DISTANCE) {
            onCancel(player);
            return false;
        }

        session =
                new SniffingSession(
                        session.targetPos,
                        session.startPlayerPos,
                        session.startTick,
                        session.progress + 1);
        activeSessions.put(playerId, session);

        if (session.progress % 10 == 0) {
            playSniffSound(player, pos);
        }

        spawnSniffParticles(player, pos);

        if (session.progress >= getDurationTicks()) {
            completeSniffing(player, pos);
            return true;
        }

        return false;
    }

    private void completeSniffing(ServerPlayer player, BlockPos pos) {
        ServerLevel level = (ServerLevel) player.level();

        activeSessions.remove(player.getUUID());

        cooldowns.put(player.getUUID(), level.getGameTime() + getCooldownTicks());

        if (!(level.getBlockEntity(pos) instanceof BrushableBlockEntity)) {
            AromaAffect.LOGGER.warn("No BrushableBlockEntity at {} during sniff completion", pos);
            return;
        }

        Block currentBlock = level.getBlockState(pos).getBlock();
        ResourceKey<LootTable> lootTableKey;
        double eggChance;

        if (currentBlock == Blocks.SUSPICIOUS_GRAVEL) {

            lootTableKey =
                    net.minecraft.world.level.storage.loot.BuiltInLootTables
                            .TRAIL_RUINS_ARCHAEOLOGY_COMMON;
            eggChance = 0.0;
        } else {

            lootTableKey =
                    net.minecraft.world.level.storage.loot.BuiltInLootTables
                            .DESERT_PYRAMID_ARCHAEOLOGY;
            eggChance = getSnifferEggChance();
        }

        ItemStack loot =
                SnifferLootTable.rollLoot(
                        level, pos, lootTableKey, player, level.getRandom(), eggChance);

        if (!loot.isEmpty()) {

            Vec3 spawnPos = Vec3.atCenterOf(pos).add(0, 0.5, 0);
            ItemEntity itemEntity = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, loot);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);

            playSuccessSound(player, pos, loot);

            if (loot.is(net.minecraft.world.item.Items.SNIFFER_EGG)) {
                spawnCelebrationParticles(level, pos);
                AromaAffect.LOGGER.info(
                        "{} found a Sniffer Egg using Precise Sniffer!",
                        player.getName().getString());
            }
        }

        damageNose(player);

        Block originalBlock = level.getBlockState(pos).getBlock();
        if (originalBlock instanceof BrushableBlock brushableBlock) {
            level.setBlock(pos, brushableBlock.getTurnsInto().defaultBlockState(), 3);
        } else {
            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
        }

        AromaAffect.LOGGER.debug(
                "Completed sniffing for {} at {}", player.getName().getString(), pos);
    }

    private void damageNose(ServerPlayer player) {
        ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headItem.isEmpty()) {
            headItem.hurtAndBreak(getDurabilityCost(), player, EquipmentSlot.HEAD);
        }
    }

    private void playSniffSound(ServerPlayer player, BlockPos pos) {
        player.level()
                .playSound(
                        null,
                        pos,
                        SoundEvents.SNIFFER_SNIFFING,
                        SoundSource.PLAYERS,
                        0.6f,
                        1.0f + (player.level().getRandom().nextFloat() * 0.2f - 0.1f));
    }

    private void playSuccessSound(ServerPlayer player, BlockPos pos, ItemStack loot) {
        if (loot.is(net.minecraft.world.item.Items.SNIFFER_EGG)) {

            player.level()
                    .playSound(
                            null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        } else {

            player.level()
                    .playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    private void spawnSniffParticles(ServerPlayer player, BlockPos pos) {
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 particlePos = Vec3.atCenterOf(pos);
            serverLevel.sendParticles(
                    ParticleTypes.WAX_ON,
                    particlePos.x,
                    particlePos.y + 0.5,
                    particlePos.z,
                    1,
                    0.2,
                    0.1,
                    0.2,
                    0.0);
        }
    }

    private void spawnCelebrationParticles(ServerLevel level, BlockPos pos) {
        Vec3 particlePos = Vec3.atCenterOf(pos);
        level.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                particlePos.x,
                particlePos.y + 0.5,
                particlePos.z,
                30,
                0.5,
                0.5,
                0.5,
                0.1);
    }

    public void cleanup() {}

    public void clearCache() {
        validBlocks = null;
    }

    private record SniffingSession(
            BlockPos targetPos, Vec3 startPlayerPos, long startTick, int progress) {}
}
