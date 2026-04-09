package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.scentzone.TutorialScentZone;
import com.ovrtechnology.tutorial.scentzone.TutorialScentZoneManager;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Networking for scent zone system.
 * - S2C: Sync all zones to client (for rendering + GUI)
 * - S2C: Trigger scent on client
 * - C2S: Create, update, delete zones
 */
public final class TutorialScentZoneNetworking {

    private static final ResourceLocation ZONE_SYNC_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_scentzone_sync");
    private static final ResourceLocation ZONE_TRIGGER_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_scentzone_trigger");
    private static final ResourceLocation ZONE_CREATE_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_scentzone_create");
    private static final ResourceLocation ZONE_UPDATE_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_scentzone_update");
    private static final ResourceLocation ZONE_DELETE_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_scentzone_delete");
    private static final ResourceLocation ZONE_OPEN_GUI_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_scentzone_gui");

    private static boolean initialized = false;

    /** Client-side cached zones for rendering and GUI. */
    private static final List<ZoneClientData> clientZones = new ArrayList<>();
    private static boolean showZoneOverlays = false;

    private TutorialScentZoneNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Sync all zones to client
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ZONE_SYNC_PACKET,
                (buf, context) -> {
                    int count = buf.readVarInt();
                    List<ZoneClientData> zones = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        zones.add(ZoneClientData.read(buf));
                    }
                    context.queue(() -> {
                        clientZones.clear();
                        clientZones.addAll(zones);
                    });
                });

        // S2C: Trigger scent on client
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ZONE_TRIGGER_PACKET,
                (buf, context) -> {
                    String scentName = buf.readUtf(256);
                    double intensity = buf.readDouble();
                    String zoneId = buf.readUtf(256);
                    context.queue(() -> triggerScentOnClient(scentName, intensity, zoneId));
                });

        // C2S: Create zone
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ZONE_CREATE_PACKET,
                (buf, context) -> {
                    ZoneClientData data = ZoneClientData.read(buf);
                    context.queue(() -> {
                        Player player = context.getPlayer();
                        if (player instanceof ServerPlayer sp && sp.level() instanceof ServerLevel level) {
                            TutorialScentZoneManager.createZone(level, data.id,
                                    new BlockPos(data.x, data.y, data.z),
                                    data.radiusX, data.radiusY, data.radiusZ, data.scentName);
                            // Apply full data
                            TutorialScentZoneManager.getZone(level, data.id).ifPresent(zone -> {
                                zone.setIntensity(data.intensity);
                                zone.setCooldownSeconds(data.cooldownSeconds);
                                zone.setOneShot(data.oneShot);
                                zone.setEnabled(data.enabled);
                                TutorialScentZoneManager.updateZone(level, zone);
                            });
                            syncToAllPlayers(level);
                        }
                    });
                });

        // C2S: Update zone
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ZONE_UPDATE_PACKET,
                (buf, context) -> {
                    ZoneClientData data = ZoneClientData.read(buf);
                    context.queue(() -> {
                        Player player = context.getPlayer();
                        if (player instanceof ServerPlayer sp && sp.level() instanceof ServerLevel level) {
                            TutorialScentZoneManager.getZone(level, data.id).ifPresent(zone -> {
                                zone.setPosition(new BlockPos(data.x, data.y, data.z));
                                zone.setRadiusX(data.radiusX);
                                zone.setRadiusY(data.radiusY);
                                zone.setRadiusZ(data.radiusZ);
                                zone.setScentName(data.scentName);
                                zone.setIntensity(data.intensity);
                                zone.setCooldownSeconds(data.cooldownSeconds);
                                zone.setOneShot(data.oneShot);
                                zone.setEnabled(data.enabled);
                                TutorialScentZoneManager.updateZone(level, zone);
                            });
                            syncToAllPlayers(level);
                        }
                    });
                });

        // C2S: Delete zone
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ZONE_DELETE_PACKET,
                (buf, context) -> {
                    String id = buf.readUtf(256);
                    context.queue(() -> {
                        Player player = context.getPlayer();
                        if (player instanceof ServerPlayer sp && sp.level() instanceof ServerLevel level) {
                            TutorialScentZoneManager.deleteZone(level, id);
                            syncToAllPlayers(level);
                        }
                    });
                });

        // S2C: Open scent zone GUI
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ZONE_OPEN_GUI_PACKET,
                (buf, context) -> context.queue(() -> openGuiOnClient()));

        AromaAffect.LOGGER.debug("Tutorial scent zone networking initialized");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // S2C: Sync
    // ─────────────────────────────────────────────────────────────────────────────

    public static void syncToPlayer(ServerPlayer player, ServerLevel level) {
        Collection<TutorialScentZone> zones = TutorialScentZoneManager.getAllZones(level);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeVarInt(zones.size());
        for (TutorialScentZone z : zones) {
            new ZoneClientData(z.getId(), z.getPosition().getX(), z.getPosition().getY(), z.getPosition().getZ(),
                    z.getRadiusX(), z.getRadiusY(), z.getRadiusZ(),
                    z.getScentName(), z.getIntensity(),
                    z.getCooldownSeconds(), z.isOneShot(), z.isEnabled()).write(buf);
        }
        NetworkManager.sendToPlayer(player, ZONE_SYNC_PACKET, buf);
    }

    public static void syncToAllPlayers(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            syncToPlayer(player, level);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // S2C: Trigger scent
    // ─────────────────────────────────────────────────────────────────────────────

    public static void sendScentTrigger(ServerPlayer player, String scentName, double intensity, String zoneId) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeUtf(scentName, 256);
        buf.writeDouble(intensity);
        buf.writeUtf(zoneId, 256);
        NetworkManager.sendToPlayer(player, ZONE_TRIGGER_PACKET, buf);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // C2S: CRUD from GUI
    // ─────────────────────────────────────────────────────────────────────────────

    public static void sendCreateZone(net.minecraft.core.RegistryAccess registryAccess, ZoneClientData data) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        data.write(buf);
        NetworkManager.sendToServer(ZONE_CREATE_PACKET, buf);
    }

    public static void sendUpdateZone(net.minecraft.core.RegistryAccess registryAccess, ZoneClientData data) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        data.write(buf);
        NetworkManager.sendToServer(ZONE_UPDATE_PACKET, buf);
    }

    public static void sendDeleteZone(net.minecraft.core.RegistryAccess registryAccess, String id) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        buf.writeUtf(id, 256);
        NetworkManager.sendToServer(ZONE_DELETE_PACKET, buf);
    }

    /**
     * Server tells client to open the scent zone editor GUI.
     */
    public static void sendOpenGui(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        NetworkManager.sendToPlayer(player, ZONE_OPEN_GUI_PACKET, buf);
    }

    private static void openGuiOnClient() {
        try {
            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.scentzone.client.TutorialScentZoneScreen");
            clientClass.getMethod("open").invoke(null);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to open scent zone GUI", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Client-side accessors
    // ─────────────────────────────────────────────────────────────────────────────

    public static List<ZoneClientData> getClientZones() {
        return clientZones;
    }

    public static boolean isShowZoneOverlays() {
        return showZoneOverlays;
    }

    public static void setShowZoneOverlays(boolean show) {
        showZoneOverlays = show;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Client-side scent trigger
    // ─────────────────────────────────────────────────────────────────────────────

    private static void triggerScentOnClient(String scentName, double intensity, String zoneId) {
        try {
            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.scentzone.client.TutorialScentZoneClient");
            clientClass.getMethod("onZoneTrigger", String.class, double.class, String.class)
                    .invoke(null, scentName, intensity, zoneId);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.debug("Failed to trigger scent zone on client", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Data transfer record
    // ─────────────────────────────────────────────────────────────────────────────

    public record ZoneClientData(String id, int x, int y, int z,
                                  int radiusX, int radiusY, int radiusZ,
                                  String scentName, double intensity,
                                  int cooldownSeconds, boolean oneShot, boolean enabled) {

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(id, 256);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeInt(radiusX);
            buf.writeInt(radiusY);
            buf.writeInt(radiusZ);
            buf.writeUtf(scentName, 256);
            buf.writeDouble(intensity);
            buf.writeInt(cooldownSeconds);
            buf.writeBoolean(oneShot);
            buf.writeBoolean(enabled);
        }

        public static ZoneClientData read(RegistryFriendlyByteBuf buf) {
            return new ZoneClientData(
                    buf.readUtf(256),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readUtf(256),
                    buf.readDouble(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
        }
    }
}
