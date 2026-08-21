package com.ovrtechnology.trigger.event;

import com.google.gson.JsonObject;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.ScentEventNetworking;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;

/**
 * Dispatch hub for server-side event-trigger scents.
 *
 * <p>On Balm (26.1) there is no free-registration event API like Architectury's
 * {@code BlockEvent.BREAK}. Detection is therefore done via mixins
 * (Block/Living/Craft/Smelt/Advancement/AnimalTame) that call the public
 * {@code on*} handlers here. The matching/cooldown/packet-dispatch logic is the
 * same as the Architectury branches.</p>
 */
public final class ServerEventBusHandler {

    public static final String TT_BLOCK_BROKEN = "BLOCK_BROKEN";
    public static final String TT_MOB_KILLED = "MOB_KILLED";
    public static final String TT_ADVANCEMENT_OBTAINED = "ADVANCEMENT_OBTAINED";
    public static final String TT_ANIMAL_TAMED = "ANIMAL_TAMED";
    public static final String TT_ITEM_CRAFTED = "ITEM_CRAFTED";
    public static final String TT_ITEM_SMELTED = "ITEM_SMELTED";
    public static final String TT_ITEM_EQUIPPED = "ITEM_EQUIPPED";
    public static final String TT_FISHING_PULLED = "FISHING_PULLED";
    public static final String TT_TRADE_COMPLETED = "TRADE_COMPLETED";
    public static final String TT_ANVIL_USED = "ANVIL_USED";
    public static final String TT_SNIFFER_DUG = "SNIFFER_DUG";
    public static final String TT_FLINT_USED = "FLINT_USED";
    public static final String TT_FIREWORK_USED_ON = "FIREWORK_USED_ON";
    public static final String TT_FIREWORK_USED = "FIREWORK_USED";
    public static final String TT_SEED_PLANTED = "SEED_PLANTED";
    public static final String TT_DISC_JUKEBOX = "DISC_JUKEBOX";
    public static final String TT_TOTEM_USE = "TOTEM_USE";
    public static final String TT_ENTITY_RIDE_SHOULDER = "RIDE_SHOULDER";
    public static final String TT_PLAYER_DEATH = "PLAYER_DEATH";
    public static final String TT_COOKING_STARTED = "COOKING_STARTED";
    public static final String TT_COPPER_OXIDIZE = "COPPER_OXIDIZE";
    public static final String TT_SCULK_SHRIEK = "SCULK_SHRIEK";
    public static final String TT_MINECART_OVERLAP_POWERED_RAIL = "MINECART_OVERLAP_POWERED_RAIL";
    public static final String TT_REDSTONE_ACTIVATED = "REDSTONE_ACTIVATED";

    /** Seed/crop items whose placement counts as "planting" (in a regular class, so safe to init). */
    private static final Set<Item> SEED_ITEMS = Set.of(
            Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS,
            Items.CARROT, Items.POTATO, Items.NETHER_WART, Items.TORCHFLOWER_SEEDS,
            Items.PITCHER_POD, Items.SWEET_BERRIES);

    /** Item key matched for cake-block eating; see {@link #onCakeEaten(ServerPlayer)}. */
    private static final String CAKE_ITEM_KEY = "minecraft:cake";

    private static final Map<UUID, Map<String, Long>> serverCooldowns = new HashMap<>();

    private static boolean initialized = false;

    private ServerEventBusHandler() {}

    /**
     * No event registrations to perform here on Balm — server detection is
     * mixin-driven. Kept for parity with the other branches' init flow.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        AromaAffect.LOGGER.info("ServerEventBusHandler initialized (mixin-driven on Balm)");
    }

    public static void onAnimalTamed(Animal animal, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || animal == null) return;
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());
        if (entityId == null) return;
        String entityKey = entityId.toString();

        dispatch(
                serverPlayer,
                TT_ANIMAL_TAMED,
                def -> matchesEntitySimple(def.getConditions(), entityKey));
    }

    private static boolean matchesEntitySimple(JsonObject conditions, String entityKey) {
        List<String> ids = EventConditionUtils.getStringArray(conditions, "entity_ids");
        if (ids.isEmpty()) {
            return EventConditionUtils.getBoolean(conditions, "default", true);
        }
        return ids.contains(entityKey);
    }

    public static void onItemCrafted(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return;
        String itemKey = itemId.toString();

        dispatch(
                serverPlayer,
                TT_ITEM_CRAFTED,
                def -> matchesItemSimple(def.getConditions(), itemKey));
    }

    public static void onItemSmelted(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return;
        String itemKey = itemId.toString();

        dispatch(
                serverPlayer,
                TT_ITEM_SMELTED,
                def -> matchesItemSimple(def.getConditions(), itemKey));
    }

    private static boolean matchesItemSimple(JsonObject conditions, String itemKey) {
        List<String> ids = EventConditionUtils.getStringArray(conditions, "item_ids");
        if (!ids.isEmpty() && ids.contains(itemKey)) return true;
        List<String> tagFilters = EventConditionUtils.getStringArray(conditions, "item_tags");
        if (!tagFilters.isEmpty()) {
            for (String t : tagFilters) {
                if (t.startsWith("#") && itemKey.contains(t.substring(1))) return true;
            }
        }
        return EventConditionUtils.getBoolean(conditions, "default", false);
    }

    /** Flint &amp; steel used. */
    public static void onFlintUsed(ServerPlayer player) {
        fireSimpleEvent(player, TT_FLINT_USED);
    }

    /** When Jukebox gets a music disc placed into it */
    public static void onJukeboxUsed(ServerPlayer player, BlockState state, ItemStack stack) {
        if (player == null || stack == null) return;
        if (!stack.has(DataComponents.JUKEBOX_PLAYABLE) || state.getValue(JukeboxBlock.HAS_RECORD)) return;

        fireSimpleEvent(player, TT_DISC_JUKEBOX);
    }

    /**  */
    public static void onEquippedItem(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemKey = itemId.toString();

        dispatch(
                player,
                TT_ITEM_EQUIPPED,
                def -> matchesItemSimple(def.getConditions(), itemKey));
    }

    /**
     * A bite was taken out of a cake block (plain or candle cake).
     *
     * <p>Cake is the one vanilla food that never reaches
     * {@code PlayerItemUseEventDispatcher}: the item is a {@code BlockItem} with no
     * {@code FOOD} data component, and eating happens through block interaction
     * rather than {@code completeUsingItem}. It is dispatched here instead, under the
     * same {@code PLAYER_FOOD_EATEN} trigger type, so a food event can keep listing
     * {@code minecraft:cake} in its {@code item_ids} like any other food.</p>
     */
    public static void onCakeEaten(ServerPlayer player) {
        if (player == null) return;
        dispatch(
                player,
                PlayerStateTickHandler.TT_PLAYER_FOOD_EATEN,
                def -> matchesItemSimple(def.getConditions(), CAKE_ITEM_KEY));
    }

    /** A seed/crop item was placed (planting). Filtered to {@link #SEED_ITEMS}. */
    public static void onSeedPlanted(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;
        if (!SEED_ITEMS.contains(stack.getItem())) return;
        fireSimpleEvent(player, TT_SEED_PLANTED);
    }

    public static void onCopperOxidize(ServerPlayer player) {
        if (player == null) return;
        fireSimpleEvent(player, TT_COPPER_OXIDIZE);
    }

    /**
     * A lever was flipped or a button was pressed. Both are the moment redstone
     * actually switches on, so they share one trigger type; which of the two carried
     * the signal is irrelevant to the scent.
     *
     * <p>Only fires when the switch actually drives something — see
     * {@link #drivesRedstone}. A decorative button on a wall is not a redstone
     * moment, and firing on it made the scent meaningless.</p>
     *
     * <p>{@code player} is nullable on purpose: buttons can also be pressed by
     * arrows, and a puff with nobody to smell it is simply dropped.</p>
     */
    public static void onRedstoneActivated(Player player, Level level, BlockPos pos, BlockState state) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (level == null || pos == null || state == null) return;
        if (!drivesRedstone(level, pos, state)) return;
        fireSimpleEvent(serverPlayer, TT_REDSTONE_ACTIVATED);
    }

    /**
     * Whether flipping the switch at {@code pos} can reach anything that reacts to
     * redstone.
     *
     * <p>Checks exactly the positions the signal can reach: the switch's own six
     * neighbours (weak power, e.g. dust running away from it), plus the six
     * neighbours of the block it is attached to, since a switch strongly powers its
     * support and a solid support re-emits that to everything touching it. A
     * non-conducting support (glass, a fence) stops there, so it is skipped.</p>
     */
    private static boolean drivesRedstone(Level level, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            if (consumesRedstone(level.getBlockState(pos.relative(dir)))) return true;
        }

        Direction away = connectedDirection(state);
        if (away == null) return false;

        BlockPos support = pos.relative(away.getOpposite());
        BlockState supportState = level.getBlockState(support);
        if (!supportState.isRedstoneConductor(level, support)) return false;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = support.relative(dir);
            if (neighbor.equals(pos)) continue;
            if (consumesRedstone(level.getBlockState(neighbor))) return true;
        }
        return false;
    }

    /**
     * Direction a face-attached switch points away from its support, mirroring the
     * protected {@code FaceAttachedHorizontalDirectionalBlock.getConnectedDirection}.
     */
    private static Direction connectedDirection(BlockState state) {
        if (!state.hasProperty(FaceAttachedHorizontalDirectionalBlock.FACE)) return null;
        AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        if (face == AttachFace.CEILING) return Direction.DOWN;
        if (face == AttachFace.FLOOR) return Direction.UP;
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return null;
        return state.getValue(HorizontalDirectionalBlock.FACING);
    }

    /**
     * Whether a block does something when it receives a redstone signal. Matching on
     * the family classes rather than on ids covers every wood and copper variant at
     * once, and keeps modded blocks that extend them working for free.
     */
    private static boolean consumesRedstone(BlockState state) {
        Block block = state.getBlock();
        return block instanceof RedStoneWireBlock
                || block instanceof DiodeBlock          // repeater + comparator
                || block instanceof RedstoneTorchBlock  // standing + wall
                || block instanceof RedstoneLampBlock
                || block instanceof PistonBaseBlock
                || block instanceof DispenserBlock      // dispenser + dropper
                || block instanceof HopperBlock
                || block instanceof CrafterBlock
                || block instanceof ObserverBlock
                || block instanceof NoteBlock
                || block instanceof TntBlock
                || block instanceof BellBlock
                || block instanceof CopperBulbBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof BaseRailBlock;
    }

    public static void onSculkShriek(ServerPlayer player) {
        if (player == null) return;

        fireSimpleEvent(player, TT_SCULK_SHRIEK);
    }

    public static void onTotemUse(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        fireSimpleEvent(player, TT_TOTEM_USE);
    }

    /**
     * The player started cooking on a campfire (placed food onto it). Fires COOKING_STARTED
     * (→ Smoky) — the "process begins" scent. The furnace take-result events (Savory Spice /
     * Terra Silva) are a separate, later moment and are unaffected.
     */
    public static void onCampfireCook(ServerPlayer player) {
        fireSimpleEvent(player, TT_COOKING_STARTED);
    }

    /** Radius (blocks) around an igniting furnace within which players receive the scent. */
    private static final double FURNACE_IGNITE_RADIUS = 16.0;

    /**
     * A furnace / blast furnace / smoker just ignited (transitioned to lit). Block entities have
     * no owner, so — like {@link com.ovrtechnology.network.OmaraDeviceNetworking#broadcastPuff} —
     * the scent goes to every nearby player. Called at most once per
     * ignition (see {@code FurnaceIgniteServerMixin}); the per-player event cooldown in
     * {@link #dispatch} collapses the re-ignitions between items within one cook session.
     */
    public static void onFurnaceIgnited(Level level, BlockPos pos) {
        if (level == null || level.isClientSide() || pos == null) return;
        AABB area = new AABB(pos).inflate(FURNACE_IGNITE_RADIUS);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            fireSimpleEvent(player, TT_COOKING_STARTED);
        }
    }

    public static void fireSimpleEvent(ServerPlayer player, String triggerType) {
        if (player == null || triggerType == null) return;
        dispatch(player, triggerType, def -> true);
    }

    public static void onBlockBroken(ServerPlayer player, BlockState state) {
        if (player == null || state == null) return;
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) return;
        String blockKey = blockId.toString();
        Set<String> tags = collectBlockTags(state);

        dispatch(player, TT_BLOCK_BROKEN, def -> matchesBlock(def.getConditions(), blockKey, tags));
    }

    private static Set<String> collectBlockTags(BlockState state) {
        Set<String> out = new HashSet<>();
        state.getBlock().builtInRegistryHolder().tags()
                .forEach(
                        tagKey -> {
                            Identifier rl = tagKey.location();
                            if (rl != null) {
                                out.add("#" + rl);
                            }
                        });
        return out;
    }

    private static boolean matchesBlock(JsonObject conditions, String blockKey, Set<String> tags) {
        List<String> ids = EventConditionUtils.getStringArray(conditions, "block_ids");
        if (!ids.isEmpty() && ids.contains(blockKey)) return true;

        List<String> tagFilters = EventConditionUtils.getStringArray(conditions, "block_tags");
        for (String t : tagFilters) {
            if (tags.contains(t)) return true;
        }
        return false;
    }

    public static void onLivingDeath(LivingEntity entity, DamageSource source) {
        if (entity == null || source == null) return;

        if (entity instanceof ServerPlayer victim) {
            fireSimpleEvent(victim, TT_PLAYER_DEATH);
        }

        ServerPlayer killer = resolvePlayerSource(source);
        if (killer == null) return;
        if (entity == killer) return;

        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId == null) return;
        String entityKey = entityId.toString();
        Set<String> tags = collectEntityTags(entity);

        dispatch(killer, TT_MOB_KILLED, def -> matchesEntity(def.getConditions(), entityKey, tags));
    }

    private static ServerPlayer resolvePlayerSource(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer p) return p;
        if (source.getDirectEntity() instanceof ServerPlayer p) return p;
        return null;
    }

    private static Set<String> collectEntityTags(LivingEntity entity) {
        Set<String> out = new HashSet<>();
        entity.getType()
                .builtInRegistryHolder()
                .tags()
                .forEach(
                        tagKey -> {
                            Identifier rl = tagKey.location();
                            if (rl != null) {
                                out.add("#" + rl);
                            }
                        });
        return out;
    }

    private static boolean matchesEntity(
            JsonObject conditions, String entityKey, Set<String> tags) {
        List<String> ids = EventConditionUtils.getStringArray(conditions, "entity_ids");
        if (!ids.isEmpty() && ids.contains(entityKey)) return true;

        List<String> tagFilters = EventConditionUtils.getStringArray(conditions, "entity_tags");
        for (String t : tagFilters) {
            if (tags.contains(t)) return true;
        }
        return EventConditionUtils.getBoolean(conditions, "default", false);
    }

    /**
     * An advancement was completed.
     *
     * <p>Only advancements the player can actually see are eligible. Recipe unlocks
     * are advancements too -- vanilla ships 1562 of them against 126 real ones -- and
     * they carry no {@code DisplayInfo}, so filtering on {@link Advancement#display()}
     * keeps routine recipe unlocks from firing a scent. Root advancements are category
     * headers rather than achievements, so they are excluded as well.</p>
     */
    public static void onAdvancement(ServerPlayer player, AdvancementHolder advancement) {
        if (player == null || advancement == null) return;
        Identifier advId = advancement.id();
        if (advId == null) return;
        String advKey = advId.toString();

        if (advancement.value().display().isEmpty()) {
            return;
        }

        // Match on the path: the full id reads "minecraft:recipes/...", so a
        // substring test for "/recipes/" would never hit.
        String path = advId.getPath();
        if (path.startsWith("recipes/") || path.equals("root") || path.endsWith("/root")) {
            return;
        }

        dispatch(
                player,
                TT_ADVANCEMENT_OBTAINED,
                def -> matchesAdvancement(def.getConditions(), advKey));
    }

    private static boolean matchesAdvancement(JsonObject conditions, String advKey) {
        List<String> ids = EventConditionUtils.getStringArray(conditions, "advancement_ids");
        if (!ids.isEmpty()) return ids.contains(advKey);
        return EventConditionUtils.getBoolean(conditions, "default", false);
    }

    private static void dispatch(
            ServerPlayer player, String triggerType, Predicate<EventDefinition> matcher) {
        EventTriggersConfig config = EventTriggersConfig.getInstance();
        if (!config.isEventTriggersEnabled()) return;

        List<EventDefinition> candidates = EventDefinitionLoader.getByTriggerType(triggerType);
        if (candidates.isEmpty()) return;

        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns =
                serverCooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>());

        for (EventDefinition def : candidates) {
            if (!matcher.test(def)) continue;

            long cooldown =
                    Math.max(def.getCooldownMs(), config.getCategoryCooldownMs(def.getCategory()));
            Long lastTime = playerCooldowns.get(def.getId());
            if (lastTime != null && (now - lastTime) < cooldown) {
                return;
            }

            playerCooldowns.put(def.getId(), now);
            ScentEventNetworking.sendEvent(player, def.getId());
            AromaAffect.LOGGER.debug(
                    "[Events] dispatched {} for {} via packet (trigger {})",
                    def.getId(),
                    player.getName().getString(),
                    triggerType);
            return;
        }
    }

    public static void clearPlayerCooldowns(UUID playerId) {
        serverCooldowns.remove(playerId);
    }
}
