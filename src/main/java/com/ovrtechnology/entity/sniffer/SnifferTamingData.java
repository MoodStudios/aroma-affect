package com.ovrtechnology.entity.sniffer;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SnifferTamingData {

    private static final Map<UUID, SnifferTamingData> DATA_MAP = new ConcurrentHashMap<>();

    public int tamingProgress = 0;
    public UUID ownerUUID = null;
    public ItemStack saddleItem = ItemStack.EMPTY;
    public ItemStack decorationItem = ItemStack.EMPTY;

    // Tracking de esencias de dimensión obtenidas
    public boolean hasOverworldScent = false;
    public boolean hasNetherScent = false;
    public boolean hasEndScent = false;

    public static SnifferTamingData get(UUID snifferUUID) {
        return DATA_MAP.computeIfAbsent(snifferUUID, k -> new SnifferTamingData());
    }

    /**
     * Verifica si el Sniffer ha completado las 3 esencias
     */
    public boolean hasAllScents() {
        return hasOverworldScent && hasNetherScent && hasEndScent;
    }
}
