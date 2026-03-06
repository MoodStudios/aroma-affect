package com.ovrtechnology.tutorial.oliver.client.dialogue;

import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialOliverDialogueNetworking;
import com.ovrtechnology.network.TutorialOliverTradeNetworking;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dialogue screen for Tutorial Oliver NPC.
 * <p>
 * Features:
 * <ul>
 *   <li>Typewriter text animation (30 characters/second)</li>
 *   <li>Portrait of Oliver that follows mouse</li>
 *   <li>Close button after typing finishes</li>
 *   <li>Keepalive packets to server</li>
 *   <li>Musical typing sounds</li>
 * </ul>
 */
public final class TutorialOliverDialogueScreen extends Screen {

    private static final int KEEPALIVE_INTERVAL_TICKS = 20;
    private static final int BOX_MARGIN = 18;
    private static final int BOX_HEIGHT = 110;
    private static final int HEADER_HEIGHT = 18;
    private static final int PADDING = 10;
    private static final int PORTRAIT_WIDTH = 90;
    private static final float PORTRAIT_Y_OFFSET = 0.35F;

    // Pink theme colors (matching OVR branding)
    private static final int COLOR_BOX_BG = 0xCC0B0D12;
    private static final int COLOR_HEADER_BG = 0xE0141620;
    private static final int COLOR_BORDER = 0xFFFF69B4;  // Pink border
    private static final int COLOR_TEXT = 0xFFE8E8E8;
    private static final int COLOR_HEADER_TEXT = 0xFFFF69B4;  // Pink header

    private static final float CHARACTERS_PER_SECOND = 30.0F;
    private static final int[] HARMONY_SEMITONES = {0, 2, 4, 7, 9, 12};

    /**
     * Dialogue content based on dialogue ID.
     * Organized by tutorial phases for structured progression.
     */
    private static final Map<String, String> DIALOGUE_CONTENT = buildDialogueContent();

    private static Map<String, String> buildDialogueContent() {
        java.util.HashMap<String, String> dialogues = new java.util.HashMap<>();

        // ═══════════════════════════════════════════════════════════════════
        // PHASE 1: Welcome & Introduction
        // ═══════════════════════════════════════════════════════════════════
        dialogues.put("phase1",
                "Welcome, stranger! I am Oliver, keeper of ancient scent knowledge. " +
                "Here, I shall teach you how the wonderful world of noses works... " +
                "Follow the glowing path ahead to find the Nose Smith. He holds the key to your aromatic journey!");

        // ═══════════════════════════════════════════════════════════════════
        // PHASE 2: Finding the Nose Smith
        // ═══════════════════════════════════════════════════════════════════
        dialogues.put("phase2",
                "Excellent! You've found the Nose Smith. Now, speak with him and discover what he seeks. " +
                "Every great journey begins with a simple trade...");

        // ═══════════════════════════════════════════════════════════════════
        // PHASE 3: The Flower Quest
        // ═══════════════════════════════════════════════════════════════════
        dialogues.put("phase3",
                "The Nose Smith desires a special flower! Search the nearby gardens and bring him what he seeks. " +
                "Once you deliver the flower, he will reward you with something truly remarkable...");

        // ═══════════════════════════════════════════════════════════════════
        // PHASE 4: Obtaining the Nose
        // ═══════════════════════════════════════════════════════════════════
        dialogues.put("phase4",
                "Magnificent! You now possess your very own Nose! Equip it and discover a world of hidden scents. " +
                "With this gift, you can track creatures, find treasures, and experience Minecraft like never before!");

        // ═══════════════════════════════════════════════════════════════════
        // PHASE 5: Tutorial Complete
        // ═══════════════════════════════════════════════════════════════════
        dialogues.put("phase5",
                "Your training is complete, young sniffer! The aromatic world awaits you. " +
                "Remember: your nose knows the way. Safe travels, and may your scents be ever fragrant!");

        // ═══════════════════════════════════════════════════════════════════
        // Utility Dialogues
        // ═══════════════════════════════════════════════════════════════════
        dialogues.put("default", "Welcome to the OVR Experience! I'm Oliver, your guide. Let me show you around...");
        dialogues.put("hint", "Need a hint? Follow the glowing particles - they'll guide your way!");
        dialogues.put("lost", "Seems like you've wandered off the path! Look for the shimmering trail nearby.");
        dialogues.put("goodbye", "Safe travels, adventurer! Remember: Follow your nose!");

        // ═══════════════════════════════════════════════════════════════════
        // Boss Dialogues - Area Entry (before boss spawns)
        // ═══════════════════════════════════════════════════════════════════
        dialogues.put("boss_blaze_enter",
                "Watch out, adventurer! A dangerous Blaze lurks ahead! " +
                "Defeat this fiery creature and collect the materials it drops. " +
                "I'll need those items to craft your next Nose upgrade. Good luck!");

        dialogues.put("boss_dragon_enter",
                "By the ancient scents! A Dragon guards this territory! " +
                "This will be your greatest challenge yet. Slay the beast and gather its essence. " +
                "With those materials, I can create something truly extraordinary for you!");

        // ═══════════════════════════════════════════════════════════════════
        // Boss Dialogues - After Kill (with trade)
        // ═══════════════════════════════════════════════════════════════════
        dialogues.put("boss_blaze_killed",
                "Incredible work, adventurer! You've defeated the Blaze! " +
                "Now bring me those materials you've collected - the Blaze Powder, Flint, and Ender Pearl. " +
                "With these, I can craft your next Nose upgrade. Trade with me when you're ready!");

        dialogues.put("boss_dragon_killed",
                "MAGNIFICENT! You have slain the Dragon! Few have accomplished such a feat! " +
                "Bring me the Dragon's Breath and scales you've collected. " +
                "I shall forge you a legendary Nose unlike any other. Trade with me!");

        // ═══════════════════════════════════════════════════════════════════
        // Dream End - Player wakes up (self-dialogue)
        // ═══════════════════════════════════════════════════════════════════
        dialogues.put("dream_end_wakeup",
                "What... what happened? That dragon, the battle... was it all a dream? " +
                "No... I can still smell the scents. The Nose is real. Everything I learned is real. " +
                "The aromas, the trails, the world beyond what eyes can see... " +
                "This is just the beginning of my journey.");

        return dialogues;
    }

    private final LivingEntity portraitEntity;
    @Nullable
    private final TutorialOliverEntity oliver; // null in self-dialogue mode
    private final boolean selfMode;
    private final String dialogueId;
    private final boolean hasTrade;
    private final String tradeId;

    private Component dialogue = Component.empty();
    private int[] dialogueCodepoints = new int[0];
    private List<FormattedCharSequence> wrappedLines = List.of();
    private int[] lineCodepointCounts = new int[0];
    private int totalCodepoints = 0;

    private float typeProgress = 0.0F;
    private int typedCodepoints = 0;
    private boolean finished = false;

    private int keepAliveTicks = 0;

    @Nullable
    private Button closeButton;
    @Nullable
    private Button tradeButton;
    private boolean buttonsVisible = false;
    private boolean closedByTrade = false;

    public TutorialOliverDialogueScreen(TutorialOliverEntity oliver) {
        super(Component.literal("Oliver"));
        this.portraitEntity = oliver;
        this.oliver = oliver;
        this.selfMode = false;
        this.dialogueId = oliver.getDialogueId();
        this.hasTrade = false;
        this.tradeId = "";
    }

    public TutorialOliverDialogueScreen(TutorialOliverEntity oliver, String dialogueId,
                                         boolean hasTrade, String tradeId) {
        super(Component.literal("Oliver"));
        this.portraitEntity = oliver;
        this.oliver = oliver;
        this.selfMode = false;
        this.dialogueId = dialogueId;
        this.hasTrade = hasTrade;
        this.tradeId = tradeId != null ? tradeId : "";
    }

    /**
     * Self-dialogue mode: renders the player's own skin in the portrait.
     * Used for the dream ending sequence where the player "talks to themselves".
     */
    public TutorialOliverDialogueScreen(LivingEntity playerEntity, String speakerName,
                                         String dialogueId) {
        super(Component.literal(speakerName));
        this.portraitEntity = playerEntity;
        this.oliver = null;
        this.selfMode = true;
        this.dialogueId = dialogueId;
        this.hasTrade = false;
        this.tradeId = "";
    }

    @Override
    protected void init() {
        rebuildDialogue(true);
        sendTalkingState(true);

        int bottom = this.height - BOX_MARGIN;
        int right = this.width - BOX_MARGIN;
        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonY = bottom - PADDING - buttonHeight;

        int closeX = right - PADDING - buttonWidth;

        if (hasTrade && !tradeId.isEmpty()) {
            int tradeButtonWidth = 60;
            tradeButton = Button.builder(Component.literal("Trade"), btn -> onTrade())
                    .bounds(closeX - tradeButtonWidth - 4, buttonY, tradeButtonWidth, buttonHeight)
                    .build();
            tradeButton.visible = false;
            addRenderableWidget(tradeButton);
        }

        closeButton = Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(closeX, buttonY, buttonWidth, buttonHeight)
                .build();

        closeButton.visible = false;
        buttonsVisible = false;

        addRenderableWidget(closeButton);
    }

    @Override
    public void tick() {
        super.tick();

        // Send keepalive packets to server
        keepAliveTicks++;
        if (keepAliveTicks >= KEEPALIVE_INTERVAL_TICKS) {
            keepAliveTicks = 0;
            sendTalkingState(true);
        }

        if (finished) {
            return;
        }

        // Check if typing is complete
        if (typedCodepoints >= totalCodepoints) {
            finished = true;
            showButtons();
            return;
        }

        // Advance typewriter animation
        typeProgress = Math.min(totalCodepoints, typeProgress + (CHARACTERS_PER_SECOND / 20.0F));
        int desired = (int) typeProgress;
        while (typedCodepoints < desired && typedCodepoints < totalCodepoints) {
            int codePoint = dialogueCodepoints[typedCodepoints];
            typedCodepoints++;
            playTypeSoundFor(codePoint);
        }

        if (typedCodepoints >= totalCodepoints) {
            finished = true;
            showButtons();
        }
    }

    @Override
    public void removed() {
        sendTalkingState(false);

        // Notify server that dialogue was closed (triggers on-complete hooks)
        // Skip if closed by trade — trade has its own on-complete flow via TutorialTradeHandler
        if (!closedByTrade) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null && minecraft.level != null) {
                TutorialDialogueContentNetworking.sendDialogueClosed(
                        minecraft.level.registryAccess(), dialogueId);
            }
        }

        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = BOX_MARGIN;
        int right = this.width - BOX_MARGIN;
        int bottom = this.height - BOX_MARGIN;
        int top = bottom - BOX_HEIGHT;

        // Draw dialogue box background
        guiGraphics.fill(left, top, right, bottom, COLOR_BOX_BG);
        guiGraphics.fill(left, top, right, top + HEADER_HEIGHT, COLOR_HEADER_BG);

        // Draw pink border
        drawBorder(guiGraphics, left, top, right, bottom, COLOR_BORDER);

        // Draw header with Oliver's name
        int headerTextX = left + PADDING;
        int headerTextY = top + 5;
        guiGraphics.drawString(this.font, this.title, headerTextX, headerTextY, COLOR_HEADER_TEXT, true);

        // Draw Oliver's portrait
        int portraitLeft = left + PADDING;
        int portraitTop = top + HEADER_HEIGHT + PADDING;
        int portraitRight = portraitLeft + PORTRAIT_WIDTH;
        int portraitBottom = bottom - PADDING;

        int portraitScale = Math.min(portraitRight - portraitLeft, portraitBottom - portraitTop) - 8;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                portraitLeft,
                portraitTop,
                portraitRight,
                portraitBottom,
                portraitScale,
                PORTRAIT_Y_OFFSET,
                (float) mouseX,
                (float) mouseY,
                portraitEntity
        );

        // Draw typewriter text
        int textLeft = portraitRight + PADDING;
        int textTop = portraitTop;
        drawTypewriterText(guiGraphics, textLeft, textTop);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void sendTalkingState(boolean talking) {
        if (selfMode || oliver == null) {
            return; // No Oliver entity to notify in self-dialogue mode
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.level == null) {
            return;
        }

        TutorialOliverDialogueNetworking.sendDialogueState(
                minecraft.level.registryAccess(),
                oliver.getId(),
                talking
        );
    }

    private void drawTypewriterText(GuiGraphics guiGraphics, int x, int y) {
        if (wrappedLines.isEmpty()) {
            return;
        }

        int remaining = typedCodepoints;
        int lineY = y;
        for (int i = 0; i < wrappedLines.size(); i++) {
            int lineChars = lineCodepointCounts[i];
            int toShow = Math.min(remaining, lineChars);
            if (toShow <= 0) {
                break;
            }

            FormattedCharSequence partial = take(wrappedLines.get(i), toShow);
            guiGraphics.drawString(this.font, partial, x, lineY, COLOR_TEXT, true);
            remaining -= toShow;
            lineY += this.font.lineHeight + 2;

            // Limit visible lines
            if (lineY > y + 4 * (this.font.lineHeight + 2)) {
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!finished) {
            finishTyping();
            return true;
        }

        if (buttonsVisible) {
            return super.mouseClicked(event, doubleClick);
        }

        onClose();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (event.isEscape()) {
            onClose();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
            if (!finished) {
                finishTyping();
                return true;
            }
            if (!buttonsVisible) {
                onClose();
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void finishTyping() {
        typedCodepoints = totalCodepoints;
        finished = true;
        showButtons();
    }

    private void showButtons() {
        if (buttonsVisible) {
            return;
        }
        buttonsVisible = true;
        if (closeButton != null) {
            closeButton.visible = true;
        }
        if (tradeButton != null) {
            tradeButton.visible = true;
        }
    }

    private void onTrade() {
        if (tradeId.isEmpty() || selfMode || oliver == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null && minecraft.level != null) {
            TutorialOliverTradeNetworking.sendTradeRequest(
                    minecraft.level.registryAccess(), oliver.getId(), tradeId);
        }

        // Don't send dialogueClosed when closing via trade (trade has its own on-complete flow)
        closedByTrade = true;
        onClose();
    }

    private void rebuildDialogue(boolean resetTypewriter) {
        this.dialogue = buildDialogue();
        this.dialogueCodepoints = this.dialogue.getString().codePoints().toArray();

        int left = BOX_MARGIN;
        int right = this.width - BOX_MARGIN;

        int textLeft = left + PADDING + PORTRAIT_WIDTH + PADDING;
        int textWidth = (right - PADDING) - textLeft;

        this.wrappedLines = this.font.split(this.dialogue, Math.max(16, textWidth));

        List<Integer> counts = new ArrayList<>(this.wrappedLines.size());
        int total = 0;
        for (FormattedCharSequence seq : this.wrappedLines) {
            int c = countCodepoints(seq);
            counts.add(c);
            total += c;
        }
        this.totalCodepoints = total;
        this.lineCodepointCounts = counts.stream().mapToInt(Integer::intValue).toArray();

        if (resetTypewriter) {
            this.typeProgress = 0.0F;
            this.typedCodepoints = 0;
            this.finished = false;
        }
    }

    private Component buildDialogue() {
        // Check custom data-driven dialogues first
        String customText = TutorialDialogueContentNetworking.getClientCachedText(dialogueId);
        if (customText != null) {
            com.ovrtechnology.AromaAffect.LOGGER.info("Using custom dialogue text for '{}': {}", dialogueId, customText);
            return Component.literal(customText);
        }

        // Fall back to hardcoded dialogue content
        String content = DIALOGUE_CONTENT.getOrDefault(dialogueId, DIALOGUE_CONTENT.get("default"));
        com.ovrtechnology.AromaAffect.LOGGER.info("Using hardcoded dialogue for '{}' (custom not found in cache). Cache has: {}",
                dialogueId, TutorialDialogueContentNetworking.getClientCacheKeys());
        return Component.literal(content);
    }

    private void playTypeSoundFor(int codePoint) {
        // Typing sound disabled for tutorial dialogues - voice files will be used instead
    }

    private static void drawBorder(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, top + 1, color);
        guiGraphics.fill(left, bottom - 1, right, bottom, color);
        guiGraphics.fill(left, top, left + 1, bottom, color);
        guiGraphics.fill(right - 1, top, right, bottom, color);
    }

    private static int countCodepoints(FormattedCharSequence sequence) {
        int[] count = {0};
        sequence.accept((i, style, codePoint) -> {
            count[0]++;
            return true;
        });
        return count[0];
    }

    private static FormattedCharSequence take(FormattedCharSequence sequence, int codepoints) {
        if (codepoints <= 0) {
            return FormattedCharSequence.EMPTY;
        }

        return sink -> {
            int[] count = {0};
            return sequence.accept((i, style, codePoint) -> {
                if (count[0] >= codepoints) {
                    return false;
                }
                count[0]++;
                return sink.accept(i, style, codePoint);
            });
        };
    }
}
