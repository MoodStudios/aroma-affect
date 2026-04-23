package com.ovrtechnology.entity.nosesmith.client.dialogue;

import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import net.minecraft.client.Minecraft;

public final class NoseSmithDialogueClient {
    private NoseSmithDialogueClient() {}

    public static void open(NoseSmithEntity noseSmith) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new NoseSmithDialogueScreen(noseSmith));
    }
}
