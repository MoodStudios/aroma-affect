package com.ovrtechnology.entity.sniffer.config;

import java.util.List;

/**
 * Configuration class for all sniffer-related settings.
 * Loaded from JSON config file.
 */
public class SnifferConfig {

    public TamingConfig taming = new TamingConfig();
    public RidingConfig riding = new RidingConfig();
    public SwimmingConfig swimming = new SwimmingConfig();
    public WaterFloatingConfig waterFloating = new WaterFloatingConfig();
    public DiggingConfig digging = new DiggingConfig();
    public BonanzaConfig bonanza = new BonanzaConfig();
    public DimensionalScentsConfig dimensionalScents = new DimensionalScentsConfig();

    public static class TamingConfig {
        public int torchflowersNeeded = 4;
    }

    public static class RidingConfig {
        public float landSpeedMultiplier = 0.8f;
        public float waterSpeed = 0.12f;
        public double mountedStepHeight = 1.0;
        public double normalStepHeight = 0.6;
        public double waterExitStepHeight = 1.5;
    }

    public static class SwimmingConfig {
        public int transitionTicks = 30;
        public float slowSpeed = 0.03f;
        public float swimSpeed = 0.5f;
    }

    public static class WaterFloatingConfig {
        public double floatOffset = 0.5;
        public double horizontalDrag = 0.98;
        public double maxUpwardSpeed = 0.08;
        public double maxDownwardSpeed = 0.04;
        public double buoyancyStrength = 0.1;
    }

    public static class DiggingConfig {
        public int sniffCooldownWithNose = 200;
        public int sniffCooldownDefault = 9600;
    }

    public static class BonanzaConfig {
        public boolean enabled = true;
        public List<MineralEntry> minerals = List.of(
                new MineralEntry("minecraft:raw_copper", true, 5, 15),
                new MineralEntry("minecraft:raw_iron", true, 5, 15),
                new MineralEntry("minecraft:raw_gold", true, 5, 15),
                new MineralEntry("minecraft:emerald", true, 5, 15),
                new MineralEntry("minecraft:diamond", true, 5, 15),
                new MineralEntry("minecraft:netherite_scrap", true, 5, 15)
        );
        public ScentBaseConfig scentBase = new ScentBaseConfig();
        public SeedsConfig seeds = new SeedsConfig();
    }

    public static class MineralEntry {
        public String item;
        public boolean enabled;
        public int min;
        public int max;

        public MineralEntry() {}

        public MineralEntry(String item, boolean enabled, int min, int max) {
            this.item = item;
            this.enabled = enabled;
            this.min = min;
            this.max = max;
        }
    }

    public static class ScentBaseConfig {
        public boolean enabled = true;
        public String item = "aromaaffect:scent_base";
        public int min = 1;
        public int max = 5;
    }

    public static class SeedsConfig {
        public boolean enabled = true;
        public List<String> items = List.of("minecraft:torchflower_seeds", "minecraft:pitcher_pod");
        public int min = 1;
        public int max = 2;
    }

    public static class DimensionalScentsConfig {
        public DimensionScentEntry overworld = new DimensionScentEntry(true, "aromaaffect:overworld_scent");
        public DimensionScentEntry nether = new DimensionScentEntry(true, "aromaaffect:nether_scent");
        public DimensionScentEntry end = new DimensionScentEntry(true, "aromaaffect:end_scent");
    }

    public static class DimensionScentEntry {
        public boolean enabled;
        public String item;

        public DimensionScentEntry() {}

        public DimensionScentEntry(boolean enabled, String item) {
            this.enabled = enabled;
            this.item = item;
        }
    }
}
