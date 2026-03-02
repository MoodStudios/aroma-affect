package com.ovrtechnology.mixin;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.TutorialModule;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to block Nether/End portal teleportation when tutorial mode is active.
 * <p>
 * This prevents players from leaving the tutorial world via vanilla portals.
 * <p>
 * In Minecraft 1.21+, dimension changes use Entity.teleport(TeleportTransition).
 */
@Mixin(Entity.class)
public abstract class TutorialPortalBlockerMixin {

    /**
     * Intercepts dimension changes and blocks them if tutorial mode is active.
     * Only affects ServerPlayer entities.
     */
    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$blockPortalInTutorial(TeleportTransition transition, CallbackInfoReturnable<Entity> cir) {
        // Only affect ServerPlayer entities
        if (!((Object) this instanceof ServerPlayer self)) {
            return;
        }

        if (!(self.level() instanceof ServerLevel currentLevel)) {
            return;
        }

        // Only block if tutorial mode is active in the current level
        if (!TutorialModule.isActive(currentLevel)) {
            return;
        }

        // Get target dimension
        ServerLevel targetLevel = transition.newLevel();
        if (targetLevel == null) {
            return;
        }

        ResourceKey<Level> fromDimension = currentLevel.dimension();
        ResourceKey<Level> toDimension = targetLevel.dimension();

        // Check if this is a Nether or End portal transition
        boolean isNetherTransition = (fromDimension == Level.OVERWORLD && toDimension == Level.NETHER)
                || (fromDimension == Level.NETHER && toDimension == Level.OVERWORLD);
        boolean isEndTransition = (fromDimension == Level.OVERWORLD && toDimension == Level.END)
                || (fromDimension == Level.END && toDimension == Level.OVERWORLD);

        if (isNetherTransition || isEndTransition) {
            // Block the dimension change
            AromaAffect.LOGGER.info("[Tutorial] Blocked portal teleport for player {} ({} -> {})",
                    self.getName().getString(), fromDimension.location(), toDimension.location());

            // Send warning message
            self.displayClientMessage(
                    Component.literal("\u00a7c\u00a7l[Tutorial] \u00a7cPortals are disabled in tutorial mode"),
                    true
            );

            // Cancel the teleport by returning null
            cir.setReturnValue(null);
        }
    }
}
