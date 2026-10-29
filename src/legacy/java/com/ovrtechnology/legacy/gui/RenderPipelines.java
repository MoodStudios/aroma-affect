package com.ovrtechnology.legacy.gui;

public final class RenderPipelines {

    public static final Pipeline GUI = new Pipeline("gui");
    public static final Pipeline GUI_TEXTURED = new Pipeline("gui_textured");

    private RenderPipelines() {}

    public record Pipeline(String name) {}
}
