package com.ovrtechnology.entity.nosesmith.client.dialogue;

import com.ovrtechnology.entity.nosesmith.NoseSmithEntity;
import com.ovrtechnology.network.NoseSmithDialogueNetworking;
import com.ovrtechnology.network.NoseSmithTradeNetworking;
import com.ovrtechnology.util.Colors;
import com.ovrtechnology.util.Texts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class NoseSmithDialogueScreen extends Screen {
    private static final int KEEPALIVE_INTERVAL_TICKS = 20;
    private static final int BOX_MARGIN = 18;
    private static final int BOX_HEIGHT = 110;
    private static final int HEADER_HEIGHT = 18;
    private static final int PADDING = 10;
    private static final int PORTRAIT_WIDTH = 90;
    private static final float PORTRAIT_Y_OFFSET = 0.35F;

    private static final int COLOR_BOX_BG = 0xCC0B0D12;
    private static final int COLOR_HEADER_BG = 0xE0141620;
    private static final int COLOR_BORDER = Colors.ACCENT_PURPLE;
    private static final int COLOR_TEXT = 0xFFE8E8E8;

    private static final float CHARACTERS_PER_SECOND = 30.0F;
    private static final int[] HARMONY_SEMITONES = {0, 2, 4, 7, 9, 12};

    private final NoseSmithEntity noseSmith;

    @Nullable private ResourceLocation lastFlowerId;
    private boolean lastHasNose;

    private Component dialogue = Texts.empty();
    private int[] dialogueCodepoints = new int[0];
    private List<FormattedCharSequence> wrappedLines = List.of();
    private int[] lineCodepointCounts = new int[0];
    private int totalCodepoints = 0;

    private float typeProgress = 0.0F;
    private int typedCodepoints = 0;
    private boolean finished = false;

    private int keepAliveTicks = 0;

    @Nullable private Button closeButton;
    @Nullable private Button shopButton;
    private boolean buttonsVisible = false;

    public NoseSmithDialogueScreen(NoseSmithEntity noseSmith) {
        super(noseSmith.getName());
        this.noseSmith = noseSmith;
        this.lastFlowerId = noseSmith.getRequestedFlowerId();
        this.lastHasNose = noseSmith.hasNose();
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
        int gap = 6;

        shopButton =
                Button.builder(
                                Texts.lit("Shop"),
                                btn -> {
                                    Minecraft minecraft = Minecraft.getInstance();
                                    if (minecraft.level != null) {
                                        NoseSmithTradeNetworking.sendOpenShop(
                                                minecraft.level.registryAccess(),
                                                noseSmith.getId());
                                    }
                                    onClose();
                                })
                        .bounds(right - PADDING - buttonWidth, buttonY, buttonWidth, buttonHeight)
                        .build();

        closeButton =
                Button.builder(Texts.lit("Close"), btn -> onClose())
                        .bounds(
                                right - PADDING - buttonWidth * 2 - gap,
                                buttonY,
                                buttonWidth,
                                buttonHeight)
                        .build();

        shopButton.visible = false;
        closeButton.visible = false;
        buttonsVisible = false;

        addRenderableWidget(closeButton);
        addRenderableWidget(shopButton);
    }

    @Override
    public void tick() {
        super.tick();

        ResourceLocation currentFlowerId = noseSmith.getRequestedFlowerId();
        boolean currentHasNose = noseSmith.hasNose();
        if (currentHasNose != lastHasNose || !equalsNullable(currentFlowerId, lastFlowerId)) {
            lastHasNose = currentHasNose;
            lastFlowerId = currentFlowerId;
            rebuildDialogue(true);
        }

        keepAliveTicks++;
        if (keepAliveTicks >= KEEPALIVE_INTERVAL_TICKS) {
            keepAliveTicks = 0;
            sendTalkingState(true);
        }

        if (!lastHasNose && noseSmith.getRegrowthSecondsRemaining() > 0 && keepAliveTicks == 0) {
            rebuildDialogue(false);
        }

        if (finished) {
            return;
        }

        if (typedCodepoints >= totalCodepoints) {
            finished = true;
            showButtons();
            return;
        }

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
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = BOX_MARGIN;
        int right = this.width - BOX_MARGIN;
        int bottom = this.height - BOX_MARGIN;
        int top = bottom - BOX_HEIGHT;

        guiGraphics.fill(left, top, right, bottom, COLOR_BOX_BG);
        guiGraphics.fill(left, top, right, top + HEADER_HEIGHT, COLOR_HEADER_BG);

        drawBorder(guiGraphics, left, top, right, bottom, COLOR_BORDER);

        int headerTextX = left + PADDING;
        int headerTextY = top + 5;
        guiGraphics.drawString(this.font, this.title, headerTextX, headerTextY, COLOR_TEXT, true);

        Block flowerBlock = getRequestedFlowerBlock();
        if (lastHasNose && flowerBlock != null) {
            ItemStack flowerStack = new ItemStack(flowerBlock.asItem());
            int iconX = right - PADDING - 16;
            int iconY = top + 1;
            guiGraphics.renderItem(flowerStack, iconX, iconY);
        }

        int portraitLeft = left + PADDING;
        int portraitTop = top + HEADER_HEIGHT + PADDING;
        int portraitRight = portraitLeft + PORTRAIT_WIDTH;
        int portraitBottom = bottom - PADDING;

        int portraitScale =
                Math.min(portraitRight - portraitLeft, portraitBottom - portraitTop) - 8;
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
                noseSmith);

        int textLeft = portraitRight + PADDING;
        int textTop = portraitTop;
        int textWidth = (right - PADDING) - textLeft;

        drawTypewriterText(guiGraphics, textLeft, textTop, textWidth);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void sendTalkingState(boolean talking) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.level == null) {
            return;
        }

        NoseSmithDialogueNetworking.sendDialogueState(
                minecraft.level.registryAccess(), noseSmith.getId(), talking);
    }

    private void drawTypewriterText(GuiGraphics guiGraphics, int x, int y, int width) {
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
        if (shopButton != null) {
            shopButton.visible = true;
        }
    }

    private void rebuildDialogue(boolean resetTypewriter) {
        this.dialogue = buildDialogue();
        this.dialogueCodepoints = this.dialogue.getString().codePoints().toArray();

        int left = BOX_MARGIN;
        int right = this.width - BOX_MARGIN;
        int bottom = this.height - BOX_MARGIN;
        int top = bottom - BOX_HEIGHT;

        int textLeft = left + PADDING + PORTRAIT_WIDTH + PADDING;
        int portraitTop = top + HEADER_HEIGHT + PADDING;
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
        } else if (finished) {

            this.typedCodepoints = this.totalCodepoints;
            this.typeProgress = this.totalCodepoints;
        }
    }

    private Component buildDialogue() {
        if (!noseSmith.hasNose()) {
            int seconds = noseSmith.getRegrowthSecondsRemaining();
            if (seconds > 0) {
                int min = seconds / 60;
                int sec = seconds % 60;
                String time = String.format("%d:%02d", min, sec);
                return Texts.lit("A deal's a deal. Thanks again. My nose will grow back in ")
                        .append(
                                Texts.lit(time)
                                        .withStyle(
                                                ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                        .append(Texts.lit("."));
            }
            return Texts.lit("A deal's a deal. Thanks again.");
        }

        Block flower = getRequestedFlowerBlock();
        if (flower == null) {
            return Texts.lit("Hmm... I can't decide which flower I want today.");
        }

        Component flowerName =
                Texts.tr(flower.getDescriptionId())
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);

        return Texts.lit("Oh! I'm a flower collector, but I haven't been able to find ")
                .append(flowerName)
                .append(Texts.lit(". Drop one near me and I'll give you my nose!"));
    }

    @Nullable
    private Block getRequestedFlowerBlock() {
        ResourceLocation flowerId = noseSmith.getRequestedFlowerId();
        if (flowerId == null) {
            return null;
        }

        return BuiltInRegistries.BLOCK.get(flowerId).map(holder -> holder.value()).orElse(null);
    }

    private void playTypeSoundFor(int codePoint) {
        if (Character.isWhitespace(codePoint)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() == null) {
            return;
        }

        RandomSource random =
                minecraft.player != null ? minecraft.player.getRandom() : RandomSource.create();

        int octaveRoll = random.nextInt(10);
        int octaveShift = octaveRoll == 0 ? -12 : (octaveRoll <= 2 ? 12 : 0);
        int semitone = HARMONY_SEMITONES[random.nextInt(HARMONY_SEMITONES.length)] + octaveShift;
        float pitch = (float) Math.pow(2.0D, semitone / 12.0D);

        minecraft
                .getSoundManager()
                .play(
                        SimpleSoundInstance.forUI(
                                SoundEvents.NOTE_BLOCK_HARP.value(), 0.175F, pitch));
    }

    private static void drawBorder(
            GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, top + 1, color);
        guiGraphics.fill(left, bottom - 1, right, bottom, color);
        guiGraphics.fill(left, top, left + 1, bottom, color);
        guiGraphics.fill(right - 1, top, right, bottom, color);
    }

    private static int countCodepoints(FormattedCharSequence sequence) {
        int[] count = {0};
        sequence.accept(
                (i, style, codePoint) -> {
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
            return sequence.accept(
                    (i, style, codePoint) -> {
                        if (count[0] >= codepoints) {
                            return false;
                        }
                        count[0]++;
                        return sink.accept(i, style, codePoint);
                    });
        };
    }

    private static boolean equalsNullable(@Nullable Object a, @Nullable Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
