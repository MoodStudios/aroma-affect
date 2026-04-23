package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.TriggerSettings;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import com.ovrtechnology.websocket.WebSocketMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;

public final class ScentTriggerManager {

    private static final ScentTriggerManager INSTANCE = new ScentTriggerManager();

    @Getter private ScentTrigger activeScent = null;

    @Getter private ScentTrigger lastTriggeredScent = null;

    private int remainingTicks = 0;

    private final Map<String, Long> lastTriggerTime = new HashMap<>();

    private long lastGlobalTriggerTime = 0;

    @Getter private boolean initialized = false;

    private ScentTriggerManager() {}

    public static ScentTriggerManager getInstance() {
        return INSTANCE;
    }

    public static void init() {
        if (INSTANCE.initialized) {
            AromaAffect.LOGGER.warn("ScentTriggerManager.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing ScentTriggerManager...");
        INSTANCE.initialized = true;
        AromaAffect.LOGGER.info("ScentTriggerManager initialized");
    }

    public boolean canTrigger(String scentName) {
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        long globalCooldownMs = ClientConfig.getInstance().getGlobalCooldownMs();
        long now = System.currentTimeMillis();

        if (now - lastGlobalTriggerTime < globalCooldownMs) {
            AromaAffect.LOGGER.debug("Scent '{}' blocked by global cooldown", scentName);
            return false;
        }

        Long lastTime = lastTriggerTime.get(scentName);
        if (lastTime != null) {
            long scentCooldown = settings.getScentCooldownMs();
            if (now - lastTime < scentCooldown) {
                AromaAffect.LOGGER.debug(
                        "Scent '{}' on cooldown for {} more ms",
                        scentName,
                        scentCooldown - (now - lastTime));
                return false;
            }
        }

        return true;
    }

    public boolean canTrigger(String scentName, long cooldownMs) {
        long globalCooldownMs = ClientConfig.getInstance().getGlobalCooldownMs();
        long now = System.currentTimeMillis();

        if (now - lastGlobalTriggerTime < globalCooldownMs) {
            return false;
        }

        Long lastTime = lastTriggerTime.get(scentName);
        if (lastTime != null && now - lastTime < cooldownMs) {
            return false;
        }

        return true;
    }

    public long getRemainingCooldown(String scentName) {
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        long globalCooldownMs = ClientConfig.getInstance().getGlobalCooldownMs();
        long now = System.currentTimeMillis();

        long globalRemaining = globalCooldownMs - (now - lastGlobalTriggerTime);
        if (globalRemaining > 0) {
            return globalRemaining;
        }

        Long lastTime = lastTriggerTime.get(scentName);
        if (lastTime != null) {
            long scentRemaining = settings.getScentCooldownMs() - (now - lastTime);
            if (scentRemaining > 0) {
                return scentRemaining;
            }
        }

        return 0;
    }

    public boolean trigger(ScentTrigger trigger) {
        if (trigger == null) {
            return false;
        }

        AromaAffect.LOGGER.debug("Processing trigger: {}", trigger);

        if (!trigger.shouldReplace(activeScent)) {
            AromaAffect.LOGGER.debug(
                    "Trigger '{}' blocked by higher priority active scent '{}'",
                    trigger.scentName(),
                    activeScent != null ? activeScent.scentName() : "none");
            return false;
        }

        activeScent = trigger;
        lastTriggeredScent = trigger;
        remainingTicks = trigger.durationTicks();

        updateCooldowns(trigger.scentName());

        sendPlayToOvr(trigger.scentName(), trigger.intensity());

        AromaAffect.LOGGER.info(
                "Triggered scent '{}' (priority: {}, duration: {} ticks, intensity: {})",
                trigger.scentName(),
                trigger.priority(),
                trigger.durationTicks(),
                trigger.intensity());

        return true;
    }

    public void stop() {
        if (activeScent != null) {
            AromaAffect.LOGGER.info("Stopped tracking scent '{}'", activeScent.scentName());
            activeScent = null;
            remainingTicks = 0;
        }
    }

    public void stop(String scentName) {
        if (activeScent != null && activeScent.scentName().equals(scentName)) {
            stop();
        }
    }

    public void tick() {
        if (activeScent == null) {
            return;
        }

        if (activeScent.isIndefinite()) {
            return;
        }

        remainingTicks--;

        if (remainingTicks <= 0) {
            AromaAffect.LOGGER.debug("Scent '{}' duration expired", activeScent.scentName());
            stop();
        }
    }

    private void sendPlayToOvr(String scentName, double intensity) {
        OvrWebSocketClient client = OvrWebSocketClient.getInstance();
        if (client.isConnected()) {
            WebSocketMessage message = WebSocketMessage.playScent(scentName, intensity);
            boolean sent = client.send(message);
            AromaAffect.LOGGER.debug(
                    "Sent play scent '{}' (intensity: {}) to OVR: {}", scentName, intensity, sent);
        } else {
            AromaAffect.LOGGER.debug("OVR not connected, skipping play scent '{}'", scentName);
        }
    }

    private void updateCooldowns(String scentName) {
        long now = System.currentTimeMillis();
        lastGlobalTriggerTime = now;
        lastTriggerTime.put(scentName, now);
    }

    public Optional<ScentTrigger> getActiveScentOptional() {
        return Optional.ofNullable(activeScent);
    }

    public boolean hasActiveScent() {
        return activeScent != null;
    }

    public long getLastGlobalTriggerTime() {
        return lastGlobalTriggerTime;
    }

    public long getLastTriggerTime(String scentName) {
        return lastTriggerTime.getOrDefault(scentName, 0L);
    }

    public void resetCooldowns() {
        lastGlobalTriggerTime = 0;
        lastTriggerTime.clear();
        lastTriggeredScent = null;
        AromaAffect.LOGGER.info("All cooldowns reset");
    }

    public boolean hasActiveCooldown() {
        long globalCooldownMs = ClientConfig.getInstance().getGlobalCooldownMs();
        long now = System.currentTimeMillis();

        if (now - lastGlobalTriggerTime < globalCooldownMs) {
            return true;
        }

        if (lastTriggeredScent != null) {
            long cooldownMs = getEffectiveScentCooldownMs(lastTriggeredScent);
            Long lastTime = lastTriggerTime.get(lastTriggeredScent.scentName());
            if (lastTime != null && now - lastTime < cooldownMs) {
                return true;
            }
        }

        return false;
    }

    public long getEffectiveScentCooldownMs(ScentTrigger trigger) {
        if (trigger == null) {
            return 5000;
        }

        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        long cooldownMs;

        if (trigger.source() == ScentTriggerSource.PASSIVE_MODE) {
            cooldownMs = PassiveModeManager.getCurrentCooldownMs();
            if (cooldownMs <= 0) {
                cooldownMs = settings.getBiomeCooldownMs();
            }
        } else if (trigger.source() == ScentTriggerSource.ITEM_USE) {
            cooldownMs = settings.getItemUseCooldownMs();
        } else {
            cooldownMs = settings.getScentCooldownMs();
        }

        if (cooldownMs <= 0) {
            cooldownMs = 5000;
        }

        return cooldownMs;
    }
}
