package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.sniffer.SnifferTamingData;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Handles server-to-client networking for sniffer equipment synchronization.
 *
 * <p>Syncs saddle and decoration items so the client can render the correct texture.</p>
 */
public final class SnifferEquipmentNetworking {

    private static final ResourceLocation SNIFFER_EQUIPMENT_SYNC_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "sniffer_equipment_sync");

    private static boolean initialized = false;

    private SnifferEquipmentNetworking() {
    }

    /**
     * Initializes the networking handler.
     * Must be called on both client and server during mod initialization.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Register client-side receiver for equipment sync packets (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SNIFFER_EQUIPMENT_SYNC_ID, (buf, context) -> {
            UUID snifferUUID = buf.readUUID();
            boolean hasOwner = buf.readBoolean();
            UUID ownerUUID = hasOwner ? buf.readUUID() : null;
            ItemStack saddleItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            ItemStack decorationItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);

            context.queue(() -> {
                SnifferTamingData data = SnifferTamingData.get(snifferUUID);
                data.ownerUUID = ownerUUID;
                data.saddleItem = saddleItem;
                data.decorationItem = decorationItem;
                AromaAffect.LOGGER.debug("Received sniffer equipment sync for {}: owner={}, saddle={}, decoration={}",
                        snifferUUID, ownerUUID, !saddleItem.isEmpty(), !decorationItem.isEmpty());
            });
        });

        AromaAffect.LOGGER.info("SnifferEquipmentNetworking initialized");
    }

    /**
     * Sends sniffer equipment data to a specific player.
     *
     * @param player      the player to send the sync to
     * @param snifferUUID the sniffer's UUID
     * @param data        the sniffer's taming data
     */
    public static void sendEquipmentSync(ServerPlayer player, UUID snifferUUID, SnifferTamingData data) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.registryAccess()
        );

        buf.writeUUID(snifferUUID);
        buf.writeBoolean(data.ownerUUID != null);
        if (data.ownerUUID != null) {
            buf.writeUUID(data.ownerUUID);
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, data.saddleItem);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, data.decorationItem);

        NetworkManager.sendToPlayer(player, SNIFFER_EQUIPMENT_SYNC_ID, buf);
    }

    /**
     * Sends sniffer equipment data to all players tracking the sniffer.
     *
     * @param snifferUUID the sniffer's UUID
     * @param data        the sniffer's taming data
     * @param players     iterable of players to send to
     */
    public static void broadcastEquipmentSync(UUID snifferUUID, SnifferTamingData data, Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            sendEquipmentSync(player, snifferUUID, data);
        }
    }
}
