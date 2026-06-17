package com.ovrtechnology.trigger.event;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ResourceManagerDataSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Events-only datapack reload listener.
 *
 * <p>The event definitions live in a directory of JSON files
 * ({@code data/aromaaffect/aromaaffect_events/}) which can only be enumerated
 * through the {@link ResourceManager} (classpath listing isn't available). This
 * listener loads them on server-data reload (world load and {@code /reload}),
 * the same way the 1.21.1 branch does — but scoped to events only, so it does
 * not change how the rest of the mod's content is loaded.</p>
 */
public final class EventReloadListener extends SimplePreparableReloadListener<Void> {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "event_reload");

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        try {
            EventDefinitionLoader.loadAllEvents(new ResourceManagerDataSource(resourceManager));
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Event definition reload failed", e);
        }
    }
}
