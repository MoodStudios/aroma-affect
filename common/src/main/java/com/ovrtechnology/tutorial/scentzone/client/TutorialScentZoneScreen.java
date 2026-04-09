package com.ovrtechnology.tutorial.scentzone.client;

import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.network.TutorialScentZoneNetworking.ZoneClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Simple GUI for managing scent zones.
 * Left panel: zone list. Right panel: edit selected zone.
 */
public class TutorialScentZoneScreen extends Screen {

    private static final int BG_COLOR = 0x660B0D12;
    private static final int HEADER_COLOR = 0xE0141620;
    private static final int SELECTED_COLOR = 0x44FF69B4;
    private static final int HOVER_COLOR = 0x22FFFFFF;

    private static final int LIST_WIDTH = 130;
    private static final int MARGIN = 10;

    /** All 16 valid OVR scent names. */
    private static final String[] SCENTS = {
            "Beach", "Evergreen", "Desert", "Floral", "Barnyard", "Smoky",
            "Winter", "Terra Silva", "Savory Spice", "Timber", "Petrichor",
            "Sweet", "Machina", "Marine", "Kindred", "Citrus"
    };

    // Edit fields
    private EditBox idField;
    private EditBox xField, yField, zField;
    private EditBox rxField, ryField, rzField;
    private EditBox cooldownField;
    private Button scentButton;
    private Checkbox oneShotCheck;
    private Checkbox enabledCheck;
    private Checkbox showOverlaysCheck;

    private int selectedScentIndex = 14; // default: Kindred
    private int intensityPercent = 50;   // 0-100

    @Nullable
    private String selectedZoneId = null;
    private int listScroll = 0;
    private boolean isCreating = false;

    // Intensity slider state
    private boolean draggingIntensity = false;
    private int sliderX, sliderY, sliderW;

    public TutorialScentZoneScreen() {
        super(Component.literal("Scent Zone Editor"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new TutorialScentZoneScreen());
    }

    @Override
    protected void init() {
        int rightX = MARGIN + LIST_WIDTH + MARGIN;
        int fieldH = 16;
        int y = 35;
        int gap = 20;

        // ID
        idField = new EditBox(font, rightX + 30, y, 120, fieldH, Component.literal("ID"));
        idField.setMaxLength(64);
        addRenderableWidget(idField);
        y += gap;

        // Position X Y Z
        xField = new EditBox(font, rightX + 15, y, 50, fieldH, Component.literal("X"));
        yField = new EditBox(font, rightX + 75, y, 50, fieldH, Component.literal("Y"));
        zField = new EditBox(font, rightX + 135, y, 50, fieldH, Component.literal("Z"));
        xField.setMaxLength(8); yField.setMaxLength(8); zField.setMaxLength(8);
        addRenderableWidget(xField); addRenderableWidget(yField); addRenderableWidget(zField);

        // "My Pos" button
        addRenderableWidget(Button.builder(Component.literal("My Pos"), btn -> {
            if (minecraft != null && minecraft.player != null) {
                xField.setValue(String.valueOf(minecraft.player.blockPosition().getX()));
                yField.setValue(String.valueOf(minecraft.player.blockPosition().getY()));
                zField.setValue(String.valueOf(minecraft.player.blockPosition().getZ()));
            }
        }).bounds(rightX + 195, y, 45, fieldH).build());
        y += gap;

        // Radius X, Y, Z
        rxField = new EditBox(font, rightX + 50, y, 38, fieldH, Component.literal("RX"));
        ryField = new EditBox(font, rightX + 98, y, 38, fieldH, Component.literal("RY"));
        rzField = new EditBox(font, rightX + 146, y, 38, fieldH, Component.literal("RZ"));
        rxField.setMaxLength(4); ryField.setMaxLength(4); rzField.setMaxLength(4);
        addRenderableWidget(rxField); addRenderableWidget(ryField); addRenderableWidget(rzField);
        y += gap;

        // Scent selector with prev/next arrows
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            selectedScentIndex = (selectedScentIndex - 1 + SCENTS.length) % SCENTS.length;
            scentButton.setMessage(Component.literal(SCENTS[selectedScentIndex]));
        }).bounds(rightX + 50, y, 16, fieldH).build());

        scentButton = Button.builder(Component.literal(SCENTS[selectedScentIndex]), btn -> {})
                .bounds(rightX + 68, y, 104, fieldH).build();
        scentButton.active = false; // display only, use arrows to change
        addRenderableWidget(scentButton);

        addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            selectedScentIndex = (selectedScentIndex + 1) % SCENTS.length;
            scentButton.setMessage(Component.literal(SCENTS[selectedScentIndex]));
        }).bounds(rightX + 174, y, 16, fieldH).build());
        y += gap;

        // Intensity slider (rendered manually)
        sliderX = rightX + 65;
        sliderY = y + 2;
        sliderW = 120;
        y += gap;

        // Cooldown
        cooldownField = new EditBox(font, rightX + 80, y, 40, fieldH, Component.literal("Cooldown"));
        cooldownField.setMaxLength(5);
        addRenderableWidget(cooldownField);
        y += gap + 4;

        // Checkboxes
        oneShotCheck = Checkbox.builder(Component.literal("One-shot (only once per session)"), font)
                .pos(rightX, y).build();
        addRenderableWidget(oneShotCheck);
        y += gap;

        enabledCheck = Checkbox.builder(Component.literal("Enabled"), font)
                .pos(rightX, y).selected(true).build();
        addRenderableWidget(enabledCheck);
        y += gap + 8;

        // Action buttons
        int btnW = 55;
        addRenderableWidget(Button.builder(Component.literal("Save"), btn -> onSave())
                .bounds(rightX, y, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), btn -> onDelete())
                .bounds(rightX + (btnW + 3), y, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("New"), btn -> onNew())
                .bounds(rightX + (btnW + 3) * 2, y, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clone"), btn -> onClone())
                .bounds(rightX + (btnW + 3) * 3, y, btnW, 20).build());

        // Show overlays checkbox (bottom-left)
        showOverlaysCheck = Checkbox.builder(Component.literal("Show zones"), font)
                .pos(MARGIN, height - 30)
                .selected(TutorialScentZoneNetworking.isShowZoneOverlays())
                .build();
        addRenderableWidget(showOverlaysCheck);

        // Load selected zone if any
        if (selectedZoneId != null) {
            loadZoneToFields(selectedZoneId);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // No default darkening — we want to see the world
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Semi-transparent background so the world is visible
        g.fill(0, 0, width, height, BG_COLOR);

        // Header
        g.fill(0, 0, width, 22, HEADER_COLOR);
        g.drawString(font, title, MARGIN, 7, 0xFFFF69B4, true);

        // Zone list panel
        int listRight = MARGIN + LIST_WIDTH;
        g.fill(MARGIN - 2, 26, listRight + 2, height - 40, 0x44000000);
        drawBorder(g, MARGIN - 2, 26, listRight + 2, height - 40, 0x66FFFFFF);

        List<ZoneClientData> zones = TutorialScentZoneNetworking.getClientZones();
        int y = 28;
        int rowH = 14;
        for (int i = listScroll; i < zones.size() && y < height - 55; i++) {
            ZoneClientData zone = zones.get(i);
            boolean selected = zone.id().equals(selectedZoneId);
            boolean hover = mouseX >= MARGIN && mouseX < listRight && mouseY >= y && mouseY < y + rowH;

            if (selected) g.fill(MARGIN, y, listRight, y + rowH, SELECTED_COLOR);
            else if (hover) g.fill(MARGIN, y, listRight, y + rowH, HOVER_COLOR);

            int textColor = zone.enabled() ? 0xFFFFFFFF : 0xFF888888;
            g.drawString(font, zone.id(), MARGIN + 3, y + 3, textColor, false);
            y += rowH;
        }

        // Right panel labels
        int rightX = MARGIN + LIST_WIDTH + MARGIN;
        g.drawString(font, "ID:", rightX, 38, 0xFFAAAAAA, false);
        g.drawString(font, "X:", rightX, 58, 0xFFAAAAAA, false);
        g.drawString(font, "Y:", rightX + 60, 58, 0xFFAAAAAA, false);
        g.drawString(font, "Z:", rightX + 120, 58, 0xFFAAAAAA, false);
        g.drawString(font, "Size:", rightX, 78, 0xFFAAAAAA, false);
        g.drawString(font, "Rx", rightX + 55, 78, 0xFF888888, false);
        g.drawString(font, "Ry", rightX + 103, 78, 0xFF888888, false);
        g.drawString(font, "Rz", rightX + 151, 78, 0xFF888888, false);
        g.drawString(font, "Scent:", rightX, 98, 0xFFAAAAAA, false);

        // Intensity slider
        g.drawString(font, "Intensity:", rightX, sliderY, 0xFFAAAAAA, false);
        // Track background
        g.fill(sliderX, sliderY + 2, sliderX + sliderW, sliderY + 10, 0xFF333333);
        // Filled portion
        int filled = (int) (sliderW * intensityPercent / 100.0);
        g.fill(sliderX, sliderY + 2, sliderX + filled, sliderY + 10, 0xFFFF69B4);
        // Border
        drawBorder(g, sliderX, sliderY + 2, sliderX + sliderW, sliderY + 10, 0x66FFFFFF);
        // Thumb
        int thumbX = sliderX + filled - 2;
        g.fill(thumbX, sliderY, thumbX + 5, sliderY + 12, 0xFFFFFFFF);
        // Percentage text
        g.drawString(font, intensityPercent + "%", sliderX + sliderW + 5, sliderY + 1, 0xFFFFFFFF, false);

        // Cooldown label
        g.drawString(font, "Cooldown (s):", rightX, sliderY + gap(), 0xFFAAAAAA, false);

        super.render(g, mouseX, mouseY, delta);
    }

    private int gap() { return 20; }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (minecraft == null) return super.mouseClicked(event, doubleClick);
        double mouseX = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();

        // Intensity slider click
        if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= sliderY && mouseY <= sliderY + 12) {
            draggingIntensity = true;
            updateIntensityFromMouse(mouseX);
            return true;
        }

        // Zone list click
        int listRight = MARGIN + LIST_WIDTH;
        if (mouseX >= MARGIN && mouseX < listRight && mouseY >= 28 && mouseY < height - 40) {
            int index = listScroll + (int) ((mouseY - 28) / 14);
            List<ZoneClientData> zones = TutorialScentZoneNetworking.getClientZones();
            if (index >= 0 && index < zones.size()) {
                selectedZoneId = zones.get(index).id();
                isCreating = false;
                loadZoneToFields(selectedZoneId);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        if (draggingIntensity && minecraft != null) {
            double mouseX = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
            updateIntensityFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (draggingIntensity) {
            draggingIntensity = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void updateIntensityFromMouse(double mouseX) {
        double ratio = (mouseX - sliderX) / sliderW;
        intensityPercent = (int) Math.round(Math.max(0, Math.min(100, ratio * 100)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listRight = MARGIN + LIST_WIDTH;
        if (mouseX >= MARGIN && mouseX < listRight) {
            listScroll = Math.max(0, listScroll - (int) scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        TutorialScentZoneNetworking.setShowZoneOverlays(showOverlaysCheck.selected());

        // Live preview: update the client zone cache with current field values
        // so the zone outline updates in real-time as you type
        if (selectedZoneId != null || isCreating) {
            updateLivePreview();
        }
    }

    private void updateLivePreview() {
        String id = idField.getValue().trim();
        if (id.isEmpty()) return;

        try {
            int px = Integer.parseInt(xField.getValue().trim());
            int py = Integer.parseInt(yField.getValue().trim());
            int pz = Integer.parseInt(zField.getValue().trim());
            int rx = Integer.parseInt(rxField.getValue().trim());
            int ry = Integer.parseInt(ryField.getValue().trim());
            int rz = Integer.parseInt(rzField.getValue().trim());

            ZoneClientData preview = new ZoneClientData(
                    id, px, py, pz, rx, ry, rz,
                    SCENTS[selectedScentIndex],
                    intensityPercent / 100.0,
                    0, false, enabledCheck.selected()
            );

            // Replace or add in client cache
            List<ZoneClientData> zones = TutorialScentZoneNetworking.getClientZones();
            boolean found = false;
            for (int i = 0; i < zones.size(); i++) {
                if (zones.get(i).id().equals(id)) {
                    zones.set(i, preview);
                    found = true;
                    break;
                }
            }
            if (!found && isCreating) {
                zones.add(preview);
            }
        } catch (NumberFormatException ignored) {
            // Fields not yet valid, skip preview
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void loadZoneToFields(String zoneId) {
        for (ZoneClientData zone : TutorialScentZoneNetworking.getClientZones()) {
            if (zone.id().equals(zoneId)) {
                idField.setValue(zone.id());
                idField.setEditable(true);
                xField.setValue(String.valueOf(zone.x()));
                yField.setValue(String.valueOf(zone.y()));
                zField.setValue(String.valueOf(zone.z()));
                rxField.setValue(String.valueOf(zone.radiusX()));
                ryField.setValue(String.valueOf(zone.radiusY()));
                rzField.setValue(String.valueOf(zone.radiusZ()));
                cooldownField.setValue(String.valueOf(zone.cooldownSeconds()));

                // Set scent selector
                for (int i = 0; i < SCENTS.length; i++) {
                    if (SCENTS[i].equalsIgnoreCase(zone.scentName())) {
                        selectedScentIndex = i;
                        scentButton.setMessage(Component.literal(SCENTS[i]));
                        break;
                    }
                }

                // Set intensity slider
                intensityPercent = (int) Math.round(zone.intensity() * 100);

                // Rebuild checkboxes with correct state
                rebuildCheckboxes(zone.oneShot(), zone.enabled());
                return;
            }
        }
    }

    private void rebuildCheckboxes(boolean oneShot, boolean enabled) {
        removeWidget(oneShotCheck);
        removeWidget(enabledCheck);

        int rightX = MARGIN + LIST_WIDTH + MARGIN;
        // Calculate Y positions to match init layout
        int baseY = 35 + 20 * 6 + 4; // after cooldown field

        oneShotCheck = Checkbox.builder(Component.literal("One-shot (only once per session)"), font)
                .pos(rightX, baseY).selected(oneShot).build();
        addRenderableWidget(oneShotCheck);

        enabledCheck = Checkbox.builder(Component.literal("Enabled"), font)
                .pos(rightX, baseY + 20).selected(enabled).build();
        addRenderableWidget(enabledCheck);
    }

    private void onNew() {
        isCreating = true;
        selectedZoneId = null;
        idField.setValue("");
        idField.setEditable(true);
        if (minecraft != null && minecraft.player != null) {
            xField.setValue(String.valueOf(minecraft.player.blockPosition().getX()));
            yField.setValue(String.valueOf(minecraft.player.blockPosition().getY()));
            zField.setValue(String.valueOf(minecraft.player.blockPosition().getZ()));
        } else {
            xField.setValue("0"); yField.setValue("0"); zField.setValue("0");
        }
        rxField.setValue("5"); ryField.setValue("5"); rzField.setValue("5");
        selectedScentIndex = 14; // Kindred
        scentButton.setMessage(Component.literal(SCENTS[selectedScentIndex]));
        intensityPercent = 50;
        cooldownField.setValue("5");
        rebuildCheckboxes(false, true);
    }

    private void onSave() {
        if (minecraft == null || minecraft.level == null) return;

        String id = idField.getValue().trim();
        if (id.isEmpty()) return;

        ZoneClientData data;
        try {
            data = new ZoneClientData(
                    id,
                    Integer.parseInt(xField.getValue().trim()),
                    Integer.parseInt(yField.getValue().trim()),
                    Integer.parseInt(zField.getValue().trim()),
                    Integer.parseInt(rxField.getValue().trim()),
                    Integer.parseInt(ryField.getValue().trim()),
                    Integer.parseInt(rzField.getValue().trim()),
                    SCENTS[selectedScentIndex],
                    intensityPercent / 100.0,
                    Integer.parseInt(cooldownField.getValue().trim()),
                    oneShotCheck.selected(),
                    enabledCheck.selected()
            );
        } catch (NumberFormatException e) {
            return;
        }

        if (isCreating) {
            TutorialScentZoneNetworking.sendCreateZone(minecraft.level.registryAccess(), data);
            isCreating = false;
            selectedZoneId = id;
        } else if (selectedZoneId != null && !selectedZoneId.equals(id)) {
            // ID was renamed — delete old, create new
            TutorialScentZoneNetworking.sendDeleteZone(minecraft.level.registryAccess(), selectedZoneId);
            TutorialScentZoneNetworking.sendCreateZone(minecraft.level.registryAccess(), data);
            selectedZoneId = id;
        } else {
            TutorialScentZoneNetworking.sendUpdateZone(minecraft.level.registryAccess(), data);
        }
    }

    private void onClone() {
        if (selectedZoneId == null || minecraft == null || minecraft.player == null) return;

        // Keep all current field values but set new ID and player's position
        isCreating = true;
        selectedZoneId = null;
        idField.setValue(idField.getValue().trim() + "_copy");
        idField.setEditable(true);
        xField.setValue(String.valueOf(minecraft.player.blockPosition().getX()));
        yField.setValue(String.valueOf(minecraft.player.blockPosition().getY()));
        zField.setValue(String.valueOf(minecraft.player.blockPosition().getZ()));
        // radius, scent, intensity, cooldown, checkboxes all stay as-is
    }

    private void onDelete() {
        if (selectedZoneId == null || minecraft == null || minecraft.level == null) return;
        TutorialScentZoneNetworking.sendDeleteZone(minecraft.level.registryAccess(), selectedZoneId);
        selectedZoneId = null;
        isCreating = false;
    }

    private static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }
}
