package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.client.ScentPuffOverlay;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import dev.architectury.networking.NetworkManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

public final class OmaraDeviceNetworking {

    public record OmaraPuffS2C(String scentName, double intensity, int durationTicks)
            implements CustomPacketPayload {
        public static final Type<OmaraPuffS2C> TYPE = new Type<>(Ids.mod("omara_puff_s2c"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OmaraPuffS2C> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeUtf(payload.scentName);
                            buf.writeDouble(payload.intensity);
                            buf.writeVarInt(payload.durationTicks);
                        },
                        buf -> new OmaraPuffS2C(buf.readUtf(), buf.readDouble(), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final int OMARA_SCENT_DURATION_TICKS = 100;

    private static final double OMARA_SCENT_INTENSITY = 1.0;

    private static boolean initialized = false;

    private OmaraDeviceNetworking() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                OmaraPuffS2C.TYPE,
                OmaraPuffS2C.STREAM_CODEC,
                (payload, context) -> {
                    String scentName = payload.scentName();
                    double intensity = payload.intensity();
                    int durationTicks = payload.durationTicks();

                    context.queue(
                            () -> {
                                ScentTrigger trigger =
                                        ScentTrigger.fromOmaraDevice(
                                                scentName, durationTicks, intensity);
                                boolean triggered =
                                        ScentTriggerManager.getInstance().trigger(trigger);

                                if (triggered) {

                                    ScentPuffOverlay.onScentPuff(scentName, intensity);

                                    if (ClientConfig.getInstance().isDebugScentMessages()) {
                                        int intensityPercent = (int) Math.round(intensity * 100);
                                        String message =
                                                String.format(
                                                        "§d[Aroma Affect] §7Scent: §e%s §7(§domara device§7) §8[%d%%]",
                                                        scentName, intensityPercent);
                                        net.minecraft.client.Minecraft mc =
                                                net.minecraft.client.Minecraft.getInstance();
                                        if (mc.player != null) {
                                            mc.player.displayClientMessage(
                                                    Texts.lit(message), false);
                                        }
                                    }
                                }

                                AromaAffect.LOGGER.debug(
                                        "Received Omara puff from server: {} (triggered: {})",
                                        scentName,
                                        triggered);
                            });
                });

        AromaAffect.LOGGER.info("OmaraDeviceNetworking initialized");
    }

    public static void broadcastPuff(ServerLevel level, BlockPos devicePos, String scentName) {

        AABB area = new AABB(devicePos).inflate(2.0);
        List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, area);

        for (ServerPlayer player : nearbyPlayers) {
            sendPuffToPlayer(player, scentName);
        }

        AromaAffect.LOGGER.debug(
                "Omara Device broadcast scent '{}' to {} players at {}",
                scentName,
                nearbyPlayers.size(),
                devicePos);
    }

    private static void sendPuffToPlayer(ServerPlayer player, String scentName) {
        if (!NetworkManager.canPlayerReceive(player, OmaraPuffS2C.TYPE)) {
            return;
        }

        NetworkManager.sendToPlayer(
                player,
                new OmaraPuffS2C(scentName, OMARA_SCENT_INTENSITY, OMARA_SCENT_DURATION_TICKS));
    }
}
