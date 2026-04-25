package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.event.EventDefinition;
import com.ovrtechnology.trigger.event.EventDefinitionLoader;
import com.ovrtechnology.trigger.event.EventTriggersConfig;
import com.ovrtechnology.util.Ids;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ScentEventNetworking {

    public record ScentEventS2C(String eventId, double intensityOverride)
            implements CustomPacketPayload {
        public static final Type<ScentEventS2C> TYPE = new Type<>(Ids.mod("scent_event"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScentEventS2C> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeUtf(payload.eventId);
                            buf.writeDouble(payload.intensityOverride);
                        },
                        buf -> new ScentEventS2C(buf.readUtf(), buf.readDouble()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static final double NO_INTENSITY_OVERRIDE = -1.0;

    private static boolean initialized = false;

    private ScentEventNetworking() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                ScentEventS2C.TYPE,
                ScentEventS2C.STREAM_CODEC,
                (payload, context) ->
                        context.queue(() -> handleEventOnClient(payload.eventId(), payload.intensityOverride())));

        AromaAffect.LOGGER.info("ScentEventNetworking initialized");
    }

    private static void handleEventOnClient(String eventId, double intensityOverride) {
        EventDefinition def = EventDefinitionLoader.getById(eventId).orElse(null);
        if (def == null) {
            AromaAffect.LOGGER.warn(
                    "Received scent event for unknown event_id '{}', ignoring", eventId);
            return;
        }

        EventTriggersConfig config = EventTriggersConfig.getInstance();
        if (!config.isCategoryEnabled(def.getCategory())) {
            AromaAffect.LOGGER.debug(
                    "Event '{}' suppressed: category {} disabled", eventId, def.getCategory());
            return;
        }

        String scentName = ScentRegistry.getDisplayName(def.getScentId());
        if (scentName == null || "Unknown Scent".equals(scentName)) {
            AromaAffect.LOGGER.warn(
                    "Event '{}' references unresolvable scent '{}'", eventId, def.getScentId());
            return;
        }

        double intensity =
                intensityOverride > 0 && intensityOverride <= 1.0
                        ? intensityOverride
                        : def.getIntensity();

        ScentTrigger trigger =
                ScentTrigger.create(
                        scentName,
                        def.resolveSource(),
                        def.getPriority(),
                        def.getDurationTicks(),
                        intensity);

        boolean fired = ScentTriggerManager.getInstance().trigger(trigger);
        AromaAffect.LOGGER.debug(
                "Scent event '{}' from server -> {} (fired: {})", eventId, scentName, fired);
    }

    public static void sendEvent(ServerPlayer player, String eventId) {
        sendEvent(player, eventId, NO_INTENSITY_OVERRIDE);
    }

    public static void sendEvent(ServerPlayer player, String eventId, double intensityOverride) {
        if (player == null) {
            return;
        }
        if (!NetworkManager.canPlayerReceive(player, ScentEventS2C.TYPE)) {
            return;
        }
        NetworkManager.sendToPlayer(player, new ScentEventS2C(eventId, intensityOverride));
        AromaAffect.LOGGER.debug(
                "Sent scent event '{}' to {} (intensity override: {})",
                eventId,
                player.getName().getString(),
                intensityOverride);
    }
}
