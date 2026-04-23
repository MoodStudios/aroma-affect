package com.ovrtechnology.ability;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class AbilityDefinition {

    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("description")
    private String description;

    @SerializedName("config")
    private Map<String, Object> config;

    public static final String TYPE_PASSIVE = "passive";
    public static final String TYPE_BLOCK_INTERACTION = "block_interaction";

    public String getType() {
        return type != null ? type : TYPE_PASSIVE;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public Map<String, Object> getConfig() {
        return config != null ? config : Collections.emptyMap();
    }

    public int getConfigInt(String key, int defaultValue) {
        Object value = getConfig().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    public double getConfigDouble(String key, double defaultValue) {
        Object value = getConfig().get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public List<String> getConfigStringList(String key) {
        Object value = getConfig().get(key);
        if (value instanceof List<?>) {
            return (List<String>) value;
        }
        return Collections.emptyList();
    }

    public boolean isValid() {
        return id != null && !id.isEmpty();
    }
}
