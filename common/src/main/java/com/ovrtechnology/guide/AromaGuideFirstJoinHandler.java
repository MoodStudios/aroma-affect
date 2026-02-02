package com.ovrtechnology.guide;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.common.PlayerEvent;
import lombok.experimental.UtilityClass;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

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
        PlayerEvent.PLAYER_JOIN.register(AromaGuideFirstJoinHandler::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        ServerLevel overworld = player.level().getServer().overworld();
        GuideRecipientData data = overworld.getDataStorage().computeIfAbsent(GuideRecipientData.TYPE);

        if (data.addPlayer(player.getUUID())) {
            ItemStack guide = new ItemStack(AromaGuideRegistry.getAROMA_GUIDE().get());
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

        static final SavedDataType<GuideRecipientData> TYPE = new SavedDataType<>(
                AromaAffect.MOD_ID + "_guide_recipients",
                GuideRecipientData::new,
                CODEC,
                null
        );

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
