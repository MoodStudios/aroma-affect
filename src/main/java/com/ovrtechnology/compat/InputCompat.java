package com.ovrtechnology.compat;

import com.mojang.blaze3d.platform.InputConstants;
//? if <26.3 {
/*import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
*///?}
import org.jetbrains.annotations.Nullable;

public final class InputCompat {

    public static final InputConstants.Type KEYBOARD =
            //? if >=26.3 {
            InputConstants.Type.KEYBOARD;
            //?} else {
            /*InputConstants.Type.KEYSYM;
            *///?}

    private InputCompat() {}

    public static boolean isKeyDown(int key) {
        //? if >=26.3 {
        return InputConstants.isKeyDown(key);
        //?} else {
        /*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
        *///?}
    }

    @Nullable
    public static String keyName(int keyCode, int scanCode) {
        //? if >=26.3 {
        String name = KEYBOARD.getOrCreate(keyCode).getDisplayName().getString();
        return name.isBlank() ? null : name;
        //?} else {
        /*return GLFW.glfwGetKeyName(keyCode, scanCode);
        *///?}
    }
}
