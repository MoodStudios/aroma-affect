package com.ovrtechnology.mixin;

import com.ovrtechnology.tutorial.demo.DemoWorldManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    @Unique
    private boolean aromaaffect$replaced = false;

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void aromaaffect$replaceButtons(CallbackInfo ci) {
        if (aromaaffect$replaced) return;
        aromaaffect$replaced = true;

        if (!DemoWorldManager.hasTemplate()) return;

        this.clearWidgets();

        int centerX = this.width / 2;
        int buttonW = 204;
        int y = this.height / 4 + 8;

        // Back to Game
        this.addRenderableWidget(Button.builder(
                Component.translatable("menu.returnToGame"),
                btn -> {
                    Minecraft.getInstance().setScreen(null);
                    Minecraft.getInstance().mouseHandler.grabMouse();
                }
        ).bounds(centerX - buttonW / 2, y, buttonW, 20).build());

        y += 28;

        if (DemoWorldManager.isEditSession()) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Quit to Title"),
                    btn -> {
                        btn.active = false;
                        Minecraft.getInstance().disconnect(new TitleScreen(), false);
                    }
            ).bounds(centerX - buttonW / 2, y, buttonW, 20).build());
        }
        // Play mode: only Back to Game (F6 is the secret reset)
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void aromaaffect$resetFlag(CallbackInfo ci) {
        aromaaffect$replaced = false;
    }
}
