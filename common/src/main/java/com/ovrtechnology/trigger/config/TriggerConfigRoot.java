package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Root object for the scent_triggers.json configuration file.
 * 
 * <p>This class is the top-level POJO that maps to the entire JSON structure.</p>
 */
@Getter
@Setter
@ToString
public class TriggerConfigRoot {
    
    /**
     * Global settings for the trigger system.
     */
    @SerializedName("settings")
    private TriggerSettings settings = new TriggerSettings();
    
    /**
     * List of item-based triggers.
     */
    @SerializedName("item_triggers")
    private List<ItemTriggerDefinition> itemTriggers = new ArrayList<>();
    
    /**
     * List of biome-based triggers.
     * (Placeholder for future implementation)
     */
    @SerializedName("biome_triggers")
    private List<BiomeTriggerDefinition> biomeTriggers = new ArrayList<>();
    
    /**
     * List of block-based triggers.
     * (Placeholder for future implementation)
     */
    @SerializedName("block_triggers")
    private List<BlockTriggerDefinition> blockTriggers = new ArrayList<>();
    
    /**
     * List of mob/entity-based triggers.
     * (Placeholder for future implementation)
     */
    @SerializedName("mob_triggers")
    private List<MobTriggerDefinition> mobTriggers = new ArrayList<>();
    
    /**
     * List of structure-based triggers.
     */
    @SerializedName("structure_triggers")
    private List<StructureTriggerDefinition> structureTriggers = new ArrayList<>();
    
    /**
     * Default constructor for GSON.
     */
    public TriggerConfigRoot() {
    }
    
    /**
     * Validates all trigger definitions and settings.
     * Logs warnings for invalid entries.
     */
    public void validate() {
        if (settings == null) {
            settings = new TriggerSettings();
        }
        settings.validate();
        
        if (itemTriggers == null) {
            itemTriggers = new ArrayList<>();
        }
        if (biomeTriggers == null) {
            biomeTriggers = new ArrayList<>();
        }
        if (blockTriggers == null) {
            blockTriggers = new ArrayList<>();
        }
        if (mobTriggers == null) {
            mobTriggers = new ArrayList<>();
        }
        if (structureTriggers == null) {
            structureTriggers = new ArrayList<>();
        }
    }
    
    /**
     * Gets the count of all trigger definitions.
     * 
     * @return total number of triggers
     */
    public int getTotalTriggerCount() {
        return itemTriggers.size() 
            + biomeTriggers.size() 
            + blockTriggers.size() 
            + mobTriggers.size()
            + structureTriggers.size();
    }
}
