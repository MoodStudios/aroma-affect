package com.ovrtechnology.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class TrackingDirectionIndicator {

    private static final int ICON_SIZE = 5;
    private static final int COLUMN_WIDTH = ICON_SIZE + 2;

    private static final double XZ_NEAR = 10.0;
    private static final double ON_TARGET_XZ = 2.0;
    private static final double Y_SIGNIFICANT = 3.0;
    private static final double ON_TARGET_Y = 2.0;

    public enum Kind {
        N,
        NE,
        E,
        SE,
        S,
        SW,
        W,
        NW,
        UP,
        DOWN,
        ON_TARGET
    }

    private TrackingDirectionIndicator() {}

    public static int getColumnWidth() {
        return COLUMN_WIDTH;
    }

    public static Kind resolve(Minecraft mc, BlockPos destination) {
        if (mc.player == null || destination == null) {
            return Kind.N;
        }

        double dx = destination.getX() + 0.5 - mc.player.getX();
        double dz = destination.getZ() + 0.5 - mc.player.getZ();
        double dy = destination.getY() + 0.5 - mc.player.getY();
        double xzDist = Math.sqrt(dx * dx + dz * dz);

        if (xzDist <= ON_TARGET_XZ && Math.abs(dy) <= ON_TARGET_Y) {
            return Kind.ON_TARGET;
        }
        if (xzDist <= XZ_NEAR && Math.abs(dy) >= Y_SIGNIFICANT) {
            return dy > 0 ? Kind.UP : Kind.DOWN;
        }

        if (dx * dx + dz * dz < 0.0001) {
            return Kind.N;
        }

        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        float delta = Mth.wrapDegrees((float) targetYaw - mc.player.getYRot());
        int sector = Math.floorMod(Math.round(delta / 45.0f), 8);
        return switch (sector) {
            case 0 -> Kind.N;
            case 1 -> Kind.NE;
            case 2 -> Kind.E;
            case 3 -> Kind.SE;
            case 4 -> Kind.S;
            case 5 -> Kind.SW;
            case 6 -> Kind.W;
            case 7 -> Kind.NW;
            default -> Kind.N;
        };
    }

    public static int colorForKind(Kind kind, int defaultColor) {
        return switch (kind) {
            case UP, DOWN -> 0xFFD08BFF;
            case ON_TARGET -> 0xFF66FF99;
            default -> defaultColor;
        };
    }

    public static void draw(GuiGraphics graphics, int x, int y, Kind kind, int color) {
        int ox = x;
        int oy = y + 1;
        switch (kind) {
            case N -> drawN(graphics, ox, oy, color);
            case NE -> drawNE(graphics, ox, oy, color);
            case E -> drawE(graphics, ox, oy, color);
            case SE -> drawSE(graphics, ox, oy, color);
            case S -> drawS(graphics, ox, oy, color);
            case SW -> drawSW(graphics, ox, oy, color);
            case W -> drawW(graphics, ox, oy, color);
            case NW -> drawNW(graphics, ox, oy, color);
            case UP -> drawUp(graphics, ox, oy, color);
            case DOWN -> drawDown(graphics, ox, oy, color);
            case ON_TARGET -> drawOnTarget(graphics, ox, oy, color);
        }
    }

    private static void drawN(GuiGraphics g, int x, int y, int c) {
        fillPx(g, x + 2, y, c);
        fillPx(g, x + 1, y + 1, c);
        fillPx(g, x + 2, y + 1, c);
        fillPx(g, x + 3, y + 1, c);
        fillPx(g, x + 2, y + 2, c);
        fillPx(g, x + 2, y + 3, c);
        fillPx(g, x + 2, y + 4, c);
    }

    private static void drawS(GuiGraphics g, int x, int y, int c) {
        fillPx(g, x + 2, y + 4, c);
        fillPx(g, x + 1, y + 3, c);
        fillPx(g, x + 2, y + 3, c);
        fillPx(g, x + 3, y + 3, c);
        fillPx(g, x + 2, y + 2, c);
        fillPx(g, x + 2, y + 1, c);
        fillPx(g, x + 2, y, c);
    }

    private static void drawE(GuiGraphics g, int x, int y, int c) {
        fillPx(g, x + 4, y + 2, c);
        fillPx(g, x + 3, y + 1, c);
        fillPx(g, x + 3, y + 2, c);
        fillPx(g, x + 3, y + 3, c);
        fillPx(g, x + 2, y + 2, c);
        fillPx(g, x + 1, y + 2, c);
        fillPx(g, x, y + 2, c);
    }

    private static void drawW(GuiGraphics g, int x, int y, int c) {
        fillPx(g, x, y + 2, c);
        fillPx(g, x + 1, y + 1, c);
        fillPx(g, x + 1, y + 2, c);
        fillPx(g, x + 1, y + 3, c);
        fillPx(g, x + 2, y + 2, c);
        fillPx(g, x + 3, y + 2, c);
        fillPx(g, x + 4, y + 2, c);
    }

    private static void drawNE(GuiGraphics g, int x, int y, int c) {
        fillPx(g, x + 4, y, c);
        fillPx(g, x + 3, y, c);
        fillPx(g, x + 4, y + 1, c);
        fillPx(g, x + 3, y + 1, c);
        fillPx(g, x + 2, y + 2, c);
        fillPx(g, x + 1, y + 3, c);
        fillPx(g, x, y + 4, c);
    }

    private static void drawNW(GuiGraphics g, int x, int y, int c) {
        fillPx(g, x, y, c);
        fillPx(g, x + 1, y, c);
        fillPx(g, x, y + 1, c);
        fillPx(g, x + 1, y + 1, c);
        fillPx(g, x + 2, y + 2, c);
        fillPx(g, x + 3, y + 3, c);
        fillPx(g, x + 4, y + 4, c);
    }

    private static void drawSE(GuiGraphics g, int x, int y, int c) {
        fillPx(g, x + 4, y + 4, c);
        fillPx(g, x + 3, y + 4, c);
        fillPx(g, x + 4, y + 3, c);
        fillPx(g, x + 3, y + 3, c);
        fillPx(g, x + 2, y + 2, c);
        fillPx(g, x + 1, y + 1, c);
        fillPx(g, x, y, c);
    }

    private static void drawSW(GuiGraphics g, int x, int y, int c) {
        fillPx(g, x, y + 4, c);
        fillPx(g, x + 1, y + 4, c);
        fillPx(g, x, y + 3, c);
        fillPx(g, x + 1, y + 3, c);
        fillPx(g, x + 2, y + 2, c);
        fillPx(g, x + 3, y + 1, c);
        fillPx(g, x + 4, y, c);
    }

    private static void drawUp(GuiGraphics g, int x, int y, int c) {
        drawN(g, x, y, c);
        fillPx(g, x, y + 4, c);
        fillPx(g, x + 4, y + 4, c);
    }

    private static void drawDown(GuiGraphics g, int x, int y, int c) {
        drawS(g, x, y, c);
        fillPx(g, x, y, c);
        fillPx(g, x + 4, y, c);
    }

    private static void drawOnTarget(GuiGraphics g, int x, int y, int c) {
        int left = x;
        int top = y;
        int right = x + ICON_SIZE - 1;
        int bottom = y + ICON_SIZE - 1;
        for (int yy = top; yy <= bottom; yy++) {
            for (int xx = left; xx <= right; xx++) {
                boolean border = xx == left || xx == right || yy == top || yy == bottom;
                boolean center = xx == x + 2 && yy == y + 2;
                if (border || center) {
                    fillPx(g, xx, yy, c);
                }
            }
        }
    }

    private static void fillPx(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 1, y + 1, color);
    }
}
