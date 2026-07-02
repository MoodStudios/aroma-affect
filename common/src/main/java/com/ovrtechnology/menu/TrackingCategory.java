package com.ovrtechnology.menu;

import com.ovrtechnology.lookup.LookupType;
import com.ovrtechnology.nose.NoseAbilityResolver.ResolvedAbilities;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TrackingCategory {

    private final String id;
    private final LookupType lookupType;
    private final String pathCommandType;
    private final Supplier<ItemStack> iconItem;
    private final Identifier headerIcon;
    private final Identifier radialIcon;
    private final String titleKey;
    private final String descriptionKey;
    private final TrailDomain trailDomain;
    private final boolean worldOutline;
    private final Supplier<Screen> screenFactory;
    private final Function<ResolvedAbilities, Set<String>> abilityAccessor;

    private TrackingCategory(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.lookupType = Objects.requireNonNull(b.lookupType, "lookupType");
        this.pathCommandType = Objects.requireNonNull(b.pathCommandType, "pathCommandType");
        this.iconItem = Objects.requireNonNull(b.iconItem, "iconItem");
        this.headerIcon = Objects.requireNonNull(b.headerIcon, "headerIcon");
        this.radialIcon = b.radialIcon != null ? b.radialIcon : b.headerIcon;
        this.titleKey = b.titleKey != null ? b.titleKey : "menu.aromaaffect.category." + b.id;
        this.descriptionKey = b.descriptionKey != null ? b.descriptionKey : this.titleKey + ".description";
        this.trailDomain = Objects.requireNonNull(b.trailDomain, "trailDomain");
        this.worldOutline = b.worldOutline;
        this.screenFactory = Objects.requireNonNull(b.screenFactory, "screenFactory");
        this.abilityAccessor = Objects.requireNonNull(b.abilityAccessor, "abilityAccessor");
    }

    public String getId() {
        return id;
    }

    public LookupType getLookupType() {
        return lookupType;
    }

    public String getPathCommandType() {
        return pathCommandType;
    }

    public ItemStack getIconItem() {
        return iconItem.get().copy();
    }

    public Identifier getHeaderIcon() {
        return headerIcon;
    }

    public Identifier getRadialIcon() {
        return radialIcon;
    }

    public Component getDisplayName() {
        return Component.translatable(titleKey);
    }

    public Component getDescription() {
        return Component.translatable(descriptionKey);
    }

    public TrailDomain getTrailDomain() {
        return trailDomain;
    }

    public boolean hasWorldOutline() {
        return worldOutline;
    }

    public Screen createScreen() {
        return screenFactory.get();
    }

    public Set<String> detectableFor(ResolvedAbilities abilities) {
        return abilityAccessor.apply(abilities);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private LookupType lookupType;
        private String pathCommandType;
        private Supplier<ItemStack> iconItem;
        private Identifier headerIcon;
        private Identifier radialIcon;
        private String titleKey;
        private String descriptionKey;
        private TrailDomain trailDomain = TrailDomain.BLOCK;
        private boolean worldOutline = true;
        private Supplier<Screen> screenFactory;
        private Function<ResolvedAbilities, Set<String>> abilityAccessor;

        private Builder(String id) {
            this.id = id;
        }

        public Builder lookupType(LookupType lookupType) {
            this.lookupType = lookupType;
            return this;
        }

        public Builder pathCommandType(String pathCommandType) {
            this.pathCommandType = pathCommandType;
            return this;
        }

        public Builder iconItem(Supplier<ItemStack> iconItem) {
            this.iconItem = iconItem;
            return this;
        }

        public Builder headerIcon(Identifier headerIcon) {
            this.headerIcon = headerIcon;
            return this;
        }

        public Builder radialIcon(Identifier radialIcon) {
            this.radialIcon = radialIcon;
            return this;
        }

        public Builder titleKey(String titleKey) {
            this.titleKey = titleKey;
            return this;
        }

        public Builder descriptionKey(String descriptionKey) {
            this.descriptionKey = descriptionKey;
            return this;
        }

        public Builder trailDomain(TrailDomain trailDomain) {
            this.trailDomain = trailDomain;
            return this;
        }

        public Builder worldOutline(boolean worldOutline) {
            this.worldOutline = worldOutline;
            return this;
        }

        public Builder screenFactory(Supplier<Screen> screenFactory) {
            this.screenFactory = screenFactory;
            return this;
        }

        public Builder abilityAccessor(Function<ResolvedAbilities, Set<String>> abilityAccessor) {
            this.abilityAccessor = abilityAccessor;
            return this;
        }

        public TrackingCategory build() {
            return new TrackingCategory(this);
        }
    }
}
