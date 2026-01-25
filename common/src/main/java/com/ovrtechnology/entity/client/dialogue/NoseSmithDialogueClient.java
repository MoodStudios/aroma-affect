package com.ovrtechnology.entity.client.dialogue;

import com.ovrtechnology.entity.NoseSmithEntity;
import net.minecraft.client.Minecraft;

public final class NoseSmithDialogueClient {
    private NoseSmithDialogueClient() {
    }

    public static void open(NoseSmithEntity noseSmith) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new NoseSmithDialogueScreen(noseSmith));
    }
}

