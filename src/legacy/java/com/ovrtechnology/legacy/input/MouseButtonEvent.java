package com.ovrtechnology.legacy.input;

public record MouseButtonEvent(double x, double y, MouseButtonInfo buttonInfo) implements InputWithModifiers {

    public MouseButtonEvent(double x, double y, int button) {
        this(x, y, new MouseButtonInfo(button, 0));
    }

    @Override
    public int input() {
        return button();
    }

    public int button() {
        return buttonInfo.button();
    }

    @Override
    public int modifiers() {
        return buttonInfo.modifiers();
    }
}
