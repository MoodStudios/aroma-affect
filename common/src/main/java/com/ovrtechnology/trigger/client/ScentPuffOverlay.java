package com.ovrtechnology.trigger.client;

import com.ovrtechnology.AromaAffect;
import net.blay09.mods.balm.client.platform.event.callback.RenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fullscreen scent mask overlay for non-tracking puffs (Omara Device, etc.).
 *
 * <p>Shows the scent's border/corner image for ~2 seconds with fade-in/fade-out.
 * This is independent of the path tracking system and does not require an active
 * tracking session.</p>
 */
public final class ScentPuffOverlay {

    private static final Map<String, Identifier> SCENT_MASKS = new HashMap<>();

    /**
     * An animated mask: a horizontal sprite strip of {@link #frames} cells, each
     * {@value #SHEET_FRAME_W}x{@value #SHEET_FRAME_H}.
     *
     * @param texture the sheet
     * @param frames  how many cells the strip holds
     * @param loop    true to restart from cell 0, false to hold the last cell once
     *                the strip has played through (for art that ends on a settled
     *                pose and would jump if wrapped around)
     */
    private record AnimatedMask(Identifier texture, int frames, boolean loop) {}

    /**
     * Animated sheets keyed by what fired the puff. These override the scent's static
     * mask without changing which scent fires: a golden apple still triggers citrus and
     * redstone still triggers machina, they just get their own border.
     *
     * <p>Two kinds of key live here. Event ids ({@code aromaaffect:...}) come from the
     * data-driven event system; passive-mode sources ({@code biome:}, {@code mob:},
     * {@code structure:}, {@code block:} + the vanilla id) come from
     * {@link com.ovrtechnology.trigger.PassiveModeManager}. They cannot collide, so one
     * map serves both.</p>
     */
    private static final Map<String, AnimatedMask> SOURCE_MASKS = new HashMap<>();

    private static final long PULSE_DURATION_MS = 2000L;
    private static final long FADE_IN_MS = 90L;
    private static final long FADE_OUT_MS = 800L;
    private static final float MIN_VISIBLE_ALPHA = 0.28f;

    /** Every animated sheet is a horizontal strip of this cell size. */
    private static final int SHEET_FRAME_W = 160;
    private static final int SHEET_FRAME_H = 90;
    private static final long SHEET_FRAME_MS = 120L;

    private static boolean initialized = false;

    private static Identifier activeMask = null;
    private static AnimatedMask activeSheet = null;
    private static long pulseStartMs = 0L;
    private static double lastPuffIntensity = 0.5;

    static {
        register("winter", "winterlayermask");
        register("barnyard", "barnyardlayermask");
        register("sweet", "sweetlayermask");
        register("floral", "flowerlayermask");
        register("beach", "beachlayermask");
        register("kindred", "kindredlayermask");
        register("petrichor", "rainlayermask");
        register("marine", "marinelayermask");
        register("evergreen", "forestlayermask");
        register("terra silva", "terrasilvalayermask");
        register("citrus", "citruslayermask");
        register("desert", "desertlayermask");
        register("savory spice", "savoryspicelayermask");
        register("timber", "timberlayermask");
        register("smoky", "smokylayermask");
        register("machina", "diesellayermask");

        // ── Golden: eating a golden apple / enchanted golden apple / golden carrot ──
        AnimatedMask golden = sheet("golden", 5, true);
        registerSheet("aromaaffect:player_food_citrus", golden);

        // ── Redstone technology: mining it, building with it, switching it on ──
        AnimatedMask redstone = sheet("redstone_technology", 5, true);
        registerSheet("aromaaffect:block_break_redstone_ore", redstone);
        registerSheet("aromaaffect:block_break_redstone_component", redstone);
        registerSheet("aromaaffect:item_crafted_redstone", redstone);
        registerSheet("aromaaffect:redstone_activated", redstone);
        registerSheet("block:minecraft:redstone_ore", redstone);
        registerSheet("block:minecraft:deepslate_redstone_ore", redstone);

        // ── Boss encounter: smelling (or felling) the Ender Dragon or the Wither ──
        AnimatedMask boss = sheet("boss_encounter", 9, true);
        registerSheet("mob:minecraft:ender_dragon", boss);
        registerSheet("mob:minecraft:wither", boss);
        registerSheet("aromaaffect:mob_killed_boss", boss);

        // ── Deep dark: the biome, the city, the warden, and the shriek that warns ──
        AnimatedMask deepDark = sheet("deep_dark", 6, true);
        registerSheet("biome:minecraft:deep_dark", deepDark);
        registerSheet("structure:minecraft:ancient_city", deepDark);
        registerSheet("mob:minecraft:warden", deepDark);
        registerSheet("aromaaffect:sculk_shriek", deepDark);
        registerSheet("block:minecraft:sculk", deepDark);
        registerSheet("block:minecraft:sculk_catalyst", deepDark);
        registerSheet("block:minecraft:sculk_sensor", deepDark);
        registerSheet("block:minecraft:sculk_shrieker", deepDark);
        registerSheet("block:minecraft:sculk_vein", deepDark);

        // ── Jungle discovery: authored as a one-shot reveal, so it does not loop ──
        AnimatedMask jungle = sheet("jungle_biome_discovery", 7, false);
        registerSheet("biome:minecraft:jungle", jungle);
        registerSheet("biome:minecraft:bamboo_jungle", jungle);
        registerSheet("biome:minecraft:sparse_jungle", jungle);

        // ── Speed buff: a powered rail kicking the minecart forward ──
        registerSheet("aromaaffect:ride_over_powered_rail",
                sheet("speed_buff_powered_minecart", 4, true));
    }

    private ScentPuffOverlay() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Draw the scent mask BEFORE the HUD so the border/vignette sits behind
        // the hotbar, health, hunger, etc. instead of covering them. Returning
        // true lets the vanilla GUI render proceed (returning false would cancel
        // the entire HUD render).
        RenderCallback.Gui.BEFORE.register((graphics, window) -> {
            render(graphics);
            return true;
        });
        AromaAffect.LOGGER.info("ScentPuffOverlay initialized");
    }

    /**
     * Triggers the scent overlay for a general puff (Omara Device, etc.).
     *
     * @param scentName the scent name (used to resolve the mask texture)
     * @param intensity the scent intensity (0.0 to 1.0)
     */
    public static void onScentPuff(String scentName, double intensity) {
        onScentPuff(scentName, intensity, null);
    }

    /**
     * Triggers the scent overlay, preferring the animated sheet registered for whatever
     * fired the puff.
     *
     * @param scentName the scent name (used to resolve the static mask)
     * @param intensity the scent intensity (0.0 to 1.0)
     * @param sourceKey the firing event id or passive-mode source, or null when the puff
     *                  has neither behind it
     */
    public static void onScentPuff(String scentName, double intensity, String sourceKey) {
        AnimatedMask sheet = sourceKey != null ? SOURCE_MASKS.get(sourceKey) : null;

        if (sheet == null) {
            if (scentName == null || scentName.isBlank()) {
                return;
            }
            Identifier mask = resolveMask(scentName);
            if (mask == null) {
                AromaAffect.LOGGER.debug("No mask mapping for scent '{}' (ScentPuffOverlay)", scentName);
                return;
            }
            activeMask = mask;
            activeSheet = null;
        } else {
            activeMask = sheet.texture();
            activeSheet = sheet;
        }

        pulseStartMs = System.currentTimeMillis();
        lastPuffIntensity = clamp01(intensity);
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (activeMask == null || mc.player == null || mc.isPaused()) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - pulseStartMs;
        if (elapsed < 0 || elapsed > PULSE_DURATION_MS) {
            return;
        }

        float pulseAlpha = computePulseAlpha(elapsed);
        if (pulseAlpha <= 0.001f) {
            return;
        }

        float intensityAlpha = (float) (0.75 + lastPuffIntensity * 0.25);
        float rawAlpha = pulseAlpha * intensityAlpha;
        float finalAlpha = clamp01f(Math.max(MIN_VISIBLE_ALPHA * pulseAlpha, rawAlpha));
        if (finalAlpha <= 0.01f) {
            return;
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int tint = ARGB.color(finalAlpha, 0xFFFFFF);

        if (activeSheet != null) {
            int frame = resolveFrame(activeSheet, elapsed);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    activeMask,
                    0,
                    0,
                    (float) (frame * SHEET_FRAME_W),
                    0.0f,
                    width,
                    height,
                    SHEET_FRAME_W,
                    SHEET_FRAME_H,
                    SHEET_FRAME_W * activeSheet.frames(),
                    SHEET_FRAME_H,
                    tint
            );
            return;
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                activeMask,
                0,
                0,
                0.0f,
                0.0f,
                width,
                height,
                width,
                height,
                width,
                height,
                tint
        );
    }

    /**
     * Cell to draw for an animated sheet. Looping sheets wrap; one-shot sheets stop on
     * their last cell and hold it for the rest of the puff, which fades out anyway.
     */
    private static int resolveFrame(AnimatedMask sheet, long elapsedMs) {
        long step = elapsedMs / SHEET_FRAME_MS;
        if (sheet.loop()) {
            return (int) (step % sheet.frames());
        }
        return (int) Math.min(step, sheet.frames() - 1L);
    }

    private static float computePulseAlpha(long elapsedMs) {
        if (elapsedMs < FADE_IN_MS) {
            return clamp01f((float) elapsedMs / FADE_IN_MS);
        }
        long fadeOutStart = PULSE_DURATION_MS - FADE_OUT_MS;
        if (elapsedMs >= fadeOutStart) {
            return clamp01f((float) (PULSE_DURATION_MS - elapsedMs) / FADE_OUT_MS);
        }
        return 1.0f;
    }

    private static Identifier resolveMask(String scentName) {
        String key = scentName.toLowerCase(Locale.ROOT).trim();
        return SCENT_MASKS.get(key);
    }

    private static AnimatedMask sheet(String sheetFileStem, int frames, boolean loop) {
        return new AnimatedMask(
                Identifier.fromNamespaceAndPath(
                        AromaAffect.MOD_ID, "textures/masks/animated/" + sheetFileStem + ".png"),
                frames,
                loop);
    }

    private static void registerSheet(String sourceKey, AnimatedMask sheet) {
        SOURCE_MASKS.put(sourceKey, sheet);
    }

    private static void register(String scentName, String maskFileStem) {
        SCENT_MASKS.put(
                scentName.toLowerCase(Locale.ROOT),
                Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/masks/" + maskFileStem + ".png")
        );
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static float clamp01f(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
