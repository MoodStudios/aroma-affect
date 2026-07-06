package com.ovrtechnology.client;

import com.ovrtechnology.websocket.OvrWebSocketClient;
import net.blay09.mods.balm.client.platform.event.callback.ScreenCallback;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public class ClientHooks {

    private static final String OMARA_STATE = "aromaaffect.state.";

    public static void init() {
        ScreenCallback.Render.AFTER.register((screen, guiGraphics, mouseX, mouseY, delta) -> {
            if (screen instanceof TitleScreen titleScreen) {
                Font font = titleScreen.getFont();
                String state = OMARA_STATE + OvrWebSocketClient.getInstance().getState().toString().toLowerCase();

                Component stateText = Component.translatable(state);
                Component bridgeText = Component.translatable("aromaaffect.bridge.title", stateText);
                guiGraphics.text(font, bridgeText, 4, 4, ARGB.color(1.0f, 0xFFFFFF));
            }
        });
    }
}
