package com.ovrtechnology.guide;

import com.ovrtechnology.util.Texts;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single page in the guide book. Each page has a title, optional icon,
 * and a list of content elements rendered top-to-bottom.
 *
 * <p>Example usage:</p>
 * <pre>
 *   GuidePage page = GuidePage.builder("Getting Started")
 *       .icon(GuideIcon.ofItem(new ItemStack(Items.BOOK)))
 *       .element(GuideElement.header("Welcome to AromaCraft!"))
 *       .element(GuideElement.text("This mod adds scent detection..."))
 *       .element(GuideElement.separator())
 *       .element(GuideElement.tip("Equip a Nose to get started!"))
 *       .build();
 * </pre>
 */
public final class GuidePage {

    private final String id;
    private final Component title;
    @Nullable
    private final GuideIcon icon;
    private final List<GuideElement> elements;

    private GuidePage(String id, Component title, @Nullable GuideIcon icon, List<GuideElement> elements) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.elements = Collections.unmodifiableList(elements);
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

    public List<GuideElement> getElements() {
        return elements;
    }

    public static final class Builder {
        private final String id;
        private final Component title;
        @Nullable
        private GuideIcon icon;
        private final List<GuideElement> elements = new ArrayList<>();

        private Builder(String id, Component title) {
            this.id = id;
            this.title = title;
        }

        public Builder icon(GuideIcon icon) {
            this.icon = icon;
            return this;
        }

        public Builder element(GuideElement element) {
            this.elements.add(element);
            return this;
        }

        public GuidePage build() {
            return new GuidePage(id, title, icon, new ArrayList<>(elements));
        }
    }
}
