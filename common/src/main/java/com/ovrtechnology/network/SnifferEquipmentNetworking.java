package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.sniffer.SnifferTamingData;
import com.ovrtechnology.util.Ids;
import dev.architectury.networking.NetworkManager;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class SnifferEquipmentNetworking {

    public record SnifferEquipmentSyncS2C(
            UUID snifferUUID, UUID ownerUUID, ItemStack saddleItem, ItemStack decorationItem)
            implements CustomPacketPayload {
        public static final Type<SnifferEquipmentSyncS2C> TYPE =
                new Type<>(Ids.mod("sniffer_equipment_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SnifferEquipmentSyncS2C>
                STREAM_CODEC =
                        StreamCodec.of(
                                (buf, payload) -> {
                                    buf.writeUUID(payload.snifferUUID);
                                    buf.writeBoolean(payload.ownerUUID != null);
                                    if (payload.ownerUUID != null) {
                                        buf.writeUUID(payload.ownerUUID);
                                    }
                                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.saddleItem);
                                    ItemStack.OPTIONAL_STREAM_CODEC.encode(
                                            buf, payload.decorationItem);
                                },
                                buf -> {
                                    UUID snifferUUID = buf.readUUID();
                                    boolean hasOwner = buf.readBoolean();
                                    UUID ownerUUID = hasOwner ? buf.readUUID() : null;
                                    ItemStack saddleItem =
                                            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                                    ItemStack decorationItem =
                                            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                                    return new SnifferEquipmentSyncS2C(
                                            snifferUUID, ownerUUID, saddleItem, decorationItem);
                                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static boolean initialized = false;

    private SnifferEquipmentNetworking() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                SnifferEquipmentSyncS2C.TYPE,
                SnifferEquipmentSyncS2C.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(
                            () -> {
                                SnifferTamingData data =
                                        SnifferTamingData.get(payload.snifferUUID());
                                data.ownerUUID = payload.ownerUUID();
                                data.saddleItem = payload.saddleItem();
                                data.decorationItem = payload.decorationItem();
                                AromaAffect.LOGGER.debug(
                                        "Received sniffer equipment sync for {}: owner={}, saddle={}, decoration={}",
                                        payload.snifferUUID(),
                                        payload.ownerUUID(),
                                        !payload.saddleItem().isEmpty(),
                                        !payload.decorationItem().isEmpty());
                            });
                });

        AromaAffect.LOGGER.info("SnifferEquipmentNetworking initialized");
    }

    public static void sendEquipmentSync(
            ServerPlayer player, UUID snifferUUID, SnifferTamingData data) {
        if (!NetworkManager.canPlayerReceive(player, SnifferEquipmentSyncS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(
                player,
                new SnifferEquipmentSyncS2C(
                        snifferUUID, data.ownerUUID, data.saddleItem, data.decorationItem));
    }

    public static void broadcastEquipmentSync(
            UUID snifferUUID, SnifferTamingData data, Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            sendEquipmentSync(player, snifferUUID, data);
        }
    }
}
