package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.util.Texts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Lets players send feedback about the mod straight from the radial menu.
 * Submissions are POSTed asynchronously to the OVR/OMARA backend via {@link FeedbackClient}.
 */
public class FeedbackScreen extends BaseMenuScreen {

    private static final Identifier ICON_BACK = Identifier.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/radial/icon_back.png");

    private static final int COL_BG_PANEL = 0xDD1A1A2E;
    private static final int COL_ACCENT = 0xFF9A7CFF;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM = 0xFFAAAAAA;
    private static final int COL_HOVER = 0x40FFFFFF;
    private static final int COL_DISABLED = 0xFF666666;
    private static final int COL_OUTLINE = 0x66FFFFFF;
    private static final int COL_SUBMIT = 0x9944BB44;
    private static final int COL_SUBMIT_HOVER = 0xDD44DD44;
    private static final int COL_SUCCESS = 0xFF44FF44;
    private static final int COL_PURPLE_FADE = 0x809A7CFF;
    private static final int COL_TOOLTIP_OUTLINE = 0x88FFFFFF;

    private static final int PANEL_W = 440;
    private static final int INNER_PAD = 16;
    private static final int LINE_H = 10;
    private static final int NAME_BOX_HEIGHT = 18;
    private static final int CHECKBOX_SIZE = 12;
    private static final int SUBMIT_BTN_HEIGHT = 20;
    private static final int FEEDBACK_BOX_PREFERRED_HEIGHT = 72;
    private static final int FEEDBACK_BOX_MIN_HEIGHT = 40;

    private static final int FEEDBACK_CHAR_LIMIT = 1000;
    private static final int NAME_CHAR_LIMIT = 64;

    private static final int BACK_BUTTON_SIZE = 24;
    private static final int BACK_BUTTON_PADDING = 8;

    private enum State {
        FORM,
        SUBMITTING,
        THANKS
    }

    private State state = State.FORM;

    private MultiLineEditBox feedbackBox;
    private EditBox nameBox;

    /**
     * Mirrored field values so text survives screen re-init (e.g. window resize).
     */
    private String feedbackText = "";

    private String nameText = "";
    private boolean anonymous = false;

    private boolean isHoveringBack = false;
    private boolean isHoveringSubmit = false;
    private boolean isHoveringCheckbox = false;
    private boolean isHoveringClose = false;

    private int checkboxX, checkboxY;
    private int submitX, submitY, submitW, submitH;
    private int closeX, closeY, closeW, closeH;

    public FeedbackScreen() {
        super(Texts.tr("feedback.aromaaffect.title"));
    }

    @Override
    protected void init() {
        super.init();

        // MultiLineEditBox wraps text to the width passed at construction; setWidth() afterwards
        // only resizes the frame, not the wrap width. So build it with the final content width
        // (init() re-runs on resize, rebuilding at the new width).
        int contentW = Math.min(PANEL_W, width - 40) - INNER_PAD * 2;

        feedbackBox = MultiLineEditBox.builder()
                .setPlaceholder(Texts.tr("feedback.aromaaffect.feedback_placeholder"))
                .setShowBackground(true)
                .build(font, contentW, FEEDBACK_BOX_MIN_HEIGHT, Texts.empty());
        feedbackBox.setCharacterLimit(FEEDBACK_CHAR_LIMIT);
        feedbackBox.setValueListener(value -> feedbackText = value);
        feedbackBox.setValue(feedbackText);
        addWidget(feedbackBox);

        nameBox = new EditBox(font, 0, 0, 100, NAME_BOX_HEIGHT, Texts.empty());
        nameBox.setMaxLength(NAME_CHAR_LIMIT);
        nameBox.setHint(Texts.tr("feedback.aromaaffect.name_hint"));
        nameBox.setResponder(value -> nameText = value);
        nameBox.setValue(nameText);
        addWidget(nameBox);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mouseX, int mouseY,
                                 float partialTick, float animationProgress) {
        float a = animationProgress;
        int centerX = width / 2;
        int centerY = height / 2;

        int panelW = Math.min(PANEL_W, width - 40);
        int contentLeft0 = centerX - panelW / 2 + INNER_PAD;
        int contentW = panelW - INNER_PAD * 2;

        if (state == State.THANKS) {
            renderThanksPanel(g, mouseX, mouseY, centerX, centerY, panelW, contentW, a);
            renderBackButton(g, mouseX, mouseY, a);
            return;
        }

        // Pre-split wrapped text so we can size the panel exactly to its content.
        List<FormattedCharSequence> descLines =
                font.split(Texts.tr("feedback.aromaaffect.description"), contentW);
        List<FormattedCharSequence> nameLabelLines =
                font.split(Texts.tr("feedback.aromaaffect.name_label"), contentW);
        List<FormattedCharSequence> footerLines =
                font.split(Texts.tr("feedback.aromaaffect.footer"), contentW);

        // Vertical budget for everything except the feedback box (top-down, deterministic).
        int fixedH = 12
                + 18 // title block
                + descLines.size() * LINE_H
                + 6
                + 14 // "Feedback:" label
                + 10 // gap below box
                + nameLabelLines.size() * LINE_H
                + 4
                + NAME_BOX_HEIGHT
                + 10
                + 22 // checkbox row
                + footerLines.size() * LINE_H
                + 8
                + SUBMIT_BTN_HEIGHT
                + INNER_PAD; // bottom padding

        int maxPanelH = height - 30;
        int feedbackBoxH = FEEDBACK_BOX_PREFERRED_HEIGHT;
        int panelH = fixedH + feedbackBoxH;
        if (panelH > maxPanelH) {
            panelH = maxPanelH;
            feedbackBoxH = Math.max(FEEDBACK_BOX_MIN_HEIGHT, maxPanelH - fixedH);
            panelH = fixedH + feedbackBoxH;
        }

        int panelLeft = centerX - panelW / 2;
        int panelTop = centerY - panelH / 2;
        int panelRight = panelLeft + panelW;
        int panelBottom = panelTop + panelH;

        drawPanel(g, panelLeft, panelTop, panelRight, panelBottom, panelW, panelH, a);

        Component title = Texts.tr("feedback.aromaaffect.title");
        drawCentered(g, title, centerX, panelTop + 12, MenuRenderUtils.withAlpha(COL_TEXT, a));

        boolean ready = a > 0.95f;
        int y = panelTop + 12 + 18;

        // Intro paragraph.
        for (FormattedCharSequence line : descLines) {
            g.text(font, line, contentLeft0, y, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a), false);
            y += LINE_H;
        }
        y += 6;

        // "Feedback:" label + multiline text area.
        g.text(font, Texts.tr("feedback.aromaaffect.feedback_label"), contentLeft0, y,
                MenuRenderUtils.withAlpha(COL_TEXT, a));
        y += 14;
        if (ready) {
            feedbackBox.setX(contentLeft0);
            feedbackBox.setY(y);
            feedbackBox.setWidth(contentW);
            feedbackBox.setHeight(feedbackBoxH);
            feedbackBox.extractRenderState(g, mouseX, mouseY, partialTick);
        }
        y += feedbackBoxH + 10;

        // Name label + field.
        for (FormattedCharSequence line : nameLabelLines) {
            g.text(font, line, contentLeft0, y, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a), false);
            y += LINE_H;
        }
        y += 4;
        if (ready) {
            nameBox.setX(contentLeft0);
            nameBox.setY(y);
            nameBox.setWidth(contentW);
            nameBox.extractRenderState(g, mouseX, mouseY, partialTick);
        }
        y += NAME_BOX_HEIGHT + 10;

        // "Submit anonymously" checkbox.
        checkboxX = contentLeft0;
        checkboxY = y;
        isHoveringCheckbox = ready
                && isInBounds(mouseX, mouseY, checkboxX, checkboxY, contentW, CHECKBOX_SIZE);
        int boxBg = isHoveringCheckbox
                ? MenuRenderUtils.withAlpha(COL_HOVER, a)
                : MenuRenderUtils.withAlpha(COL_OUTLINE, a);
        g.fill(checkboxX, checkboxY, checkboxX + CHECKBOX_SIZE, checkboxY + CHECKBOX_SIZE, boxBg);
        MenuRenderUtils.renderOutline(g, checkboxX, checkboxY, CHECKBOX_SIZE, CHECKBOX_SIZE,
                MenuRenderUtils.withAlpha(COL_ACCENT, a));
        if (anonymous) {
            g.fill(checkboxX + 3, checkboxY + 3,
                    checkboxX + CHECKBOX_SIZE - 3, checkboxY + CHECKBOX_SIZE - 3,
                    MenuRenderUtils.withAlpha(COL_ACCENT, a));
        }
        g.text(font, Texts.tr("feedback.aromaaffect.anonymous"),
                checkboxX + CHECKBOX_SIZE + 6, checkboxY + 2,
                MenuRenderUtils.withAlpha(COL_TEXT, a));
        y += 22;

        // Footer line.
        for (FormattedCharSequence line : footerLines) {
            g.text(font, line, contentLeft0, y, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a), false);
            y += LINE_H;
        }
        y += 8;

        // Submit button.
        submitW = 120;
        submitH = SUBMIT_BTN_HEIGHT;
        submitX = centerX - submitW / 2;
        submitY = y;

        boolean canSubmit = state == State.FORM && !feedbackText.isBlank();
        isHoveringSubmit = ready && canSubmit
                && isInBounds(mouseX, mouseY, submitX, submitY, submitW, submitH);

        int submitBg;
        if (state == State.SUBMITTING) {
            submitBg = MenuRenderUtils.withAlpha(COL_SUBMIT, a * 0.6f);
        } else if (!canSubmit) {
            submitBg = MenuRenderUtils.withAlpha(COL_DISABLED, a * 0.6f);
        } else if (isHoveringSubmit) {
            submitBg = MenuRenderUtils.withAlpha(COL_SUBMIT_HOVER, a);
        } else {
            submitBg = MenuRenderUtils.withAlpha(COL_SUBMIT, a);
        }
        g.fill(submitX, submitY, submitX + submitW, submitY + submitH, submitBg);
        MenuRenderUtils.renderOutline(g, submitX, submitY, submitW, submitH,
                MenuRenderUtils.withAlpha(COL_OUTLINE, a));
        Component submitLabel = state == State.SUBMITTING
                ? Texts.tr("feedback.aromaaffect.submitting")
                : Texts.tr("feedback.aromaaffect.submit");
        drawCentered(g, submitLabel, submitX + submitW / 2, submitY + (submitH - 8) / 2,
                MenuRenderUtils.withAlpha(canSubmit || state == State.SUBMITTING ? COL_TEXT : COL_TEXT_DIM, a));

        renderBackButton(g, mouseX, mouseY, a);
    }

    private void drawPanel(GuiGraphicsExtractor g, int panelLeft, int panelTop, int panelRight,
                           int panelBottom, int panelW, int panelH, float a) {
        g.fill(panelLeft, panelTop, panelRight, panelBottom, MenuRenderUtils.withAlpha(COL_BG_PANEL, a));
        MenuRenderUtils.renderOutline(g, panelLeft, panelTop, panelW, panelH,
                MenuRenderUtils.withAlpha(COL_OUTLINE, a));
        g.fill(panelLeft, panelTop, panelRight, panelTop + 3, MenuRenderUtils.withAlpha(COL_ACCENT, a));
    }

    private void renderThanksPanel(GuiGraphicsExtractor g, int mouseX, int mouseY, int centerX,
                                   int centerY, int panelW, int contentW, float a) {
        int panelH = Math.min(220, height - 30);
        int panelLeft = centerX - panelW / 2;
        int panelTop = centerY - panelH / 2;
        int panelRight = panelLeft + panelW;
        int panelBottom = panelTop + panelH;

        drawPanel(g, panelLeft, panelTop, panelRight, panelBottom, panelW, panelH, a);

        drawCentered(g, Texts.tr("feedback.aromaaffect.title"), centerX, panelTop + 12,
                MenuRenderUtils.withAlpha(COL_TEXT, a));

        int y = panelTop + 48;
        drawCentered(g, Texts.tr("feedback.aromaaffect.thanks_title"), centerX, y,
                MenuRenderUtils.withAlpha(COL_SUCCESS, a));
        y += 20;
        for (FormattedCharSequence line : font.split(Texts.tr("feedback.aromaaffect.thanks_body"), contentW)) {
            drawCentered(g, line, centerX, y, MenuRenderUtils.withAlpha(COL_TEXT_DIM, a));
            y += 11;
        }

        closeW = 120;
        closeH = SUBMIT_BTN_HEIGHT;
        closeX = centerX - closeW / 2;
        closeY = panelBottom - INNER_PAD - closeH;
        isHoveringClose = isInBounds(mouseX, mouseY, closeX, closeY, closeW, closeH);

        int closeBg = isHoveringClose
                ? MenuRenderUtils.withAlpha(COL_PURPLE_FADE, a)
                : MenuRenderUtils.withAlpha(COL_HOVER, a);
        g.fill(closeX, closeY, closeX + closeW, closeY + closeH, closeBg);
        MenuRenderUtils.renderOutline(g, closeX, closeY, closeW, closeH,
                MenuRenderUtils.withAlpha(COL_OUTLINE, a));
        drawCentered(g, Texts.tr("feedback.aromaaffect.close"), closeX + closeW / 2,
                closeY + (closeH - 8) / 2, MenuRenderUtils.withAlpha(COL_TEXT, a));
    }

    private void renderBackButton(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        float appear = Math.max(0.0f, (a - 0.2f) / 0.8f);
        if (appear <= 0.0f) return;

        int bx = BACK_BUTTON_PADDING;
        int by = BACK_BUTTON_PADDING;
        int bSize = BACK_BUTTON_SIZE + 8;

        isHoveringBack = isInBounds(mouseX, mouseY, bx, by, bSize, bSize);

        if (isHoveringBack) {
            g.fill(bx, by, bx + bSize, by + bSize, MenuRenderUtils.withAlpha(COL_PURPLE_FADE, appear));
            MenuRenderUtils.renderOutline(g, bx, by, bSize, bSize,
                    MenuRenderUtils.withAlpha(COL_TOOLTIP_OUTLINE, appear));
        }

        float scale = isHoveringBack ? 1.1f : 1.0f;
        int iconSize = (int) (BACK_BUTTON_SIZE * scale * appear);
        int iconOffset = (bSize - iconSize) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, ICON_BACK,
                bx + iconOffset, by + iconOffset,
                0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
    }

    /**
     * 26.1 has no drawCenteredString helper on the graphics context, so centre manually.
     */
    private void drawCentered(GuiGraphicsExtractor g, Component text, int centerX, int y, int color) {
        g.text(font, text, centerX - font.width(text) / 2, y, color);
    }

    private void drawCentered(GuiGraphicsExtractor g, FormattedCharSequence text, int centerX, int y, int color) {
        g.text(font, text, centerX - font.width(text) / 2, y, color);
    }

    private static boolean isInBounds(double x, double y, int bx, int by, int bw, int bh) {
        return x >= bx && x < bx + bw && y >= by && y < by + bh;
    }

    @Override
    protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (isHoveringBack) {
            MenuRenderUtils.playClickSound();
            MenuManager.returnToRadialMenu();
            return true;
        }

        if (state == State.THANKS) {
            if (isHoveringClose) {
                MenuRenderUtils.playClickSound();
                MenuManager.returnToRadialMenu();
                return true;
            }
            return false;
        }

        if (isHoveringCheckbox) {
            anonymous = !anonymous;
            MenuRenderUtils.playClickSound();
            return true;
        }

        if (isHoveringSubmit) {
            submitFeedback();
            return true;
        }

        return false;
    }

    private void submitFeedback() {
        if (state != State.FORM || feedbackText.isBlank()) {
            return;
        }

        state = State.SUBMITTING;
        MenuRenderUtils.playClickSound();
        if (feedbackBox != null) {
            feedbackBox.setFocused(false);
        }
        if (nameBox != null) {
            nameBox.setFocused(false);
        }

        String feedback = feedbackText;
        String name = nameText;
        boolean anon = anonymous;

        FeedbackClient.submit(feedback, name, anon)
                .whenComplete((ok, throwable) -> Minecraft.getInstance()
                        .execute(() -> onSubmitComplete(Boolean.TRUE.equals(ok))));
    }

    private void onSubmitComplete(boolean ok) {
        if (ok) {
            state = State.THANKS;
        } else {
            state = State.FORM;
            showErrorNotification(Texts.tr("feedback.aromaaffect.error"));
        }
    }

    @Override
    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (feedbackBox != null && feedbackBox.isFocused()) {
                feedbackBox.setFocused(false);
                return true;
            }
            if (nameBox != null && nameBox.isFocused()) {
                nameBox.setFocused(false);
                return true;
            }
            MenuManager.returnToRadialMenu();
            return true;
        }
        // Let focused text widgets handle every other key (typing, backspace, enter, arrows).
        return false;
    }
}
