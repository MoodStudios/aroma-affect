package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Networking for SearchDiamond minigame screens and hologram.
 */
public final class SearchDiamondNetworking {

    private static final ResourceLocation START_SCREEN_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "search_diamond_start");
    private static final ResourceLocation SUCCESS_SCREEN_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "search_diamond_success");
    private static final ResourceLocation HOLOGRAM_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "search_diamond_hologram");
    private static final ResourceLocation HOLOGRAM_CLEAR_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "search_diamond_hologram_clear");
    private static final ResourceLocation PLAYER_READY_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "search_diamond_ready");

    private static boolean initialized = false;

    private SearchDiamondNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Open start screen
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, START_SCREEN_PACKET,
                (buf, context) -> context.queue(SearchDiamondNetworking::openStartScreenOnClient));

        // S2C: Open success screen
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SUCCESS_SCREEN_PACKET,
                (buf, context) -> context.queue(SearchDiamondNetworking::openSuccessScreenOnClient));

        // S2C: Set hologram position
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HOLOGRAM_PACKET,
                (buf, context) -> {
                    double x = buf.readDouble();
                    double y = buf.readDouble();
                    double z = buf.readDouble();
                    context.queue(() -> setHologramOnClient(x, y, z));
                });

        // S2C: Clear hologram
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HOLOGRAM_CLEAR_PACKET,
                (buf, context) -> context.queue(SearchDiamondNetworking::clearHologramOnClient));

        // C2S: Player clicked "Let's Go!" - give items
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PLAYER_READY_PACKET,
                (buf, context) -> {
                    context.queue(() -> {
                        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                            onPlayerReady(serverPlayer);
                        }
                    });
                });

        AromaAffect.LOGGER.info("SearchDiamond networking initialized");
    }

    /**
     * Server sends start screen to player.
     */
    public static void sendStartScreen(ServerPlayer player) {
        AromaAffect.LOGGER.info("Sending SearchDiamond start screen to player {}", player.getName().getString());
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        NetworkManager.sendToPlayer(player, START_SCREEN_PACKET, buf);
    }

    /**
     * Server sends success title to player.
     */
    public static void sendSuccessScreen(ServerPlayer player) {
        // Send title animation (fade in, stay, fade out in ticks)
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));

        // Send title text (green "Diamond Found!")
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("\u00a7a\u00a7lDiamond Found!")));

        // Send subtitle text
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("\u00a7fGreat job using your nose!")));
    }

    private static void openStartScreenOnClient() {
        AromaAffect.LOGGER.info("Client received SearchDiamond start screen packet");
        try {
            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.searchdiamond.client.SearchDiamondClient");
            clientClass.getMethod("openStartScreen").invoke(null);
            AromaAffect.LOGGER.info("SearchDiamond start screen opened successfully via reflection");
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.error("Failed to open SearchDiamond start screen: {}", e.getMessage(), e);
        }
    }

    private static void openSuccessScreenOnClient() {
        try {
            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.searchdiamond.client.SearchDiamondClient");
            clientClass.getMethod("openSuccessScreen").invoke(null);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.error("Failed to open SearchDiamond success screen: {}", e.getMessage(), e);
        }
    }

    /**
     * Server sends hologram position to player.
     */
    public static void sendHologramPosition(ServerPlayer player, BlockPos pos) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeDouble(pos.getX() + 0.5);
        buf.writeDouble(pos.getY());
        buf.writeDouble(pos.getZ() + 0.5);
        NetworkManager.sendToPlayer(player, HOLOGRAM_PACKET, buf);
    }

    /**
     * Server clears hologram for player.
     */
    public static void sendClearHologram(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        NetworkManager.sendToPlayer(player, HOLOGRAM_CLEAR_PACKET, buf);
    }

    /**
     * Broadcast hologram position to all players.
     */
    public static void broadcastHologramPosition(net.minecraft.server.MinecraftServer server, BlockPos pos) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendHologramPosition(player, pos);
        }
    }

    /**
     * Broadcast clear hologram to all players.
     */
    public static void broadcastClearHologram(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendClearHologram(player);
        }
    }

    private static void setHologramOnClient(double x, double y, double z) {
        try {
            Class<?> hologramClass = Class.forName(
                    "com.ovrtechnology.tutorial.searchdiamond.client.DiamondTextHologram");
            Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
            Object vec3 = vec3Class.getConstructor(double.class, double.class, double.class)
                    .newInstance(x, y, z);
            hologramClass.getMethod("setTarget", vec3Class).invoke(null, vec3);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.error("Failed to set hologram position: {}", e.getMessage(), e);
        }
    }

    private static void clearHologramOnClient() {
        try {
            Class<?> hologramClass = Class.forName(
                    "com.ovrtechnology.tutorial.searchdiamond.client.DiamondTextHologram");
            hologramClass.getMethod("clear").invoke(null);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.error("Failed to clear hologram: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets the packet ID for player ready.
     * Called by client code to send the packet.
     */
    public static ResourceLocation getPlayerReadyPacketId() {
        return PLAYER_READY_PACKET;
    }

    /**
     * Server handles player ready - clears inventory and gives mining tools.
     */
    private static void onPlayerReady(ServerPlayer player) {
        AromaAffect.LOGGER.info("[SearchDiamond] Player {} clicked Let's Go! - clearing inventory and giving tools", player.getName().getString());

        // Clear player's inventory first
        player.getInventory().clearContent();
        AromaAffect.LOGGER.info("[SearchDiamond] Cleared player inventory");

        // Give Iron Pickaxe
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        boolean addedPickaxe = player.getInventory().add(pickaxe);
        AromaAffect.LOGGER.info("[SearchDiamond] Added Iron Pickaxe: {}", addedPickaxe);

        // Give Iron Shovel
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        boolean addedShovel = player.getInventory().add(shovel);
        AromaAffect.LOGGER.info("[SearchDiamond] Added Iron Shovel: {}", addedShovel);

        // Give Netherite Nose (custom item)
        ResourceLocation noseId = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "netherite_nose");
        BuiltInRegistries.ITEM.getOptional(noseId).ifPresent(noseItem -> {
            ItemStack nose = new ItemStack(noseItem);
            boolean addedNose = player.getInventory().add(nose);
            AromaAffect.LOGGER.info("[SearchDiamond] Added Netherite Nose: {}", addedNose);
        });

        // Force inventory sync to client
        player.inventoryMenu.broadcastChanges();
        AromaAffect.LOGGER.info("[SearchDiamond] Finished giving tools to player and synced inventory");
    }

    /**
     * Removes the diamond search tools from player inventory.
     * Called when player finds the diamond.
     */
    public static void removeSearchTools(ServerPlayer player) {
        var inventory = player.getInventory();

        // Remove Iron Pickaxe
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.IRON_PICKAXE)) {
                inventory.removeItem(i, stack.getCount());
                break;
            }
        }

        // Remove Iron Shovel
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.IRON_SHOVEL)) {
                inventory.removeItem(i, stack.getCount());
                break;
            }
        }

        // Remove Netherite Nose
        ResourceLocation noseId = ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "netherite_nose");
        BuiltInRegistries.ITEM.getOptional(noseId).ifPresent(noseItem -> {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(noseItem)) {
                    inventory.removeItem(i, stack.getCount());
                    break;
                }
            }
        });

        AromaAffect.LOGGER.info("Removed diamond search tools from player {}", player.getName().getString());
    }
}
