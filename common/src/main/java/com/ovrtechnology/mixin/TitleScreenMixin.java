package com.ovrtechnology.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.ovrtechnology.tutorial.demo.DemoWorldManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private static final String VERSION_TEXT = "OVR Tutorial ~ V2.3.1";

    @Unique
    private boolean aromaaffect$buttonsReplaced = false;

    @Unique
    private Button aromaaffect$playButton = null;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void aromaaffect$replaceButtons(CallbackInfo ci) {
        if (aromaaffect$buttonsReplaced) return;
        aromaaffect$buttonsReplaced = true;

        // If no template exists, leave vanilla buttons (for dev/server use)
        if (!DemoWorldManager.hasTemplate()) {
            return;
        }

        // Remove all existing buttons
        this.clearWidgets();

        int centerX = this.width / 2;
        int buttonW = 200;
        int buttonY = this.height / 2 + 20;

        // "Play" button — Shift+Click = edit mode
        aromaaffect$playButton = Button.builder(
                Component.literal("Play"),
                btn -> {
                    boolean editMode = InputConstants.isKeyDown(
                            Minecraft.getInstance().getWindow(),
                            GLFW.GLFW_KEY_LEFT_SHIFT);
                    DemoWorldManager.startSession(editMode);
                }
        ).bounds(centerX - buttonW / 2, buttonY, buttonW, 20).build();
        this.addRenderableWidget(aromaaffect$playButton);

        // "Quit Game" button below
        this.addRenderableWidget(Button.builder(
                Component.literal("Quit Game"),
                btn -> Minecraft.getInstance().stop()
        ).bounds(centerX - buttonW / 2, buttonY + 26, buttonW, 20).build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void aromaaffect$renderVersion(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // Version text (bottom-left) — always show
        graphics.drawString(Minecraft.getInstance().font, VERSION_TEXT, 2, this.height - 30, 0xAAFFFFFF, false);

        // Update Play button text based on Shift state
        if (aromaaffect$playButton != null && DemoWorldManager.hasTemplate()) {
            boolean shiftDown = InputConstants.isKeyDown(
                    Minecraft.getInstance().getWindow(),
                    GLFW.GLFW_KEY_LEFT_SHIFT);
            if (shiftDown) {
                aromaaffect$playButton.setMessage(Component.literal("§e✎ Edit Template"));
            } else {
                aromaaffect$playButton.setMessage(Component.literal("Play"));
            }
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void aromaaffect$resetFlag(CallbackInfo ci) {
        aromaaffect$buttonsReplaced = false;
        aromaaffect$playButton = null;
        // Clean up previous demo session when returning to title screen
        DemoWorldManager.runPendingCleanup();
        // Auto-start new game if requested
        DemoWorldManager.runPendingNewGame();
    }
}
