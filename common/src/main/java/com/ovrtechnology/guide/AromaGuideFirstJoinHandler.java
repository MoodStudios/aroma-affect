package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

@UtilityClass
public final class AromaGuideFirstJoinHandler {

    private static final String DATA_ID = AromaAffect.MOD_ID + "_guide_recipients";

    public static void init() {
        PlayerEvent.PLAYER_JOIN.register(AromaGuideFirstJoinHandler::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        ServerLevel overworld = player.level().getServer().overworld();
        GuideRecipientData data =
                overworld.getDataStorage().computeIfAbsent(GuideRecipientData.factory(), DATA_ID);

        if (data.addPlayer(player.getUUID())) {
            ItemStack guide = new ItemStack(AromaGuideRegistry.getAROMA_GUIDE().get());
            player.getInventory().add(guide);
            AromaAffect.LOGGER.info(
                    "Gave Aroma Guide to first-time player: {}", player.getName().getString());
        }
    }

    static class GuideRecipientData extends SavedData {
        private final Set<UUID> recipients;

        GuideRecipientData() {
            this.recipients = new HashSet<>();
        }

        GuideRecipientData(Set<UUID> recipients) {
            this.recipients = recipients;
        }

        public static GuideRecipientData load(CompoundTag tag, HolderLookup.Provider registries) {
            Set<UUID> uuids = new HashSet<>();
            if (tag.contains("recipients")) {
                ListTag list = tag.getList("recipients", 8);
                for (int i = 0; i < list.size(); i++) {
                    try {
                        uuids.add(UUID.fromString(list.getString(i)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return new GuideRecipientData(uuids);
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (UUID uuid : recipients) {
                list.add(StringTag.valueOf(uuid.toString()));
            }
            tag.put("recipients", list);
            return tag;
        }

        public static Factory<GuideRecipientData> factory() {
            return new Factory<>(GuideRecipientData::new, GuideRecipientData::load, null);
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
