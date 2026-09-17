package com.ovrtechnology.menu;

/** Fits a usable logical layout into GUI pixels without changing Minecraft's global GUI scale. */
public record MenuViewport(int width, int height, double scale, double offsetX, double offsetY) {
    public static MenuViewport fit(int availableWidth, int availableHeight, int minimumWidth, int minimumHeight) {
        int w = Math.max(1, availableWidth);
        int h = Math.max(1, availableHeight);
        double expansion = Math.max(1, Math.max((double) minimumWidth / w, (double) minimumHeight / h));
        int logicalWidth = Math.max(1, (int) Math.ceil(w * expansion));
        int logicalHeight = Math.max(1, (int) Math.ceil(h * expansion));
        double scale = Math.min((double) w / logicalWidth, (double) h / logicalHeight);
        return new MenuViewport(logicalWidth, logicalHeight, scale,
                (w - logicalWidth * scale) / 2, (h - logicalHeight * scale) / 2);
    }

    /** Keep each logical pixel on a whole framebuffer pixel when the layout fits. */
    public static MenuViewport fitPixels(int framebufferWidth, int framebufferHeight, int guiScale,
                                         int minimumWidth, int minimumHeight) {
        int w = Math.max(1, framebufferWidth);
        int h = Math.max(1, framebufferHeight);
        int requestedScale = Math.max(1, guiScale);
        int pixelScale = Math.min(requestedScale, Math.min(w / Math.max(1, minimumWidth),
                h / Math.max(1, minimumHeight)));
        if (pixelScale < 1) {
            // Extremely small windows still need a complete, clickable layout.
            var physical = fit(w, h, minimumWidth, minimumHeight);
            return new MenuViewport(physical.width(), physical.height(), physical.scale() / requestedScale,
                    physical.offsetX() / requestedScale, physical.offsetY() / requestedScale);
        }
        int logicalWidth = w / pixelScale;
        int logicalHeight = h / pixelScale;
        return new MenuViewport(logicalWidth, logicalHeight, (double) pixelScale / requestedScale,
                Math.floor((w - logicalWidth * pixelScale) / 2.0) / requestedScale,
                Math.floor((h - logicalHeight * pixelScale) / 2.0) / requestedScale);
    }

    public double localX(double x) { return (x - offsetX) / scale; }
    public double localY(double y) { return (y - offsetY) / scale; }
    public int screenX(double x) { return (int) Math.round(offsetX + x * scale); }
    public int screenY(double y) { return (int) Math.round(offsetY + y * scale); }
}
