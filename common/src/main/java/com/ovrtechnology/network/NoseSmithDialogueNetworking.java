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

public final class NoseSmithDialogueNetworking {

    public record NoseSmithDialogueC2S(int entityId, boolean talking) implements CustomPacketPayload {
        public static final Type<NoseSmithDialogueC2S> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_smith_dialogue"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NoseSmithDialogueC2S> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.entityId);
                    buf.writeBoolean(payload.talking);
                },
                buf -> new NoseSmithDialogueC2S(buf.readVarInt(), buf.readBoolean())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean initialized = false;

    private NoseSmithDialogueNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        Balm.networking().registerServerboundPacket(
                NoseSmithDialogueC2S.TYPE,
                NoseSmithDialogueC2S.class,
                NoseSmithDialogueC2S.STREAM_CODEC,
                (serverPlayer, payload) -> {
                    Entity entity = serverPlayer.level().getEntity(payload.entityId());
                    if (!(entity instanceof NoseSmithEntity noseSmith)) {
                        return;
                    }

                    if (payload.talking()) {
                        noseSmith.keepDialogueAlive(serverPlayer);
                    } else {
                        noseSmith.endDialogue(serverPlayer);
                    }
                });
    }

    public static void sendDialogueState(RegistryAccess registryAccess, int noseSmithEntityId, boolean talking) {
        Balm.networking().sendToServer(new NoseSmithDialogueC2S(noseSmithEntityId, talking));
    }
}
