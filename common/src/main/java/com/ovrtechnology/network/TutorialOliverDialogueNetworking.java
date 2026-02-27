package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

    private static final ResourceLocation TUTORIAL_OLIVER_DIALOGUE_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_oliver_dialogue");

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

        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                TUTORIAL_OLIVER_DIALOGUE_PACKET_ID,
                (buf, context) -> {
                    int entityId = buf.readVarInt();
                    boolean talking = buf.readBoolean();

                    context.queue(() -> {
                        Player player = context.getPlayer();
                        if (!(player instanceof ServerPlayer serverPlayer)) {
                            return;
                        }

                        Entity entity = serverPlayer.level().getEntity(entityId);
                        if (!(entity instanceof TutorialOliverEntity oliver)) {
                            return;
                        }

                        if (talking) {
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
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        buf.writeVarInt(oliverEntityId);
        buf.writeBoolean(talking);
        NetworkManager.sendToServer(TUTORIAL_OLIVER_DIALOGUE_PACKET_ID, buf);
    }
}
