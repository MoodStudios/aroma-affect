package com.ovrtechnology.guide;

import com.ovrtechnology.util.Texts;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A category (section) in the guide table of contents.
 * Categories group related pages and can be expanded/collapsed in the sidebar.
 */
public final class GuideCategory {

    private final String id;
    private final Component title;
    @Nullable
    private final GuideIcon icon;
    private final int accentColor;
    private final List<GuidePage> pages;

    private GuideCategory(String id, Component title, @Nullable GuideIcon icon,
                          int accentColor, List<GuidePage> pages) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.accentColor = accentColor;
        this.pages = Collections.unmodifiableList(pages);
    }

    public static Builder builder(String id, String title) {
        return new Builder(id, Texts.lit(title));
    }

    public static Builder builder(String id, Component title) {
        return new Builder(id, title);
    }

    public String getId() {
        return id;
    }

    public Component getTitle() {
        return title;
    }

    @Nullable
    public GuideIcon getIcon() {
        return icon;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public List<GuidePage> getPages() {
        return pages;
    }

    public static final class Builder {
        private final String id;
        private final Component title;
        @Nullable
        private GuideIcon icon;
        private int accentColor = 0xFF6D5EF8;
        private final List<GuidePage> pages = new ArrayList<>();

        private Builder(String id, Component title) {
            this.id = id;
            this.title = title;
        }

        public Builder icon(GuideIcon icon) {
            this.icon = icon;
            return this;
        }

        public Builder accentColor(int color) {
            this.accentColor = color;
            return this;
        }

        public Builder page(GuidePage page) {
            this.pages.add(page);
            return this;
        }

        public GuideCategory build() {
            return new GuideCategory(id, title, icon, accentColor, new ArrayList<>(pages));
        }
    }
}
