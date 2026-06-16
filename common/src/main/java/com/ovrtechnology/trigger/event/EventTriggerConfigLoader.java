package com.ovrtechnology.trigger.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.AromaAffect;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the event/action scent hook definitions from
 * {@code data/aromaaffect/scents/event_triggers.json} and exposes them by id.
 */
public final class EventTriggerConfigLoader {

    private static final String PATH = "data/aromaaffect/scents/event_triggers.json";

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final Map<String, EventTriggerDefinition> triggers = new LinkedHashMap<>();

    private static boolean initialized = false;

    private EventTriggerConfigLoader() {
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("EventTriggerConfigLoader.init() called multiple times!");
            return;
        }
        initialized = true;

        triggers.clear();
        try (InputStream is = EventTriggerConfigLoader.class.getClassLoader().getResourceAsStream(PATH)) {
            if (is == null) {
                AromaAffect.LOGGER.warn("Event triggers file not found: {}", PATH);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Root root = GSON.fromJson(reader, Root.class);
                if (root != null && root.eventTriggers != null) {
                    for (EventTriggerDefinition def : root.eventTriggers) {
                        if (def != null && def.isValid()) {
                            triggers.put(def.getId(), def);
                        } else {
                            AromaAffect.LOGGER.warn("Skipping invalid event trigger definition");
                        }
                    }
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error loading event triggers: {}", e.getMessage());
        }

        AromaAffect.LOGGER.info("Loaded {} event scent triggers", triggers.size());
    }

    public static Optional<EventTriggerDefinition> get(String id) {
        return Optional.ofNullable(triggers.get(id));
    }

    public static List<EventTriggerDefinition> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(triggers.values()));
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private static final class Root {
        @SerializedName("event_triggers")
        List<EventTriggerDefinition> eventTriggers;
    }
}
