package com.ovrtechnology.tutorial.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Manages tutorial trades for a world.
 * <p>
 * Trades are stored per-world and persist across server restarts.
 * Each trade supports multiple input items and a single output item.
 */
public class TutorialTradeManager extends SavedData {

    private final Map<String, TutorialTrade> trades = new HashMap<>();

    private static final Codec<InputEntryData> INPUT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("itemId").forGetter(InputEntryData::itemId),
                    Codec.INT.fieldOf("count").forGetter(InputEntryData::count)
            ).apply(instance, InputEntryData::new)
    );

    private static final Codec<TradeData> TRADE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(TradeData::id),
                    INPUT_CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(TradeData::inputs),
                    Codec.STRING.optionalFieldOf("outputItemId", "").forGetter(TradeData::outputItemId),
                    Codec.INT.optionalFieldOf("outputCount", 0).forGetter(TradeData::outputCount),
                    Codec.STRING.optionalFieldOf("onCompleteWaypointId", "").forGetter(d -> d.onCompleteWaypointId != null ? d.onCompleteWaypointId : ""),
                    Codec.STRING.optionalFieldOf("onCompleteCinematicId", "").forGetter(d -> d.onCompleteCinematicId != null ? d.onCompleteCinematicId : ""),
                    Codec.STRING.optionalFieldOf("onCompleteAnimationId", "").forGetter(d -> d.onCompleteAnimationId != null ? d.onCompleteAnimationId : ""),
                    Codec.STRING.optionalFieldOf("onCompleteOliverAction", "").forGetter(d -> d.onCompleteOliverAction != null ? d.onCompleteOliverAction : "")
            ).apply(instance, TradeData::new)
    );

    private static final Codec<TutorialTradeManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    TRADE_CODEC.listOf().fieldOf("trades").forGetter(TutorialTradeManager::getTradeDataList)
            ).apply(instance, TutorialTradeManager::new)
    );

    static final SavedDataType<TutorialTradeManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_trades",
            TutorialTradeManager::new,
            CODEC,
            null
    );

    public TutorialTradeManager() {
    }

    private TutorialTradeManager(List<TradeData> dataList) {
        for (TradeData data : dataList) {
            List<TutorialTrade.InputEntry> inputs = new ArrayList<>();
            for (InputEntryData inputData : data.inputs) {
                inputs.add(new TutorialTrade.InputEntry(inputData.itemId, inputData.count));
            }
            String wpId = data.onCompleteWaypointId;
            String cinId = data.onCompleteCinematicId;
            String animId = data.onCompleteAnimationId;
            String olAction = data.onCompleteOliverAction;
            TutorialTrade trade = new TutorialTrade(
                    data.id,
                    inputs,
                    data.outputItemId,
                    data.outputCount,
                    wpId != null && !wpId.isEmpty() ? wpId : null,
                    cinId != null && !cinId.isEmpty() ? cinId : null,
                    animId != null && !animId.isEmpty() ? animId : null,
                    olAction != null && !olAction.isEmpty() ? olAction : null
            );
            trades.put(data.id, trade);
        }
    }

    private List<TradeData> getTradeDataList() {
        List<TradeData> list = new ArrayList<>();
        for (TutorialTrade t : trades.values()) {
            List<InputEntryData> inputs = new ArrayList<>();
            for (TutorialTrade.InputEntry entry : t.getInputs()) {
                inputs.add(new InputEntryData(entry.itemId(), entry.count()));
            }
            list.add(new TradeData(
                    t.getId(),
                    inputs,
                    t.getOutputItemId(),
                    t.getOutputCount(),
                    t.getOnCompleteWaypointId() != null ? t.getOnCompleteWaypointId() : "",
                    t.getOnCompleteCinematicId() != null ? t.getOnCompleteCinematicId() : "",
                    t.getOnCompleteAnimationId() != null ? t.getOnCompleteAnimationId() : "",
                    t.getOnCompleteOliverAction() != null ? t.getOnCompleteOliverAction() : ""
            ));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Trade CRUD
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean createTrade(ServerLevel level, String id) {
        TutorialTradeManager manager = get(level);
        if (manager.trades.containsKey(id)) {
            return false;
        }
        manager.trades.put(id, new TutorialTrade(id));
        manager.setDirty();
        AromaAffect.LOGGER.info("Created tutorial trade {}", id);
        return true;
    }

    public static boolean deleteTrade(ServerLevel level, String id) {
        TutorialTradeManager manager = get(level);
        if (manager.trades.remove(id) != null) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Deleted tutorial trade {}", id);
            return true;
        }
        return false;
    }

    public static Optional<TutorialTrade> getTrade(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).trades.get(id));
    }

    public static Set<String> getAllTradeIds(ServerLevel level) {
        return new HashSet<>(get(level).trades.keySet());
    }

    /**
     * Adds an input item to a trade (from held item).
     * If the same item type already exists, it replaces it.
     */
    public static boolean addInput(ServerLevel level, String id, ItemStack item) {
        TutorialTradeManager manager = get(level);
        TutorialTrade trade = manager.trades.get(id);
        if (trade == null) {
            return false;
        }
        String itemId = item.getItem().builtInRegistryHolder().key().location().toString();
        trade.addInput(itemId, item.getCount());
        manager.setDirty();
        AromaAffect.LOGGER.info("Added trade {} input: {}x {}", id, item.getCount(), itemId);
        return true;
    }

    /**
     * Removes an input item from a trade by item ID.
     */
    public static boolean removeInput(ServerLevel level, String id, String itemId) {
        TutorialTradeManager manager = get(level);
        TutorialTrade trade = manager.trades.get(id);
        if (trade == null) {
            return false;
        }
        if (trade.removeInput(itemId)) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Removed trade {} input: {}", id, itemId);
            return true;
        }
        return false;
    }

    /**
     * Clears all inputs from a trade.
     */
    public static boolean clearInputs(ServerLevel level, String id) {
        TutorialTradeManager manager = get(level);
        TutorialTrade trade = manager.trades.get(id);
        if (trade == null) {
            return false;
        }
        trade.clearInputs();
        manager.setDirty();
        AromaAffect.LOGGER.info("Cleared trade {} inputs", id);
        return true;
    }

    public static boolean setOutput(ServerLevel level, String id, ItemStack item) {
        TutorialTradeManager manager = get(level);
        TutorialTrade trade = manager.trades.get(id);
        if (trade == null) {
            return false;
        }
        String itemId = item.getItem().builtInRegistryHolder().key().location().toString();
        trade.setOutputItemId(itemId);
        trade.setOutputCount(item.getCount());
        manager.setDirty();
        AromaAffect.LOGGER.info("Set trade {} output: {}x {}", id, item.getCount(), itemId);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - On-Complete Hooks
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean setOnCompleteWaypoint(ServerLevel level, String id, String waypointId) {
        TutorialTradeManager manager = get(level);
        TutorialTrade trade = manager.trades.get(id);
        if (trade == null) return false;
        trade.setOnCompleteWaypointId(waypointId);
        manager.setDirty();
        return true;
    }

    public static boolean setOnCompleteCinematic(ServerLevel level, String id, String cinematicId) {
        TutorialTradeManager manager = get(level);
        TutorialTrade trade = manager.trades.get(id);
        if (trade == null) return false;
        trade.setOnCompleteCinematicId(cinematicId);
        manager.setDirty();
        return true;
    }

    public static boolean setOnCompleteAnimation(ServerLevel level, String id, String animationId) {
        TutorialTradeManager manager = get(level);
        TutorialTrade trade = manager.trades.get(id);
        if (trade == null) return false;
        trade.setOnCompleteAnimationId(animationId);
        manager.setDirty();
        return true;
    }

    public static boolean setOnCompleteOliverAction(ServerLevel level, String id, String action) {
        TutorialTradeManager manager = get(level);
        TutorialTrade trade = manager.trades.get(id);
        if (trade == null) return false;
        trade.setOnCompleteOliverAction(action);
        manager.setDirty();
        return true;
    }

    public static boolean clearOnComplete(ServerLevel level, String id) {
        TutorialTradeManager manager = get(level);
        TutorialTrade trade = manager.trades.get(id);
        if (trade == null) return false;
        trade.setOnCompleteWaypointId(null);
        trade.setOnCompleteCinematicId(null);
        trade.setOnCompleteAnimationId(null);
        trade.setOnCompleteOliverAction(null);
        manager.setDirty();
        return true;
    }

    private static TutorialTradeManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal data records
    // ─────────────────────────────────────────────────────────────────────────────

    private record InputEntryData(String itemId, int count) {}

    private record TradeData(
            String id,
            List<InputEntryData> inputs,
            String outputItemId,
            int outputCount,
            String onCompleteWaypointId,
            String onCompleteCinematicId,
            String onCompleteAnimationId,
            String onCompleteOliverAction
    ) {}
}
