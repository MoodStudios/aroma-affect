package com.ovrtechnology.legacy.balm.callback;

import com.ovrtechnology.legacy.balm.LegacyEvent;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.UseBlockEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public interface BlockCallback {

    @FunctionalInterface
    interface Use {
        InteractionEventResult handle(Player player, Level level, InteractionHand hand, BlockHitResult hitResult);

        LegacyEvent<Use> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(UseBlockEvent.class, e -> {
            InteractionEventResult result = h.handle(e.getPlayer(), e.getLevel(), e.getHand(), e.getHitResult());
            result.interactionResult().ifPresent(interaction -> {
                e.setResult(interaction);
                // Fabric reads the result directly and turns a cancel into FAIL; NeoForge only applies it when canceled
                if (!"fabric".equals(Balm.getPlatform())) {
                    e.setCanceled(true);
                }
            });
        }));
    }
}
