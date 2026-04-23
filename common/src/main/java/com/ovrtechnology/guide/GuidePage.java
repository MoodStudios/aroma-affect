package com.ovrtechnology.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@Getter
public final class GuidePage {

    private final String id;
    private final Component title;
    @Nullable private final GuideIcon icon;
    private final List<GuideElement> elements;

    private GuidePage(
            String id, Component title, @Nullable GuideIcon icon, List<GuideElement> elements) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.elements = Collections.unmodifiableList(elements);
    }

    public static Builder builder(String id, Component title) {
        return new Builder(id, title);
    }

    public static final class Builder {
        private final String id;
        private final Component title;
        @Nullable private GuideIcon icon;
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
