package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;

public final class GuideManager {

    private GuideManager() {}

    public static void openGuideClient() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open guide: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening AromaCraft guide");
        minecraft.execute(
                () -> {
                    GuideBook book = AromaAffectGuideContent.getBook();
                    minecraft.setScreen(new GuideScreen(book));
                });
    }
}
