package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class NoseSmithTradeNetworking {
    private static final ResourceLocation NOSE_SMITH_OPEN_SHOP_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_smith_open_shop");

    private static boolean initialized = false;

    private NoseSmithTradeNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, NOSE_SMITH_OPEN_SHOP_PACKET_ID, (buf, context) -> {
            int entityId = buf.readVarInt();

            context.queue(() -> {
                Player player = context.getPlayer();
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    return;
                }

                Entity entity = serverPlayer.level().getEntity(entityId);
                if (!(entity instanceof NoseSmithEntity noseSmith)) {
                    return;
                }

                if (serverPlayer.distanceToSqr(noseSmith) > 8.0 * 8.0) {
                    return;
                }

                noseSmith.openShop(serverPlayer);
            });
        });
    }

    public static void sendOpenShop(RegistryAccess registryAccess, int noseSmithEntityId) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        buf.writeVarInt(noseSmithEntityId);
        NetworkManager.sendToServer(NOSE_SMITH_OPEN_SHOP_PACKET_ID, buf);
    }
}
