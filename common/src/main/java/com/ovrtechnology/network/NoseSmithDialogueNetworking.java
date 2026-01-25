package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.NoseSmithEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class NoseSmithDialogueNetworking {
    private static final ResourceLocation NOSE_SMITH_DIALOGUE_PACKET_ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "nose_smith_dialogue");

    private static boolean initialized = false;

    private NoseSmithDialogueNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, NOSE_SMITH_DIALOGUE_PACKET_ID, (buf, context) -> {
            int entityId = buf.readVarInt();
            boolean talking = buf.readBoolean();

            context.queue(() -> {
                Player player = context.getPlayer();
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    return;
                }

                Entity entity = serverPlayer.level().getEntity(entityId);
                if (!(entity instanceof NoseSmithEntity noseSmith)) {
                    return;
                }

                if (talking) {
                    noseSmith.keepDialogueAlive(serverPlayer);
                } else {
                    noseSmith.endDialogue(serverPlayer);
                }
            });
        });
    }

    public static void sendDialogueState(RegistryAccess registryAccess, int noseSmithEntityId, boolean talking) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        buf.writeVarInt(noseSmithEntityId);
        buf.writeBoolean(talking);
        NetworkManager.sendToServer(NOSE_SMITH_DIALOGUE_PACKET_ID, buf);
    }
}
