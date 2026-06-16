package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.PassiveModeManager;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import com.ovrtechnology.trigger.client.ScentPuffOverlay;
import com.ovrtechnology.trigger.config.ClientConfig;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-to-client networking for event/action scent hooks.
 *
 * <p>Event hooks are detected server-side (mining, eating, rain, etc.) but the
 * OVR WebSocket client runs on the client, so the server tells the client to
 * play a scent. The client gates on passive mode being enabled before routing
 * the trigger through {@link ScentTriggerManager} (which applies its own
 * cooldown/priority).</p>
 *
 * <p>Modeled on {@link PathScentNetworking.PathScentTriggerS2C}.</p>
 */
public final class EventScentNetworking {

    public record EventScentTriggerS2C(String scentName, double intensity, int priorityOrdinal,
                                       int durationTicks, String sourceId) implements CustomPacketPayload {
        public static final Type<EventScentTriggerS2C> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "event_scent_trigger"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EventScentTriggerS2C> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.scentName);
                    buf.writeDouble(payload.intensity);
                    buf.writeVarInt(payload.priorityOrdinal);
                    buf.writeVarInt(payload.durationTicks);
                    buf.writeUtf(payload.sourceId);
                },
                buf -> new EventScentTriggerS2C(buf.readUtf(), buf.readDouble(), buf.readVarInt(),
                        buf.readVarInt(), buf.readUtf())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean initialized = false;

    private EventScentNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, EventScentTriggerS2C.TYPE,
                EventScentTriggerS2C.STREAM_CODEC, (payload, context) -> context.queue(() -> {
                    // Event hooks are part of passive mode — respect the toggle.
                    if (!PassiveModeManager.isPassiveModeEnabled()) {
                        return;
                    }

                    ScentPriority priority = ScentPriority.values()[payload.priorityOrdinal()];
                    ScentTrigger trigger = ScentTrigger.create(
                            payload.scentName(),
                            ScentTriggerSource.EVENT,
                            priority,
                            payload.durationTicks(),
                            payload.intensity());

                    boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);
                    if (triggered && ClientConfig.getInstance().isPassivePuffOverlay()) {
                        ScentPuffOverlay.onScentPuff(payload.scentName(), payload.intensity());
                    }
                    AromaAffect.LOGGER.debug("Received event scent '{}' ({}) from server: triggered={}",
                            payload.scentName(), payload.sourceId(), triggered);
                }));

        AromaAffect.LOGGER.info("EventScentNetworking initialized");
    }

    /**
     * Sends an event scent trigger to a specific client.
     */
    public static void sendEventScent(ServerPlayer player, String scentName, double intensity,
                                      ScentPriority priority, int durationTicks, String sourceId) {
        if (!NetworkManager.canPlayerReceive(player, EventScentTriggerS2C.TYPE)) {
            return;
        }
        NetworkManager.sendToPlayer(player, new EventScentTriggerS2C(
                scentName, intensity, priority.ordinal(), durationTicks, sourceId));
    }
}
