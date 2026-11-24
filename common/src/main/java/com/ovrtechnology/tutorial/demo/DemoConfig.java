package com.ovrtechnology.tutorial.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.ovrtechnology.AromaAffect;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Timings of an unattended demo station, read from {@code config/aromaaffect_demo.json}.
 * The file is written with the defaults the first time the game runs, so it can be edited on site.
 * It is read again every time a new game starts, so an edit applies from the next visitor on.
 */
public final class DemoConfig {

    private static final String CONFIG_FILE_NAME = "aromaaffect_demo.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static DemoConfig instance;

    /**
     * Longest a play session may last, in minutes. 0 means no limit: a session then ends when the
     * visitor picks a new game, walks away or chooses End Demo, never in the middle of playing.
     */
    private int sessionMinutes = 0;

    /** Seconds without mouse or keyboard input before the station starts a new game. 0 turns it off. */
    private int idleSeconds = 90;

    /** How long before that restart the "still there?" notice is shown. */
    private int idleWarningSeconds = 10;

    /** How long the end screen stays up before the next game starts by itself. */
    private int finishScreenSeconds = 30;

    /** Frame rate of {@code aromaaffect_idle_video.mjpeg}, the video behind the start screen. */
    private int idleVideoFps = 30;

    /**
     * Whether the start screen offers the guided demo next to the free walkaround. Off by default:
     * the station shows a single Start button that drops the visitor into the free area.
     */
    private boolean showPlayDemo = false;

    /**
     * Where the start screen puts the video: "column" (on the left, next to the logo), "background"
     * (the whole screen, logo and button on top) or "auto" (column for a vertical video, background
     * for a horizontal one).
     */
    private String idleVideoLayout = "auto";

    /** Whether the Aroma Affect logo is drawn over a background video. Off leaves only the Start button. */
    private boolean idleVideoShowLogo = true;

    /**
     * Stretches of the video, in seconds from its start, during which the logo over a background video
     * fades out so the video's own title shows, for example "0-1.2, 17.8-21". Empty keeps the logo up.
     */
    private String idleVideoHideLogo = "";

    private DemoConfig() {
    }

    public static synchronized DemoConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /** Reads the file again. */
    public static synchronized void reload() {
        instance = load();
    }

    // The limits keep a typo in the file from overflowing the tick counters or freezing the station

    /** 0 when sessions have no time limit. */
    public int sessionSeconds() {
        return sessionMinutes <= 0 ? 0 : clamp(sessionMinutes, 1, 180) * 60;
    }

    /** 0 when the idle restart is turned off. */
    public int idleSeconds() {
        return clamp(idleSeconds, 0, 3600);
    }

    public int idleWarningSeconds() {
        return clamp(idleWarningSeconds, 0, idleSeconds());
    }

    public int finishScreenSeconds() {
        return clamp(finishScreenSeconds, 5, 600);
    }

    public int idleVideoFps() {
        return clamp(idleVideoFps, 1, 60);
    }

    public boolean showPlayDemo() {
        return showPlayDemo;
    }

    /** "column", "background" or "auto". */
    public String idleVideoLayout() {
        String value = idleVideoLayout == null ? "" : idleVideoLayout.trim().toLowerCase(java.util.Locale.ROOT);
        return value.equals("column") || value.equals("background") ? value : "auto";
    }

    public boolean idleVideoShowLogo() {
        return idleVideoShowLogo;
    }

    /** The stretches of {@link #idleVideoHideLogo} as {start, end} pairs in seconds; bad entries are skipped. */
    public java.util.List<float[]> idleVideoHideLogoRanges() {
        java.util.List<float[]> ranges = new java.util.ArrayList<>();
        if (idleVideoHideLogo == null) return ranges;
        for (String part : idleVideoHideLogo.split(",")) {
            String[] ends = part.trim().split("-");
            if (ends.length != 2) continue;
            try {
                float start = Float.parseFloat(ends[0].trim());
                float end = Float.parseFloat(ends[1].trim());
                if (end > start) ranges.add(new float[]{start, end});
            } catch (NumberFormatException ignored) {
                // a typo in one stretch leaves the others working
            }
        }
        return ranges;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static DemoConfig load() {
        Path path = Platform.getConfigFolder().resolve(CONFIG_FILE_NAME);

        if (Files.exists(path)) {
            try {
                DemoConfig config = GSON.fromJson(Files.readString(path), DemoConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException | JsonSyntaxException e) {
                AromaAffect.LOGGER.warn("Could not read {}, using the default demo timings: {}", path, e.getMessage());
                return new DemoConfig();
            }
        }

        DemoConfig config = new DemoConfig();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config));
        } catch (IOException e) {
            AromaAffect.LOGGER.warn("Could not write {}: {}", path, e.getMessage());
        }
        return config;
    }
}
