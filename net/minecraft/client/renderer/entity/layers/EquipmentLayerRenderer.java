package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EquipmentLayerRenderer {
	private static final int NO_LAYER_COLOR = 0;
	private final EquipmentAssetManager equipmentAssets;
	private final Function<EquipmentLayerRenderer.LayerTextureKey, ResourceLocation> layerTextureLookup;
	private final Function<EquipmentLayerRenderer.TrimSpriteKey, TextureAtlasSprite> trimSpriteLookup;

	public EquipmentLayerRenderer(EquipmentAssetManager equipmentAssetManager, TextureAtlas textureAtlas) {
		this.equipmentAssets = equipmentAssetManager;
		this.layerTextureLookup = Util.memoize(
			(Function<EquipmentLayerRenderer.LayerTextureKey, ResourceLocation>)(layerTextureKey -> layerTextureKey.layer.getTextureLocation(layerTextureKey.layerType))
		);
		this.trimSpriteLookup = Util.memoize(
			(Function<EquipmentLayerRenderer.TrimSpriteKey, TextureAtlasSprite>)(trimSpriteKey -> textureAtlas.getSprite(trimSpriteKey.spriteId()))
		);
	}

	public <S> void renderLayers(
		EquipmentClientInfo.LayerType layerType,
		ResourceKey<EquipmentAsset> resourceKey,
		Model<? super S> model,
		S object,
		ItemStack itemStack,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int i,
		int j
	) {
		this.renderLayers(layerType, resourceKey, model, object, itemStack, poseStack, submitNodeCollector, i, null, j, 1);
	}

	public <S> void renderLayers(
		EquipmentClientInfo.LayerType layerType,
		ResourceKey<EquipmentAsset> resourceKey,
		Model<? super S> model,
		S object,
		ItemStack itemStack,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int i,
		@Nullable ResourceLocation resourceLocation,
		int j,
		int k
	) {
		List<EquipmentClientInfo.Layer> list = this.equipmentAssets.get(resourceKey).getLayers(layerType);
		if (!list.isEmpty()) {
			int l = DyedItemColor.getOrDefault(itemStack, 0);
			boolean bl = itemStack.hasFoil();
			int m = k;

			for (EquipmentClientInfo.Layer layer : list) {
				int n = getColorForLayer(layer, l);
				if (n != 0) {
					ResourceLocation resourceLocation2 = layer.usePlayerTexture() && resourceLocation != null
						? resourceLocation
						: (ResourceLocation)this.layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, layer));
					submitNodeCollector.order(m++)
						.submitModel(model, object, poseStack, RenderType.armorCutoutNoCull(resourceLocation2), i, OverlayTexture.NO_OVERLAY, n, null, j, null);
					if (bl) {
						submitNodeCollector.order(m++).submitModel(model, object, poseStack, RenderType.armorEntityGlint(), i, OverlayTexture.NO_OVERLAY, n, null, j, null);
					}

					bl = false;
				}
			}

			ArmorTrim armorTrim = itemStack.get(DataComponents.TRIM);
			if (armorTrim != null) {
				TextureAtlasSprite textureAtlasSprite = (TextureAtlasSprite)this.trimSpriteLookup
					.apply(new EquipmentLayerRenderer.TrimSpriteKey(armorTrim, layerType, resourceKey));
				RenderType renderType = Sheets.armorTrimsSheet(armorTrim.pattern().value().decal());
				submitNodeCollector.order(m++).submitModel(model, object, poseStack, renderType, i, OverlayTexture.NO_OVERLAY, -1, textureAtlasSprite, j, null);
			}
		}
	}

	private static int getColorForLayer(EquipmentClientInfo.Layer layer, int i) {
		Optional<EquipmentClientInfo.Dyeable> optional = layer.dyeable();
		if (optional.isPresent()) {
			int j = (Integer)((EquipmentClientInfo.Dyeable)optional.get()).colorWhenUndyed().map(ARGB::opaque).orElse(0);
			return i != 0 ? i : j;
		} else {
			return -1;
		}
	}

	@Environment(EnvType.CLIENT)
	record LayerTextureKey(EquipmentClientInfo.LayerType layerType, EquipmentClientInfo.Layer layer) {
	}

	@Environment(EnvType.CLIENT)
	record TrimSpriteKey(ArmorTrim trim, EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAssetId) {
		public ResourceLocation spriteId() {
			return this.trim.layerAssetId(this.layerType.trimAssetPrefix(), this.equipmentAssetId);
		}
	}
}
