package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking for the tutorial scent counter HUD.
 * <p>
 * S2C: Activate/deactivate the scent counter on the client.
 * The counter tracks unique scents detected after activation.
 */
public final class TutorialScentCounterNetworking {

    private static final ResourceLocation SCENT_COUNTER_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_scent_counter");

    private static boolean initialized = false;

    /** Client-side: whether the scent counter is active. */
    private static boolean clientCounterActive = false;

    /** Client-side: max scents to display. */
    private static int clientMaxScents = 16;

    private TutorialScentCounterNetworking() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Receive counter activation state
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                SCENT_COUNTER_PACKET,
                (buf, context) -> {
                    boolean active = buf.readBoolean();
                    int maxScents = buf.readVarInt();
                    context.queue(() -> {
                        clientCounterActive = active;
                        clientMaxScents = maxScents;
                        if (active) {
                            // Reset the counter on activation
                            resetClientCounter();
                        }
                    });
                }
        );

        AromaAffect.LOGGER.debug("Tutorial scent counter networking initialized");
    }

    /**
     * Activates the scent counter for a player.
     */
    public static void sendActivate(ServerPlayer player, int maxScents) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), player.registryAccess());
        buf.writeBoolean(true);
        buf.writeVarInt(maxScents);
        NetworkManager.sendToPlayer(player, SCENT_COUNTER_PACKET, buf);
    }

    /**
     * Deactivates the scent counter for a player.
     */
    public static void sendDeactivate(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), player.registryAccess());
        buf.writeBoolean(false);
        buf.writeVarInt(0);
        NetworkManager.sendToPlayer(player, SCENT_COUNTER_PACKET, buf);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Client-side state
    // ─────────────────────────────────────────────────────────────────────────

    private static final java.util.Set<String> clientDetectedScents =
            java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());

    public static boolean isClientCounterActive() {
        return clientCounterActive;
    }

    public static int getClientMaxScents() {
        return clientMaxScents;
    }

    public static int getClientDetectedCount() {
        return Math.min(clientDetectedScents.size(), clientMaxScents);
    }

    /**
     * Called when a scent is triggered on the client.
     * Adds it to the detected set if the counter is active.
     */
    private static boolean allScentsVictoryFired = false;
    private static long allScentsVictoryScheduledAt = 0;

    public static void onScentTriggered(String scentName) {
        if (!clientCounterActive) return;
        if (scentName == null || scentName.isEmpty()) return;
        if (clientDetectedScents.size() >= clientMaxScents) return;

        boolean wasComplete = clientDetectedScents.size() >= clientMaxScents;
        clientDetectedScents.add(scentName);
        boolean isComplete = clientDetectedScents.size() >= clientMaxScents;

        // Schedule victory 10 seconds after discovering all scents
        if (!wasComplete && isComplete && !allScentsVictoryFired) {
            allScentsVictoryScheduledAt = System.currentTimeMillis() + 10000;
        }
    }

    /**
     * Called every client tick to check if the delayed victory should fire.
     */
    public static void tickClient() {
        if (allScentsVictoryFired || allScentsVictoryScheduledAt == 0) return;
        if (System.currentTimeMillis() < allScentsVictoryScheduledAt) return;

        allScentsVictoryFired = true;
        allScentsVictoryScheduledAt = 0;

        // Fire Sweet scent + victory sound + congratulations message
        try {
            var trigger = com.ovrtechnology.trigger.ScentTrigger.create(
                    "Sweet", com.ovrtechnology.trigger.ScentTriggerSource.CUSTOM_EVENT,
                    com.ovrtechnology.trigger.ScentPriority.HIGH, 200, 1.0);
            com.ovrtechnology.trigger.ScentTriggerManager.getInstance().trigger(trigger);
            com.ovrtechnology.trigger.client.ScentPuffOverlay.onScentPuff("Sweet", 1.0);

            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§6§l✦ Congratulations! §eYou discovered all 16 scents! ✦"),
                        false);
                mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        } catch (Exception e) {
            com.ovrtechnology.AromaAffect.LOGGER.error("Failed to fire all-scents victory", e);
        }
    }

    public static void resetClientCounter() {
        clientDetectedScents.clear();
    }

    public static void deactivateClient() {
        clientCounterActive = false;
        clientDetectedScents.clear();
        allScentsVictoryFired = false;
        allScentsVictoryScheduledAt = 0;
    }
}
