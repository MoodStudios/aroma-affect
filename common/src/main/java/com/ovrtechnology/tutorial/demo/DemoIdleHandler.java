package com.ovrtechnology.tutorial.demo;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.cinematic.client.CinematicCameraLock;
import com.ovrtechnology.tutorial.finishscreen.client.TutorialFinishScreen;
import com.ovrtechnology.tutorial.intro.client.TutorialIntroScreen;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * Starts a new game when a visitor walks away mid-session: after
 * {@link DemoConfig#idleSeconds()} without mouse or keyboard input the station goes back
 * to the start screen. A notice counts down the last seconds so a player who is only
 * reading or looking around can cancel it by touching anything.
 * <p>
 * Nothing is counted on the start screen (that is the resting state), on the end screen
 * (it restarts by itself) or while a cinematic holds the camera (there is nothing to press).
 */
public final class DemoIdleHandler {

    private static final int COLOR_TEXT = 0xFFFFFFFF;

    /** Mouse travel below this many pixels is a mouse settling on the counter, not a hand. */
    private static final double MOUSE_DEADZONE = 3.0;

    /** How long a key or button that stays down keeps counting as somebody playing while the player stands still. */
    private static final int HELD_TICKS = 20 * 30;

    /**
     * A key or button down for longer than this without a break stops counting altogether, moving
     * or not. Nobody plays that way, and something resting on a key can keep the player moving
     * (jumping on the spot, circling in a current) for as long as it rests there.
     */
    private static final int HELD_MAX_TICKS = 20 * 180;

    /** Blocks per tick, measured on the ground plane, below which the player counts as standing still. */
    private static final double PLAYER_MOVE_EPSILON = 0.01;

    /** Every key GLFW knows by name; asking it about any other code logs an error. */
    private static final int[] KEYS = buildKeyList();

    private static boolean initialized = false;

    private static int idleTicks = 0;
    private static double mouseAnchorX = Double.NaN;
    private static double mouseAnchorY = Double.NaN;
    private static int mouseAnchorAge = 0;
    private static int mouseButtons = 0;
    private static int mouseButtonsHeldTicks = 0;
    private static int mouseButtonsDownTicks = 0;
    private static final boolean[] keyDown = new boolean[KEYS.length];
    private static final int[] keyHeldTicks = new int[KEYS.length];
    private static final int[] keyDownTicks = new int[KEYS.length];
    private static double lastPlayerX = Double.NaN;
    private static double lastPlayerZ = Double.NaN;

    private DemoIdleHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientTickEvent.CLIENT_POST.register(DemoIdleHandler::tick);

        // The notice has to show over the world and over any open screen (dialogue, trade, pause)
        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            if (Minecraft.getInstance().screen == null) {
                renderNotice(graphics);
            }
        });
        ClientGuiEvent.RENDER_POST.register((screen, graphics, mouseX, mouseY, delta) -> renderNotice(graphics));
    }

    /** Counts as input. For callers that know somebody is at the station without a key or the mouse saying so. */
    public static void poke() {
        idleTicks = 0;
    }

    private static void tick(Minecraft mc) {
        // Advancement and recipe toasts sat over the scent counter and the timer ("Isn't It Iron
        // Pick" right as the walkaround starts); a station shows none
        if (mc.player != null && DemoWorldManager.isStationSession()) {
            mc.getToastManager().clear();
        }

        // Read the devices every tick, counting or not, so the first tick that counts
        // compares against the present and not against a state from minutes ago
        boolean input = hadInput(mc);

        int limitSeconds = DemoConfig.get().idleSeconds();
        if (limitSeconds <= 0 || mc.player == null || !DemoWorldManager.isStationSession() || isResting(mc)) {
            idleTicks = 0;
            return;
        }

        if (input) {
            idleTicks = 0;
            return;
        }

        idleTicks++;
        if (idleTicks >= limitSeconds * 20) {
            idleTicks = 0;
            AromaAffect.LOGGER.info("No input for {} seconds, starting a new demo game", limitSeconds);
            DemoWorldManager.restartSession();
        }
    }

    /** States in which nobody is expected to touch the controls. */
    private static boolean isResting(Minecraft mc) {
        if (mc.screen instanceof TutorialIntroScreen) return true;
        if (mc.screen instanceof TutorialFinishScreen && TutorialFinishScreen.isTimeExpired()) return true;
        // A cinematic ends by itself, unless the game is paused: then it never does, and a
        // visitor who pressed Escape and left would hold the station until somebody came
        return CinematicCameraLock.isLocked() && !mc.isPaused();
    }

    /**
     * True when somebody is at the controls: the mouse travelled, or a key or mouse button was
     * pressed or released, or one is held while the player walks, or one has been held with the
     * player standing still for less than {@link #HELD_TICKS}. A hold longer than
     * {@link #HELD_MAX_TICKS} counts for nothing.
     * <p>
     * Walking and mining are done by holding a key or button down, so a held one has to count.
     * A bag resting on the keyboard holds one down too, so it cannot count for ever. Jumping is
     * not walking: the space bar held down bounces the player on the spot for as long as it is held.
     */
    private static boolean hadInput(Minecraft mc) {
        boolean input = false;

        // Travel is measured inside one-second windows, so a mouse creeping a pixel at a
        // time on a glossy counter never adds up to a movement
        MouseHandler mouse = mc.mouseHandler;
        double x = mouse.xpos();
        double y = mouse.ypos();
        if (Double.isNaN(mouseAnchorX)) {
            mouseAnchorX = x;
            mouseAnchorY = y;
            mouseAnchorAge = 0;
        } else if (Math.abs(x - mouseAnchorX) > MOUSE_DEADZONE || Math.abs(y - mouseAnchorY) > MOUSE_DEADZONE) {
            input = true;
            mouseAnchorX = x;
            mouseAnchorY = y;
            mouseAnchorAge = 0;
        } else if (++mouseAnchorAge >= 20) {
            mouseAnchorX = x;
            mouseAnchorY = y;
            mouseAnchorAge = 0;
        }

        // A held key that moves the player is somebody walking, for as long as they walk.
        // A bag on the keyboard walks the player into the first obstacle and then nothing moves.
        // Height is left out, or a held space bar would count as walking for ever.
        boolean moving = false;
        if (mc.player != null) {
            double px = mc.player.getX();
            double pz = mc.player.getZ();
            moving = !Double.isNaN(lastPlayerX)
                    && Math.abs(px - lastPlayerX) + Math.abs(pz - lastPlayerZ) > PLAYER_MOVE_EPSILON;
            lastPlayerX = px;
            lastPlayerZ = pz;
        } else {
            lastPlayerX = Double.NaN;
        }

        int buttons = (mouse.isLeftPressed() ? 1 : 0) | (mouse.isRightPressed() ? 2 : 0) | (mouse.isMiddlePressed() ? 4 : 0);
        if (buttons != mouseButtons) {
            mouseButtons = buttons;
            mouseButtonsHeldTicks = 0;
            mouseButtonsDownTicks = 0;
            input = true;
        } else if (buttons != 0 && ++mouseButtonsDownTicks <= HELD_MAX_TICKS) {
            if (moving) {
                mouseButtonsHeldTicks = 0;
                input = true;
            } else if (++mouseButtonsHeldTicks <= HELD_TICKS) {
                input = true;
            }
        }

        Window window = mc.getWindow();
        for (int i = 0; i < KEYS.length; i++) {
            boolean down = InputConstants.isKeyDown(window, KEYS[i]);
            if (down != keyDown[i]) {
                keyDown[i] = down;
                keyHeldTicks[i] = 0;
                keyDownTicks[i] = 0;
                input = true;
            } else if (down && ++keyDownTicks[i] <= HELD_MAX_TICKS) {
                if (moving) {
                    keyHeldTicks[i] = 0;
                    input = true;
                } else if (++keyHeldTicks[i] <= HELD_TICKS) {
                    input = true;
                }
            }
        }
        return input;
    }

    private static void renderNotice(GuiGraphics graphics) {
        if (idleTicks <= 0) return;

        DemoConfig config = DemoConfig.get();
        int ticksLeft = config.idleSeconds() * 20 - idleTicks;
        if (ticksLeft > config.idleWarningSeconds() * 20) return;

        Minecraft mc = Minecraft.getInstance();
        String title = "Still there?";
        String detail = "Move the mouse to keep playing";
        String count = "New game in " + Math.max(1, (ticksLeft + 19) / 20);

        int width = Math.max(mc.font.width(detail), mc.font.width(count)) + 24;
        int height = 46;
        int left = (graphics.guiWidth() - width) / 2;
        // Over a screen (the pause menu, a dialogue) it goes to the top edge, clear of the buttons
        int top = mc.screen == null ? graphics.guiHeight() / 4 : 6;
        int center = graphics.guiWidth() / 2;

        com.ovrtechnology.tutorial.ui.VanillaPanel.draw(graphics, left, top, left + width, top + height);
        graphics.drawCenteredString(mc.font, title, center, top + 6, com.ovrtechnology.tutorial.ui.VanillaPanel.TITLE_COLOR);
        graphics.drawCenteredString(mc.font, detail, center, top + 19, COLOR_TEXT);
        graphics.drawCenteredString(mc.font, count, center, top + 32, COLOR_TEXT);
    }

    private static int[] buildKeyList() {
        int[][] ranges = {
                {GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_SPACE},
                {GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_APOSTROPHE},
                {GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_9},
                {GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_SEMICOLON},
                {GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_EQUAL},
                {GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_RIGHT_BRACKET},
                {GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_GRAVE_ACCENT},
                {GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_END},
                {GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_PAUSE},
                {GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F25},
                {GLFW.GLFW_KEY_KP_0, GLFW.GLFW_KEY_KP_EQUAL},
                {GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_MENU},
        };
        int total = 0;
        for (int[] range : ranges) {
            total += range[1] - range[0] + 1;
        }
        int[] keys = new int[total];
        int i = 0;
        for (int[] range : ranges) {
            for (int key = range[0]; key <= range[1]; key++) {
                keys[i++] = key;
            }
        }
        return keys;
    }
}
