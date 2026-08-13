package com.ovrtechnology.definition.trigger;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.util.SoundRef;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public abstract class DefinitionTrigger {

    /**
     * The ID (e.g., "minecraft:campfire").
     */
    @SerializedName("id")
    protected String id;

    /**
     * The exact OVR scent name to trigger.
     */
    @SerializedName("scent_name")
    protected String scentName;

    /**
     *
     */
    @SerializedName("perception")
    protected String perception;

    /**
     *
     */
    @SerializedName("sound")
    protected SoundRef sound;

    /**
     * Default range for proximity triggers (in blocks).
     */
    @SerializedName("range")
    protected int range = getDefaultRange();

    /**
     * Trigger mode: "INTERACT" (on right-click) or "PROXIMITY" (when near).
     */
    @SerializedName("trigger_on")
    protected String triggerOn = getDefaultTrigger();

    /**
     * Trigger mode: "ENTER" (once on entry) or "AMBIENT" (continuous).
     */
    @SerializedName("mode")
    protected String mode = getDefaultMode();

    /**
     * Priority level for this trigger.
     */
    @SerializedName("priority")
    protected ScentPriority priority = getDefaultPriority();

    /**
     * Scent intensity (0.0 to 1.0).
     * If not specified, uses the global block_intensity from settings.
     */
    @SerializedName("intensity")
    protected Double intensity;

    /**
     * Optional comment for documentation in JSON.
     */
    @SerializedName("_comment")
    protected String comment;

    /**
     * Default constructor for GSON.
     */
    public DefinitionTrigger() {
    }

    /**
     * Checks if this is a proximity-based trigger.
     *
     * @return true if trigger_on is PROXIMITY
     */
    public boolean isProximityTrigger() {
        return "PROXIMITY".equalsIgnoreCase(triggerOn);
    }

    /**
     * Checks if this is an interaction-based trigger.
     *
     * @return true if trigger_on is INTERACT
     */
    public boolean isInteractTrigger() {
        return "INTERACT".equalsIgnoreCase(triggerOn);
    }

    /**
     * Gets the intensity, falling back to global setting if not specified.
     *
     * @param globalIntensity the global default intensity from TriggerSettings
     * @return intensity value (0.0 to 1.0)
     */
    public double getIntensityOrDefault(double globalIntensity) {
        return intensity != null ? intensity : globalIntensity;
    }

    /**
     * Checks if this is an ambient (continuous) trigger.
     *
     * @return true if mode is AMBIENT
     */
    public boolean isAmbient() {
        return "AMBIENT".equalsIgnoreCase(mode);
    }

    /**
     * Checks if this is an entry trigger (one-time on biome change).
     *
     * @return true if mode is ENTER
     */
    public boolean isEnterTrigger() {
        return "ENTER".equalsIgnoreCase(mode);
    }

    /**
     * Checks if this trigger is for item use (right-click).
     *
     * @return true if trigger_on is "USE"
     */
    public boolean isUseTriggered() {
        return "USE".equalsIgnoreCase(triggerOn);
    }

    /**
     * Validates the definition has required fields.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return id != null && !id.isEmpty()
                && scentName != null && !scentName.isEmpty();
    }

    protected int getDefaultRange() {
        return 0;
    }

    protected String getDefaultTrigger() {
        return "PROXIMITY";
    }

    protected String getDefaultMode() {
        return "AMBIENT";
    }

    protected ScentPriority getDefaultPriority() {
        return ScentPriority.MEDIUM;
    }
}
