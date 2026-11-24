package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialFinishNetworking;
import com.ovrtechnology.network.TutorialIntroNetworking;
import com.ovrtechnology.tutorial.finishscreen.client.TutorialFinishScreen;
import com.ovrtechnology.tutorial.intro.client.TutorialIntroScreen;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.phys.Vec3;

/**
 * Test rig for the unattended station, off unless the game is started with the environment
 * variable {@code AROMAAFFECT_DEMO_AUTOPILOT=<number of sessions>}.
 * <p>
 * It plays the part of the visitors: presses Play on the title screen once, presses PLAY DEMO
 * on every start screen, and ends each session a different way in turn (walking away, End Demo,
 * running out of time, a new game from the end-of-route screen). After the requested number of
 * sessions it closes the game. Every step
 * is logged with the {@code [autopilot]} prefix and a screenshot is saved, so a run can be
 * checked from the log and the screenshots folder.
 */
public final class DemoAutopilot {

    private static final String ENV = "AROMAAFFECT_DEMO_AUTOPILOT";
    private static final String TAG = "[autopilot] ";

    /** A step that takes longer than this is reported as a hang and ends the run. */
    private static final int HANG_TICKS = 20 * 120;

    private static final int CAMERA_CHECK_FROM_TICK = 110;

    private enum Stage { BOOT, TITLE, LOADING, INTRO, PLAYING, FINISH }

    private enum Ending { WALK_AWAY, END_DEMO, TIME_UP, ROUTE_DONE }

    private static int sessionsWanted = 0;
    private static int sessionsStarted = 0;
    private static Stage stage = Stage.BOOT;
    private static int stageTicks = 0;
    private static boolean endDemoSent = false;
    /** In a TIME_UP session, "Continue playing" was pressed once already. */
    private static boolean keptPlaying = false;
    private static Vec3 lastCameraPosition = null;
    private static int introFrames = 0;
    private static int introFramesMoved = 0;
    /** PLAY DEMO was pressed in the world that is loaded now. */
    private static boolean sessionRunning = false;

    private DemoAutopilot() {}

    public static void init() {
        String value = System.getenv(ENV);
        if (value == null || value.isBlank()) return;
        try {
            sessionsWanted = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            sessionsWanted = 3;
        }
        if (sessionsWanted <= 0) return;

        AromaAffect.LOGGER.info(TAG + "on, {} sessions", sessionsWanted);
        ClientTickEvent.CLIENT_POST.register(DemoAutopilot::tick);

        // How smooth the camera flight behind the start screen is: a camera that only moves
        // with the server ticks stands still on most frames. Measured on the first start screen,
        // after the opening shot (the flight holds still for its first 80 ticks).
        ClientGuiEvent.RENDER_POST.register((screen, graphics, mouseX, mouseY, delta) -> {
            if (!(screen instanceof TutorialIntroScreen)) return;
            if (sessionsStarted > 0 || stageTicks < CAMERA_CHECK_FROM_TICK) return;
            Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (lastCameraPosition != null) {
                introFrames++;
                if (!position.equals(lastCameraPosition)) {
                    introFramesMoved++;
                }
            }
            lastCameraPosition = position;
        });
    }

    private static Ending ending() {
        // AROMAAFFECT_DEMO_ENDING=TIME_UP (or another ending) makes every session end that way
        String forced = System.getenv("AROMAAFFECT_DEMO_ENDING");
        if (forced != null && !forced.isBlank()) {
            try {
                return Ending.valueOf(forced.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // unknown name: the usual rotation
            }
        }
        return Ending.values()[(sessionsStarted - 1) % Ending.values().length];
    }

    private static void tick(Minecraft mc) {
        // The test window usually sits in the background; a station's never does
        mc.options.pauseOnLostFocus = false;
        if (mc.player == null) {
            sessionRunning = false;
        }
        Stage now = currentStage(mc);
        if (now != stage) {
            AromaAffect.LOGGER.info(TAG + "{} -> {} after {} ticks (session {})", stage, now, stageTicks, sessionsStarted);
            if (now == Stage.TITLE && sessionsStarted > 0) {
                AromaAffect.LOGGER.warn(TAG + "a restart left the station on the title screen, waiting for the recovery");
            }
            stage = now;
            stageTicks = 0;
            endDemoSent = false;
        }
        stageTicks++;

        if (stageTicks == HANG_TICKS) {
            AromaAffect.LOGGER.error(TAG + "FAILED: stuck in {} for {} seconds (session {})", stage, HANG_TICKS / 20, sessionsStarted);
            shot(mc, "hang_" + stage);
        }
        if (stageTicks == HANG_TICKS + 40) {
            mc.stop();
            return;
        }

        switch (stage) {
            case TITLE -> {
                // Only the first time, as the staff would. A later title screen is a failed
                // restart, and getting out of it is the job of DemoWorldManager.tickRecovery
                if (stageTicks == 40 && sessionsStarted == 0 && DemoWorldManager.hasTemplate()) {
                    AromaAffect.LOGGER.info(TAG + "pressing Play on the title screen");
                    DemoWorldManager.startSession(false);
                }
            }
            case INTRO -> {
                if (stageTicks == 60) {
                    shot(mc, "intro");
                }
                String introShots = System.getenv("AROMAAFFECT_DEMO_INTROSHOTS");
                if (introShots != null && !introShots.isBlank() && sessionsStarted == 0) {
                    // Only looks at the start screen as the video plays: a shot at each listed tick, then quits
                    int last = 0;
                    for (String part : introShots.split(",")) {
                        int at = Integer.parseInt(part.trim());
                        last = Math.max(last, at);
                        if (stageTicks == at) shot(mc, "intro_t" + at);
                    }
                    if (stageTicks == last + 20) mc.stop();
                    return;
                }
                if (stageTicks == (sessionsStarted == 0 ? CAMERA_CHECK_FROM_TICK + 100 : 80)) {
                    if (introFrames > 0) {
                        AromaAffect.LOGGER.info(TAG + "start screen camera moved on {} of {} frames", introFramesMoved, introFrames);
                        introFrames = 0;
                        introFramesMoved = 0;
                    }
                    if (sessionsStarted >= sessionsWanted) {
                        AromaAffect.LOGGER.info(TAG + "DONE: {} sessions, every restart came back to the start screen", sessionsStarted);
                        mc.stop();
                        return;
                    }
                    sessionsStarted++;
                    keptPlaying = false;
                    AromaAffect.LOGGER.info(TAG + "session {} starts, will end by {}", sessionsStarted, ending());
                    sessionRunning = true;
                    if (DemoWalkaroundCheck.enabled() || System.getenv("AROMAAFFECT_DEMO_ENDING") != null) {
                        // The button a visitor sees: START, or WALKAROUND when the demo is shown too
                        pressFirstButton(mc, DemoConfig.get().showPlayDemo() ? 1 : 0);
                    } else {
                        TutorialIntroNetworking.sendPlayDemoToServer(mc.player.registryAccess());
                        mc.setScreen(null);
                    }
                }
            }
            case PLAYING -> {
                if (stageTicks == 60) {
                    shot(mc, "playing");
                }
                if (DemoWalkaroundCheck.enabled()) {
                    DemoIdleHandler.poke();
                    if (DemoWalkaroundCheck.tick(mc, stageTicks, name -> shot(mc, name)) && !endDemoSent) {
                        endDemoSent = true;
                        TutorialFinishNetworking.sendEndDemoToServer(mc.player.registryAccess());
                    }
                    return;
                }
                if (DemoFinalAreaCheck.enabled()) {
                    DemoIdleHandler.poke();
                    if (DemoFinalAreaCheck.tick(mc, stageTicks, name -> shot(mc, name)) && !endDemoSent) {
                        endDemoSent = true;
                        TutorialFinishNetworking.sendEndDemoToServer(mc.player.registryAccess());
                    }
                    return;
                }
                switch (ending()) {
                    case WALK_AWAY -> {
                        // A visitor who presses Escape and leaves: the server stops ticking with
                        // work still pending, and the idle handler has to notice and restart
                        if (stageTicks == 2 && mc.screen == null) {
                            AromaAffect.LOGGER.info(TAG + "pausing the game before walking away");
                            mc.pauseGame(false);
                        }
                        int noticeAt = (DemoConfig.get().idleSeconds() - DemoConfig.get().idleWarningSeconds() / 2) * 20;
                        if (stageTicks == noticeAt) {
                            shot(mc, "idle_notice");
                        }
                    }
                    case END_DEMO -> {
                        DemoIdleHandler.poke();
                        if (stageTicks == 100 && !endDemoSent) {
                            endDemoSent = true;
                            AromaAffect.LOGGER.info(TAG + "sending End Demo");
                            TutorialFinishNetworking.sendEndDemoToServer(mc.player.registryAccess());
                        }
                    }
                    case ROUTE_DONE -> {
                        DemoIdleHandler.poke();
                        // What the finish zone at the end of the route does
                        if (stageTicks == 100) {
                            AromaAffect.LOGGER.info(TAG + "opening the end-of-route screen");
                            TutorialFinishScreen.setTimeExpired(false);
                            mc.setScreen(new TutorialFinishScreen());
                        }
                    }
                    case TIME_UP -> {
                        DemoIdleHandler.poke();
                        // With no time limit nothing may end a session that is being played:
                        // play past the idle limit, then leave through End Demo
                        if (DemoConfig.get().sessionSeconds() == 0) {
                            int playTicks = (DemoConfig.get().idleSeconds() + 15) * 20;
                            if (stageTicks == playTicks && !endDemoSent) {
                                endDemoSent = true;
                                AromaAffect.LOGGER.info(TAG + "no time limit, still playing after {} seconds, sending End Demo", playTicks / 20);
                                TutorialFinishNetworking.sendEndDemoToServer(mc.player.registryAccess());
                            }
                        }
                    }
                }
            }
            case FINISH -> {
                if (stageTicks == 50) {
                    shot(mc, "finish");
                }
                // Time up: the first time keep playing (a new countdown), the second time leave the
                // screen alone so it starts the next game by itself
                if (ending() == Ending.TIME_UP && TutorialFinishScreen.isTimeExpired() && !keptPlaying
                        && stageTicks == 80) {
                    keptPlaying = true;
                    pressButton(mc, "Continue playing");
                }
                if (ending() == Ending.ROUTE_DONE && !TutorialFinishScreen.isTimeExpired()) {
                    DemoIdleHandler.poke();
                    if (stageTicks == 80) {
                        pressButton(mc, "Start a new game");
                    }
                }
            }
            default -> { }
        }
    }

    private static Stage currentStage(Minecraft mc) {
        if (mc.screen instanceof TitleScreen) return Stage.TITLE;
        if (mc.screen instanceof TutorialIntroScreen) return Stage.INTRO;
        if (mc.screen instanceof TutorialFinishScreen) return Stage.FINISH;
        if (sessionRunning && mc.level != null && DemoWorldManager.isStationSession()) {
            // Dialogues and the pause menu are still part of a running session
            return Stage.PLAYING;
        }
        return stage == Stage.BOOT ? Stage.BOOT : Stage.LOADING;
    }

    private static void pressButton(Minecraft mc, String label) {
        if (mc.screen != null) {
            for (GuiEventListener child : mc.screen.children()) {
                if (child instanceof Button button && button.active && button.getMessage().getString().equals(label)) {
                    AromaAffect.LOGGER.info(TAG + "pressing \"{}\"", label);
                    button.onPress(new MouseButtonInfo(0, 0));
                    return;
                }
            }
        }
        AromaAffect.LOGGER.error(TAG + "FAILED: no active \"{}\" button on the screen", label);
    }

    private static void pressFirstButton(Minecraft mc, int index) {
        int seen = 0;
        if (mc.screen != null) {
            for (GuiEventListener child : mc.screen.children()) {
                if (child instanceof Button button && button.active) {
                    if (seen++ == index) {
                        AromaAffect.LOGGER.info(TAG + "pressing \"{}\"", button.getMessage().getString());
                        button.onPress(new MouseButtonInfo(0, 0));
                        return;
                    }
                }
            }
        }
        AromaAffect.LOGGER.error(TAG + "FAILED: no active button {} on the start screen", index);
    }

    private static void shot(Minecraft mc, String name) {
        String file = String.format("autopilot_%03d_%s.png", sessionsStarted, name);
        try {
            Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), 1, message -> { });
        } catch (RuntimeException e) {
            AromaAffect.LOGGER.warn(TAG + "screenshot {} failed: {}", file, e.getMessage());
        }
    }
}
