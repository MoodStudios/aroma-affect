package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Networking handler for Tutorial Oliver dialogue packets.
 * <p>
 * Handles keepalive packets from client to server to maintain
 * dialogue session state.
 */
public final class TutorialOliverDialogueNetworking {

    public record OliverDialogueC2S(int entityId, boolean talking) implements CustomPacketPayload {
        public static final Type<OliverDialogueC2S> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_oliver_dialogue"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OliverDialogueC2S> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.entityId);
                    buf.writeBoolean(payload.talking);
                },
                buf -> new OliverDialogueC2S(buf.readVarInt(), buf.readBoolean())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean initialized = false;

    private TutorialOliverDialogueNetworking() {
        // Utility class
    }

    /**
     * Initializes the networking receivers.
     * <p>
     * Should be called during mod initialization.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OliverDialogueC2S.TYPE, OliverDialogueC2S.STREAM_CODEC,
                (payload, context) -> {
                    context.queue(() -> {
                        Player player = context.getPlayer();
                        if (!(player instanceof ServerPlayer serverPlayer)) {
                            return;
                        }

                        Entity entity = serverPlayer.level().getEntity(payload.entityId());
                        if (!(entity instanceof TutorialOliverEntity oliver)) {
                            return;
                        }

                        if (payload.talking()) {
                            oliver.keepDialogueAlive(serverPlayer);
                        } else {
                            oliver.endDialogue(serverPlayer);
                        }
                    });
                }
        );

        AromaAffect.LOGGER.debug("Tutorial Oliver dialogue networking initialized");
    }

    /**
     * Sends a dialogue state packet to the server.
     * <p>
     * Called by the client-side dialogue screen to maintain the dialogue session
     * or signal when the dialogue ends.
     *
     * @param registryAccess the registry access for buffer creation
     * @param oliverEntityId the entity ID of the Tutorial Oliver
     * @param talking        {@code true} to keep dialogue alive, {@code false} to end
     */
    public static void sendDialogueState(RegistryAccess registryAccess, int oliverEntityId, boolean talking) {
        NetworkManager.sendToServer(new OliverDialogueC2S(oliverEntityId, talking));
    }
}
