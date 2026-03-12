package com.ovrtechnology.tutorial.searchdiamond.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Start screen shown when player begins the diamond search minigame.
 * Features a typewriter effect for the instructions.
 */
public class SearchDiamondStartScreen extends Screen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;

    // Typewriter effect settings
    private static final int CHARS_PER_TICK = 1;
    private static final int TICKS_PER_CHAR = 1;

    // The full instruction text
    private static final String TITLE = "Do you want Diamonds?";
    private static final String FULL_TEXT =
            "Now we will put everything you've learned to the test. " +
            "Use your nose to find the valuable diamond! " +
            "Press R to open the menu, search for Diamond in Blocks, " +
            "and let's mine!";

    // Animation state
    private int ticksOpen = 0;
    private int visibleChars = 0;
    private Button startButton;
    private boolean readySent = false;

    public SearchDiamondStartScreen() {
        super(Component.literal("Search Diamond"));
    }

    @Override
    protected void init() {
        super.init();

        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int buttonY = this.height - 60;

        startButton = Button.builder(
                Component.literal("Let's Go!"),
                btn -> this.onClose()
        ).bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        // Button starts invisible until typewriter is done
        startButton.visible = false;
        startButton.active = false;

        addRenderableWidget(startButton);
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;

        // Update typewriter effect
        if (ticksOpen % TICKS_PER_CHAR == 0 && visibleChars < FULL_TEXT.length()) {
            visibleChars = Math.min(visibleChars + CHARS_PER_TICK, FULL_TEXT.length());
        }

        // Show button when typewriter is complete
        if (visibleChars >= FULL_TEXT.length() && startButton != null) {
            startButton.visible = true;
            startButton.active = true;
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent dark overlay
        graphics.fill(0, 0, this.width, this.height, 0xDD000000);

        var font = Minecraft.getInstance().font;

        // Title with cyan color (0xFF for full alpha)
        int titleWidth = font.width(TITLE);
        int titleX = (this.width - titleWidth) / 2;
        graphics.drawString(font, TITLE, titleX, 40, 0xFF55FFFF, true);

        // Typewriter text - word wrap
        String visibleText = FULL_TEXT.substring(0, visibleChars);

        // Add blinking cursor if still typing
        if (visibleChars < FULL_TEXT.length() && (ticksOpen / 5) % 2 == 0) {
            visibleText += "_";
        }

        // Word wrap the text
        int maxWidth = this.width - 80;
        int startY = 70;
        int lineHeight = 12;

        // Split into words and wrap
        String[] words = visibleText.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int currentY = startY;

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (font.width(testLine) > maxWidth && currentLine.length() > 0) {
                // Draw current line and start new one
                int lineX = (this.width - font.width(currentLine.toString())) / 2;
                graphics.drawString(font, currentLine.toString(), lineX, currentY, 0xFFFFFFFF, false);
                currentY += lineHeight;
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }

        // Draw remaining text
        if (currentLine.length() > 0) {
            int lineX = (this.width - font.width(currentLine.toString())) / 2;
            graphics.drawString(font, currentLine.toString(), lineX, currentY, 0xFFFFFFFF, false);
        }
    }

    @Override
    public void onClose() {
        // Always send ready when closing (whether by button or ESC)
        if (!readySent) {
            readySent = true;
            SearchDiamondClient.sendPlayerReady();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
