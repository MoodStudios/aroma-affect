package com.ovrtechnology.entity.sniffer;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.sniffer.config.SnifferConfigLoader;
import com.ovrtechnology.network.SnifferEquipmentNetworking;
import com.ovrtechnology.sniffernose.SnifferNoseItem;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
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

    @SuppressWarnings("this-escape")
    public SnifferContainer(Sniffer sniffer) {
        super(CONTAINER_SIZE);
        this.sniffer = sniffer;
        this.data = SnifferTamingData.get(sniffer.getUUID());

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

        if (initializing) {
            return;
        }

        data.saddleItem = super.getItem(SADDLE_SLOT).copy();
        data.decorationItem = super.getItem(DECORATION_SLOT).copy();

        if (hasSnifferNose()) {
            truncateSniffCooldown();
        }

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

    private void truncateSniffCooldown() {
        long noseCooldown = SnifferConfigLoader.getConfig().digging.sniffCooldownWithNose;
        if (sniffer.getBrain().hasMemoryValue(MemoryModuleType.SNIFF_COOLDOWN)
                && sniffer.getBrain().getTimeUntilExpiry(MemoryModuleType.SNIFF_COOLDOWN)
                        > noseCooldown) {
            sniffer.getBrain().eraseMemory(MemoryModuleType.SNIFF_COOLDOWN);
            sniffer.getBrain()
                    .setMemoryWithExpiry(
                            MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, noseCooldown);
        }
        sniffer.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
    }

    public Optional<String> getSnifferNoseId() {
        ItemStack nose = super.getItem(DECORATION_SLOT);
        if (nose.isEmpty() || !(nose.getItem() instanceof SnifferNoseItem snifferNose)) {
            return Optional.empty();
        }
        return Optional.of(AromaAffect.MOD_ID + ":" + snifferNose.getItemId());
    }
}
