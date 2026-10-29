package com.ovrtechnology.legacy.input;

import net.minecraft.util.StringUtil;

public record CharacterEvent(int codepoint, int modifiers) {

    public CharacterEvent(int codepoint) {
        this(codepoint, 0);
    }

    public String codepointAsString() {
        return Character.toString(codepoint);
    }

    public boolean isAllowedChatCharacter() {
        return codepoint <= Character.MAX_VALUE && StringUtil.isAllowedChatCharacter((char) codepoint);
    }
}
