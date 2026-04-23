package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderContext;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class NoseItemClientExtensions implements IClientItemExtensions {
    private static volatile NoseMaskModel hiddenModel;
    private static volatile NoseMaskModel visibleNoStrapModel;
    private static volatile NoseMaskModel visibleWithStrapModel;

    private static void ensureModels() {
        if (hiddenModel != null) return;

        var entityModels = Minecraft.getInstance().getEntityModels();

        hiddenModel = new NoseMaskModel(entityModels.bakeLayer(NoseModelLayers.NOSE_MASK));
        hiddenModel.setAllVisible(false);
        hiddenModel.head.visible = true;
        hiddenModel.hat.visible = true;
        hiddenModel.setNoseMaskVisible(false);

        visibleNoStrapModel = new NoseMaskModel(entityModels.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleNoStrapModel.setAllVisible(false);
        visibleNoStrapModel.head.visible = true;
        visibleNoStrapModel.hat.visible = true;
        visibleNoStrapModel.setNoseMaskVisible(true);
        visibleNoStrapModel.setStrapVisible(false);

        visibleWithStrapModel =
                new NoseMaskModel(entityModels.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleWithStrapModel.setAllVisible(false);
        visibleWithStrapModel.head.visible = true;
        visibleWithStrapModel.hat.visible = true;
        visibleWithStrapModel.setNoseMaskVisible(true);
        visibleWithStrapModel.setStrapVisible(true);
    }

    private static NoseRenderPreferencesManager.NosePrefs resolvePrefs() {
        Minecraft mc = Minecraft.getInstance();
        UUID entityUuid = NoseRenderContext.getCurrentEntityUuid();
        UUID localUuid = mc.player != null ? mc.player.getUUID() : null;

        if (entityUuid != null && entityUuid.equals(localUuid)) {
            boolean noseEnabled = NoseRenderToggles.isNoseEnabled();
            boolean strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
            return new NoseRenderPreferencesManager.NosePrefs(noseEnabled, strapEnabled);
        }

        if (entityUuid != null) {
            NoseRenderPreferencesManager.NosePrefs remotePrefs =
                    NoseRenderPreferencesManager.getClientPrefsIfPresent(entityUuid);
            if (remotePrefs != null) {
                return remotePrefs;
            }
        }

        return new NoseRenderPreferencesManager.NosePrefs(true, false);
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(
            LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        if (slot != EquipmentSlot.HEAD) {
            return original;
        }

        ensureModels();

        NoseRenderPreferencesManager.NosePrefs prefs = resolvePrefs();

        if (!prefs.noseEnabled()) {
            return hiddenModel;
        }
        return prefs.strapEnabled() ? visibleWithStrapModel : visibleNoStrapModel;
    }
}
