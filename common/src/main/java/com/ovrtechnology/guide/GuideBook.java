package com.ovrtechnology.guide;

import com.ovrtechnology.util.Texts;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The top-level guide book. Contains categories which contain pages.
 * This is the root object for the entire guide system.
 *
 * <p>Example usage:</p>
 * <pre>
 *   GuideBook book = GuideBook.builder("AromaCraft Guide")
 *       .category(gettingStartedCategory)
 *       .category(nosesCategory)
 *       .category(scentsCategory)
 *       .build();
 * </pre>
 */
public final class GuideBook {

    private final Component title;
    private final Component subtitle;
    private final List<GuideCategory> categories;

    private GuideBook(Component title, Component subtitle, List<GuideCategory> categories) {
        this.title = title;
        this.subtitle = subtitle;
        this.categories = Collections.unmodifiableList(categories);
    }

    public static Builder builder(String title) {
        return new Builder(Texts.lit(title));
    }

    public static Builder builder(Component title) {
        return new Builder(title);
    }

    public Component getTitle() {
        return title;
    }

    public Component getSubtitle() {
        return subtitle;
    }

    public List<GuideCategory> getCategories() {
        return categories;
    }

    @Nullable
    public GuidePage getFirstPage() {
        for (GuideCategory category : categories) {
            if (!category.getPages().isEmpty()) {
                return category.getPages().get(0);
            }
        }
        return null;
    }

    @Nullable
    public GuidePage findPageById(String pageId) {
        for (GuideCategory category : categories) {
            for (GuidePage page : category.getPages()) {
                if (page.getId().equals(pageId)) {
                    return page;
                }
            }
        }
        return null;
    }

    @Nullable
    public GuideCategory findCategoryForPage(GuidePage page) {
        for (GuideCategory category : categories) {
            if (category.getPages().contains(page)) {
                return category;
            }
        }
        return null;
    }

    public static final class Builder {
        private final Component title;
        private Component subtitle = Texts.empty();
        private final List<GuideCategory> categories = new ArrayList<>();

        private Builder(Component title) {
            this.title = title;
        }

        public Builder subtitle(String subtitle) {
            this.subtitle = Texts.lit(subtitle);
            return this;
        }

        public Builder subtitle(Component subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder category(GuideCategory category) {
            this.categories.add(category);
            return this;
        }

        public GuideBook build() {
            return new GuideBook(title, subtitle, new ArrayList<>(categories));
        }
    }
}
