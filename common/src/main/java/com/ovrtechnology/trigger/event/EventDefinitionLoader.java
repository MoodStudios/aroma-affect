package com.ovrtechnology.trigger.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.scent.ScentRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

public final class EventDefinitionLoader {

    public static final String EVENTS_DIR = "aromaaffect_events";

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    @Getter private static List<EventDefinition> loadedEvents = new ArrayList<>();

    private static final Map<String, EventDefinition> eventsById = new HashMap<>();

    private static final Map<String, List<EventDefinition>> eventsByTriggerType = new HashMap<>();

    private static final Set<String> loadedIds = new HashSet<>();

    @Getter private static List<String> validationWarnings = new ArrayList<>();

    private EventDefinitionLoader() {}

    public static List<EventDefinition> loadAllEvents() {
        return loadAllEvents(ClasspathDataSource.INSTANCE);
    }

    public static List<EventDefinition> loadAllEvents(DataSource dataSource) {
        loadedEvents.clear();
        eventsById.clear();
        eventsByTriggerType.clear();
        loadedIds.clear();
        validationWarnings.clear();

        Map<ResourceLocation, JsonElement> files = dataSource.listJson(EVENTS_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                EventDefinition event = GSON.fromJson(entry.getValue(), EventDefinition.class);
                processEvent(event);
            } catch (Exception e) {
                AromaAffect.LOGGER.error(
                        "Failed to parse event definition {}: {}", entry.getKey(), e.getMessage());
            }
        }

        AromaAffect.LOGGER.info(
                "Loaded {} event definitions from {} file(s)", loadedEvents.size(), files.size());

        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Event loading completed with {} validation warnings",
                    validationWarnings.size());
        }

        return Collections.unmodifiableList(loadedEvents);
    }

    private static void processEvent(EventDefinition event) {
        if (event == null) {
            addWarning("Null event definition found, skipping...");
            return;
        }

        if (!event.isValid()) {
            addWarning(
                    "Invalid event definition (missing event_id / trigger_type / scent_id),"
                            + " skipping...");
            return;
        }

        String eventId = event.getEventId();

        if (loadedIds.contains(eventId)) {
            addWarning("Duplicate event_id '" + eventId + "' found, skipping...");
            return;
        }

        validateEvent(event);

        loadedIds.add(eventId);
        loadedEvents.add(event);
        eventsById.put(eventId, event);
        eventsByTriggerType
                .computeIfAbsent(event.getTriggerType(), k -> new ArrayList<>())
                .add(event);

        AromaAffect.LOGGER.debug(
                "Loaded event definition: {} (trigger: {}, scent: {})",
                eventId,
                event.getTriggerType(),
                event.getScentId());
    }

    private static void validateEvent(EventDefinition event) {
        String eventId = event.getEventId();

        if (!event.hasValidEventIdFormat()) {
            addWarning(
                    "["
                            + eventId
                            + "] Invalid event_id format - should be 'namespace:path' (e.g.,"
                            + " 'aromaaffect:player_low_health')");
        }

        if (event.getCategory() == null || event.getCategory().isEmpty()) {
            addWarning(
                    "[" + eventId + "] No category defined, will fall back to CUSTOM_EVENT source");
        } else {
            try {
                event.resolveSource();
            } catch (Exception ignored) {
                addWarning(
                        "["
                                + eventId
                                + "] Unknown category '"
                                + event.getCategory()
                                + "', will fall back to CUSTOM_EVENT");
            }
        }

        if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(event.getScentId())) {
            addWarning(
                    "["
                            + eventId
                            + "] Referenced scent_id '"
                            + event.getScentId()
                            + "' does not exist in ScentRegistry");
        }
    }

    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }

    public static Optional<EventDefinition> getById(String eventId) {
        return Optional.ofNullable(eventsById.get(eventId));
    }

    public static List<EventDefinition> getByTriggerType(String triggerType) {
        return eventsByTriggerType.getOrDefault(triggerType, Collections.emptyList());
    }

    public static Optional<EventDefinition> getFirstByTriggerType(String triggerType) {
        List<EventDefinition> list = eventsByTriggerType.get(triggerType);
        return list == null || list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public static List<EventDefinition> getByCategory(String category) {
        List<EventDefinition> result = new ArrayList<>();
        for (EventDefinition event : loadedEvents) {
            if (category.equals(event.getCategory())) {
                result.add(event);
            }
        }
        return result;
    }

    public static boolean hasValidationWarnings() {
        return !validationWarnings.isEmpty();
    }

    public static List<EventDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading event definitions...");
        return loadAllEvents();
    }
}
