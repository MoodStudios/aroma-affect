package com.ovrtechnology.guide;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;
import lombok.experimental.UtilityClass;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=26 {
import net.minecraft.world.level.saveddata.SavedDataType;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
*///?}

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gives the Aroma Guide to players when they first join a world.
 */
@UtilityClass
public final class AromaGuideFirstJoinHandler {

    public static void init() {
        ServerPlayerCallback.Join.EVENT.register(AromaGuideFirstJoinHandler::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        ServerLevel overworld = player.level().getServer().overworld();
        //? if >=26 {
        GuideRecipientData data = overworld.getDataStorage().computeIfAbsent(GuideRecipientData.TYPE);
        //?} else {
        /*GuideRecipientData data = overworld.getDataStorage().computeIfAbsent(GuideRecipientData.FACTORY, GuideRecipientData.NAME);
        *///?}

        if (data.addPlayer(player.getUUID())) {
            ItemStack guide = new ItemStack(AromaGuideRegistry.getAromaGuide().value());
            player.getInventory().add(guide);
            AromaAffect.LOGGER.info("Gave Aroma Guide to first-time player: {}", player.getName().getString());
        }
    }

    static class GuideRecipientData extends SavedData {
        private final Set<UUID> recipients;

        private static final Codec<GuideRecipientData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        UUIDUtil.CODEC.listOf().fieldOf("recipients").forGetter(d -> new ArrayList<>(d.recipients))
                ).apply(instance, GuideRecipientData::new)
        );

        //? if >=26 {
        static final SavedDataType<GuideRecipientData> TYPE = new SavedDataType<>(
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "guide_recipients"),
                GuideRecipientData::new,
                CODEC,
                null
        );
        //?} else {
        /*static final String NAME = AromaAffect.MOD_ID + "_guide_recipients";

        static final SavedData.Factory<GuideRecipientData> FACTORY = new SavedData.Factory<>(
                GuideRecipientData::new,
                (tag, registries) -> CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(GuideRecipientData::new),
                null
        );

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CODEC.encodeStart(NbtOps.INSTANCE, this).result()
                    .filter(CompoundTag.class::isInstance)
                    .ifPresent(encoded -> tag.merge((CompoundTag) encoded));
            return tag;
        }
        *///?}

        GuideRecipientData() {
            this.recipients = new HashSet<>();
        }

        GuideRecipientData(List<UUID> recipients) {
            this.recipients = new HashSet<>(recipients);
        }

        boolean addPlayer(UUID uuid) {
            if (recipients.add(uuid)) {
                setDirty();
                return true;
            }
            return false;
        }
    }
}
