package com.ovrtechnology.tutorial.waypoint.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages static arrows that are always visible in the world.
 * Persisted via SavedData, synced to clients via S2C packet.
 */
public class TutorialStaticArrowManager extends SavedData {

    private final Map<String, BlockPos> arrows = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────────
    // Networking
    // ─────────────────────────────────────────────────────────────────────────────

    private static final ResourceLocation SYNC_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_static_arrows");

    /** Client-side arrow cache. */
    private static final Map<String, BlockPos> clientArrows = new ConcurrentHashMap<>();
    private static boolean networkInitialized = false;

    public static void initNetworking() {
        if (networkInitialized) return;
        networkInitialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_PACKET,
                (buf, context) -> {
                    int count = buf.readVarInt();
                    Map<String, BlockPos> newArrows = new HashMap<>();
                    for (int i = 0; i < count; i++) {
                        String id = buf.readUtf(256);
                        int x = buf.readInt();
                        int y = buf.readInt();
                        int z = buf.readInt();
                        newArrows.put(id, new BlockPos(x, y, z));
                    }
                    context.queue(() -> {
                        clientArrows.clear();
                        clientArrows.putAll(newArrows);
                    });
                });
    }

    public static void syncToPlayer(ServerPlayer player, ServerLevel level) {
        Map<String, BlockPos> all = getAllArrows(level);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeVarInt(all.size());
        for (var entry : all.entrySet()) {
            buf.writeUtf(entry.getKey(), 256);
            buf.writeInt(entry.getValue().getX());
            buf.writeInt(entry.getValue().getY());
            buf.writeInt(entry.getValue().getZ());
        }
        NetworkManager.sendToPlayer(player, SYNC_PACKET, buf);
    }

    public static void syncToAllPlayers(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            syncToPlayer(player, level);
        }
    }

    public static Map<String, BlockPos> getClientArrows() {
        return clientArrows;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SavedData persistence
    // ─────────────────────────────────────────────────────────────────────────────

    private record ArrowData(String id, int x, int y, int z) {}

    private static final Codec<ArrowData> ARROW_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(ArrowData::id),
                    Codec.INT.fieldOf("x").forGetter(ArrowData::x),
                    Codec.INT.fieldOf("y").forGetter(ArrowData::y),
                    Codec.INT.fieldOf("z").forGetter(ArrowData::z)
            ).apply(instance, ArrowData::new)
    );

    private static final Codec<TutorialStaticArrowManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ARROW_CODEC.listOf().fieldOf("arrows").forGetter(TutorialStaticArrowManager::getArrowDataList)
            ).apply(instance, TutorialStaticArrowManager::new)
    );

    static final SavedDataType<TutorialStaticArrowManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_static_arrows",
            TutorialStaticArrowManager::new,
            CODEC,
            null
    );

    public TutorialStaticArrowManager() {}

    private TutorialStaticArrowManager(List<ArrowData> dataList) {
        for (ArrowData data : dataList) {
            arrows.put(data.id, new BlockPos(data.x, data.y, data.z));
        }
    }

    private List<ArrowData> getArrowDataList() {
        List<ArrowData> list = new ArrayList<>();
        for (var entry : arrows.entrySet()) {
            list.add(new ArrowData(entry.getKey(), entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getZ()));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static CRUD API
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean addArrow(ServerLevel level, String id, BlockPos pos) {
        TutorialStaticArrowManager mgr = get(level);
        if (mgr.arrows.containsKey(id)) return false;
        mgr.arrows.put(id, pos);
        mgr.setDirty();
        return true;
    }

    public static boolean removeArrow(ServerLevel level, String id) {
        TutorialStaticArrowManager mgr = get(level);
        if (mgr.arrows.remove(id) != null) {
            mgr.setDirty();
            return true;
        }
        return false;
    }

    public static Map<String, BlockPos> getAllArrows(ServerLevel level) {
        return Collections.unmodifiableMap(get(level).arrows);
    }

    private static TutorialStaticArrowManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
