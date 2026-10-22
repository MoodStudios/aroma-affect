package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import net.blay09.mods.balm.Balm;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

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

        Balm.networking().registerServerboundPacket(
                NoseSmithOpenShopC2S.TYPE,
                NoseSmithOpenShopC2S.class,
                NoseSmithOpenShopC2S.STREAM_CODEC,
                (serverPlayer, payload) -> {
                    Entity entity = serverPlayer.level().getEntity(payload.entityId());
                    if (!(entity instanceof NoseSmithEntity noseSmith)) {
                        return;
                    }

                    if (serverPlayer.distanceToSqr(noseSmith) > 8.0 * 8.0) {
                        return;
                    }

                    noseSmith.openShop(serverPlayer);
                });
    }

    public static void sendOpenShop(RegistryAccess registryAccess, int noseSmithEntityId) {
        Balm.networking().sendToServer(new NoseSmithOpenShopC2S(noseSmithEntityId));
    }
}
