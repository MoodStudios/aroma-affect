package com.ovrtechnology.entity.sniffer;

import com.ovrtechnology.network.SnifferEquipmentNetworking;
import com.ovrtechnology.sniffernose.SnifferNoseItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SnifferContainer extends SimpleContainer {
    private final Sniffer sniffer;
    private final SnifferTamingData data;
    private boolean initializing = true;

    public static final int SADDLE_SLOT = 0;
    public static final int DECORATION_SLOT = 1;
    public static final int CONTAINER_SIZE = 2;

    public SnifferContainer(Sniffer sniffer) {
        super(CONTAINER_SIZE);
        this.sniffer = sniffer;
        this.data = SnifferTamingData.get(sniffer.getUUID());

        // Cargar items guardados en el data (sin triggear setChanged)
        if (!data.saddleItem.isEmpty()) {
            super.setItem(SADDLE_SLOT, data.saddleItem.copy());
        }
        if (!data.decorationItem.isEmpty()) {
            super.setItem(DECORATION_SLOT, data.decorationItem.copy());
        }
        this.initializing = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();

        // No sincronizar durante la inicialización para evitar sobrescribir datos
        if (initializing) {
            return;
        }

        // Sincronizar con SnifferTamingData
        data.saddleItem = super.getItem(SADDLE_SLOT).copy();
        data.decorationItem = super.getItem(DECORATION_SLOT).copy();

        // Broadcast equipment change to all tracking players
        if (sniffer.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(sniffer) <= 128 * 128) {
                    SnifferEquipmentNetworking.sendEquipmentSync(player, sniffer.getUUID(), data);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !sniffer.isDeadOrDying() && player.distanceToSqr(sniffer) <= 64.0;
    }

    public boolean hasSaddle() {
        ItemStack saddle = super.getItem(SADDLE_SLOT);
        return !saddle.isEmpty() && saddle.is(Items.SADDLE);
    }

    public boolean hasSnifferNose() {
        ItemStack nose = super.getItem(DECORATION_SLOT);
        return !nose.isEmpty() && nose.getItem() instanceof SnifferNoseItem;
    }
}
