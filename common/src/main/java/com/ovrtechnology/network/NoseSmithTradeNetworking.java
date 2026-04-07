package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class NoseSmithTradeNetworking {

    public record NoseSmithOpenShopC2S(int entityId) implements CustomPacketPayload {
        public static final Type<NoseSmithOpenShopC2S> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_smith_open_shop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NoseSmithOpenShopC2S> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeVarInt(payload.entityId),
                buf -> new NoseSmithOpenShopC2S(buf.readVarInt())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean initialized = false;

    private NoseSmithTradeNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, NoseSmithOpenShopC2S.TYPE, NoseSmithOpenShopC2S.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        Player player = context.getPlayer();
                        if (!(player instanceof ServerPlayer serverPlayer)) {
                            return;
                        }

                        Entity entity = serverPlayer.level().getEntity(payload.entityId());
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
        NetworkManager.sendToServer(new NoseSmithOpenShopC2S(noseSmithEntityId));
    }
}
