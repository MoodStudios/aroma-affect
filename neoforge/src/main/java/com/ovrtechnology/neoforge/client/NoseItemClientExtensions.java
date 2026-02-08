package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderContext;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * NeoForge-specific armor model provider for the Nose item.
 *
 * <p>Uses three pre-configured immutable model instances to avoid shared
 * mutable state corruption in deferred renders.</p>
 *
 * <p>Entity identification relies on {@link NoseRenderContext}, which is set
 * by {@code LivingEntityRendererMixin} at the HEAD of
 * {@code LivingEntityRenderer.submit()}, BEFORE equipment layers run.</p>
 */
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

        visibleWithStrapModel = new NoseMaskModel(entityModels.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleWithStrapModel.setAllVisible(false);
        visibleWithStrapModel.head.visible = true;
        visibleWithStrapModel.hat.visible = true;
        visibleWithStrapModel.setNoseMaskVisible(true);
        visibleWithStrapModel.setStrapVisible(true);
    }

    /**
     * Resolves nose render preferences for the entity currently being rendered.
     * The UUID comes from {@link NoseRenderContext}, set by our mixin at the
     * HEAD of {@code LivingEntityRenderer.submit()}.
     */
    private static NoseRenderPreferencesManager.NosePrefs resolvePrefs() {
        Minecraft mc = Minecraft.getInstance();
        UUID entityUuid = NoseRenderContext.getCurrentEntityUuid();
        UUID localUuid = mc.player != null ? mc.player.getUUID() : null;

        // Local player — use direct toggle state
        if (entityUuid != null && entityUuid.equals(localUuid)) {
            boolean noseEnabled = NoseRenderToggles.isNoseEnabled();
            boolean strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
            return new NoseRenderPreferencesManager.NosePrefs(noseEnabled, strapEnabled);
        }

        // Remote player — use server-synced preferences cache
        if (entityUuid != null) {
            NoseRenderPreferencesManager.NosePrefs remotePrefs =
                    NoseRenderPreferencesManager.getClientPrefsIfPresent(entityUuid);
            if (remotePrefs != null) {
                return remotePrefs;
            }
        }

        // Fallback — default to visible (only nose-equipped entities reach here)
        return new NoseRenderPreferencesManager.NosePrefs(true, false);
    }

    @Override
    public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        if (layerType != EquipmentClientInfo.LayerType.HUMANOID) {
            return original;
        }

        ensureModels();

        NoseRenderPreferencesManager.NosePrefs prefs = resolvePrefs();

        if (!prefs.noseEnabled()) {
            return hiddenModel;
        }
        return prefs.strapEnabled() ? visibleWithStrapModel : visibleNoStrapModel;
    }

    @Override
    public ResourceLocation getArmorTexture(
            ItemStack stack,
            EquipmentClientInfo.LayerType layerType,
            EquipmentClientInfo.Layer layer,
            ResourceLocation defaultTexture
    ) {
        return NoseClient.getArmorTexture(stack);
    }

    @Override
    public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int defaultColor) {
        return layerIdx == 0 ? 0xFFFFFFFF : 0;
    }
}
