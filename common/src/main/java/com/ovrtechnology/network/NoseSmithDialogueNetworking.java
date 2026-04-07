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

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, NoseSmithDialogueC2S.TYPE, NoseSmithDialogueC2S.STREAM_CODEC,
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

                        if (payload.talking()) {
                            noseSmith.keepDialogueAlive(serverPlayer);
                        } else {
                            noseSmith.endDialogue(serverPlayer);
                        }
                    });
                });
    }

    public static void sendDialogueState(RegistryAccess registryAccess, int noseSmithEntityId, boolean talking) {
        NetworkManager.sendToServer(new NoseSmithDialogueC2S(noseSmithEntityId, talking));
    }
}
