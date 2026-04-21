package com.ovrtechnology.entity.sniffer.config;

public class SnifferConfig {

    public TamingConfig taming = new TamingConfig();
    public RidingConfig riding = new RidingConfig();
    public SwimmingConfig swimming = new SwimmingConfig();
    public WaterFloatingConfig waterFloating = new WaterFloatingConfig();
    public DiggingConfig digging = new DiggingConfig();
    public DimensionalScentsConfig dimensionalScents = new DimensionalScentsConfig();

    public static class TamingConfig {
        public int torchflowersNeeded = 4;
    }

    public static class RidingConfig {
        public float landSpeedMultiplier = 0.8f;
        public double mountedStepHeight = 1.0;
        public double normalStepHeight = 0.6;
        public double waterExitStepHeight = 1.5;
    }

    public static class SwimmingConfig {
        public float slowSpeed = 0.03f;
        public float swimSpeed = 0.28f;
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
    }

    public static class DimensionalScentsConfig {
        public String overworld = "aromaaffect:overworld_scent";
        public String nether = "aromaaffect:nether_scent";
        public String end = "aromaaffect:end_scent";
    }
}
