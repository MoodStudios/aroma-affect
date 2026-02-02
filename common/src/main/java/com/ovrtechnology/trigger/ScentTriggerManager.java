package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.TriggerSettings;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import com.ovrtechnology.websocket.WebSocketMessage;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central manager for scent triggers.
 * 
 * <p>
 * This singleton handles:
 * </p>
 * <ul>
 * <li>Tracking the currently active scent</li>
 * <li>Managing cooldowns to protect hardware</li>
 * <li>Resolving priority conflicts between triggers</li>
 * <li>Sending play/stop commands to OVR hardware</li>
 * <li>Processing trigger durations each tick</li>
 * </ul>
 * 
 * <h3>Usage:</h3>
 * 
 * <pre>
 * // Check if can trigger (cooldown)
 * if (ScentTriggerManager.getInstance().canTrigger("Winter")) {
 *     ScentTrigger trigger = ScentTrigger.fromItemUse("Winter", 200);
 *     ScentTriggerManager.getInstance().trigger(trigger);
 * }
 * </pre>
 */
public final class ScentTriggerManager {

    private static final ScentTriggerManager INSTANCE = new ScentTriggerManager();

    // ========================================
    // State
    // ========================================

    /**
     * The currently active scent trigger.
     */
    @Getter
    private ScentTrigger activeScent = null;

    /**
     * Remaining ticks for the active scent.
     */
    private int remainingTicks = 0;

    // ========================================
    // Cooldowns
    // ========================================

    /**
     * Map of scent name -> last trigger timestamp (ms).
     */
    private final Map<String, Long> lastTriggerTime = new HashMap<>();

    /**
     * Timestamp of the last global trigger (any scent).
     */
    private long lastGlobalTriggerTime = 0;

    // ========================================
    // Initialization
    // ========================================

    /**
     * Whether the manager has been initialized.
     */
    @Getter
    private boolean initialized = false;

    private ScentTriggerManager() {
    }

    /**
     * Gets the singleton instance.
     * 
     * @return the manager instance
     */
    public static ScentTriggerManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the trigger manager.
     * Should be called after ScentTriggerConfigLoader.init().
     */
    public static void init() {
        if (INSTANCE.initialized) {
            AromaAffect.LOGGER.warn("ScentTriggerManager.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing ScentTriggerManager...");
        INSTANCE.initialized = true;
        AromaAffect.LOGGER.info("ScentTriggerManager initialized");
    }

    // ========================================
    // Cooldown Checking
    // ========================================

    /**
     * Checks if a scent can be triggered (not on cooldown).
     * 
     * <p>
     * This checks both the global cooldown and the per-scent cooldown.
     * </p>
     * 
     * @param scentName the scent name to check
     * @return true if the scent can be triggered
     */
    public boolean canTrigger(String scentName) {
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        long now = System.currentTimeMillis();

        // Check global cooldown
        if (now - lastGlobalTriggerTime < settings.getGlobalCooldownMs()) {
            AromaAffect.LOGGER.debug("Scent '{}' blocked by global cooldown", scentName);
            return false;
        }

        // Check per-scent cooldown
        Long lastTime = lastTriggerTime.get(scentName);
        if (lastTime != null) {
            long scentCooldown = settings.getItemUseCooldownMs(); // Default to item cooldown
            if (now - lastTime < scentCooldown) {
                AromaAffect.LOGGER.debug("Scent '{}' on cooldown for {} more ms",
                        scentName, scentCooldown - (now - lastTime));
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a specific scent can be triggered with a custom cooldown.
     * 
     * @param scentName  the scent name
     * @param cooldownMs the cooldown to check against
     * @return true if can trigger
     */
    public boolean canTrigger(String scentName, long cooldownMs) {
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        long now = System.currentTimeMillis();

        // Check global cooldown
        if (now - lastGlobalTriggerTime < settings.getGlobalCooldownMs()) {
            return false;
        }

        // Check per-scent cooldown
        Long lastTime = lastTriggerTime.get(scentName);
        if (lastTime != null && now - lastTime < cooldownMs) {
            return false;
        }

        return true;
    }

    /**
     * Gets the remaining cooldown time for a scent in milliseconds.
     * 
     * @param scentName the scent name
     * @return remaining cooldown in ms, or 0 if not on cooldown
     */
    public long getRemainingCooldown(String scentName) {
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        long now = System.currentTimeMillis();

        // Check global cooldown first
        long globalRemaining = settings.getGlobalCooldownMs() - (now - lastGlobalTriggerTime);
        if (globalRemaining > 0) {
            return globalRemaining;
        }

        // Check per-scent cooldown
        Long lastTime = lastTriggerTime.get(scentName);
        if (lastTime != null) {
            long scentRemaining = settings.getItemUseCooldownMs() - (now - lastTime);
            if (scentRemaining > 0) {
                return scentRemaining;
            }
        }

        return 0;
    }

    // ========================================
    // Triggering
    // ========================================

    /**
     * Triggers a scent.
     * 
     * <p>
     * This method handles priority resolution and sends the appropriate
     * commands to OVR hardware.
     * </p>
     * 
     * @param trigger the trigger to activate
     * @return true if the trigger was activated, false if blocked
     */
    public boolean trigger(ScentTrigger trigger) {
        if (trigger == null) {
            return false;
        }

        AromaAffect.LOGGER.debug("Processing trigger: {}", trigger);

        // Check if this trigger should replace the active one
        if (!trigger.shouldReplace(activeScent)) {
            AromaAffect.LOGGER.debug("Trigger '{}' blocked by higher priority active scent '{}'",
                    trigger.scentName(), activeScent != null ? activeScent.scentName() : "none");
            return false;
        }

        // Note: No need to send stop to previous scent - OVR client handles this automatically

        // Activate the new scent
        activeScent = trigger;
        remainingTicks = trigger.durationTicks();

        // Update cooldowns
        updateCooldowns(trigger.scentName());

        // Send to OVR with intensity
        sendPlayToOvr(trigger.scentName(), trigger.intensity());

        AromaAffect.LOGGER.info("Triggered scent '{}' (priority: {}, duration: {} ticks, intensity: {})",
                trigger.scentName(), trigger.priority(), trigger.durationTicks(), trigger.intensity());

        return true;
    }

    /**
     * Stops the currently active scent.
     * Note: OVR client handles automatic stop, this just clears local state.
     */
    public void stop() {
        if (activeScent != null) {
            AromaAffect.LOGGER.info("Stopped tracking scent '{}'", activeScent.scentName());
            activeScent = null;
            remainingTicks = 0;
        }
    }

    /**
     * Stops a specific scent if it's the active one.
     * 
     * @param scentName the scent to stop
     */
    public void stop(String scentName) {
        if (activeScent != null && activeScent.scentName().equals(scentName)) {
            stop();
        }
    }

    // ========================================
    // Tick Processing
    // ========================================

    /**
     * Processes one game tick.
     * Called every client tick to manage scent durations.
     */
    public void tick() {
        if (activeScent == null) {
            return;
        }

        // Skip indefinite scents
        if (activeScent.isIndefinite()) {
            return;
        }

        remainingTicks--;

        if (remainingTicks <= 0) {
            AromaAffect.LOGGER.debug("Scent '{}' duration expired", activeScent.scentName());
            stop();
        }
    }

    // ========================================
    // OVR Communication
    // ========================================

    /**
     * Sends a play command to OVR hardware.
     * 
     * @param scentName the scent name to play
     * @param intensity the scent intensity (0.0 to 1.0)
     */
    private void sendPlayToOvr(String scentName, double intensity) {
        OvrWebSocketClient client = OvrWebSocketClient.getInstance();
        if (client.isConnected()) {
            WebSocketMessage message = WebSocketMessage.playScent(scentName, intensity);
            boolean sent = client.send(message);
            AromaAffect.LOGGER.debug("Sent play scent '{}' (intensity: {}) to OVR: {}", scentName, intensity, sent);
        } else {
            AromaAffect.LOGGER.debug("OVR not connected, skipping play scent '{}'", scentName);
        }
    }

    // ========================================
    // Internal Helpers
    // ========================================

    /**
     * Updates cooldown timestamps after a successful trigger.
     */
    private void updateCooldowns(String scentName) {
        long now = System.currentTimeMillis();
        lastGlobalTriggerTime = now;
        lastTriggerTime.put(scentName, now);
    }

    // ========================================
    // State Accessors
    // ========================================

    /**
     * Gets the currently active scent trigger.
     * 
     * @return Optional containing the active scent, or empty if none
     */
    public Optional<ScentTrigger> getActiveScentOptional() {
        return Optional.ofNullable(activeScent);
    }

    /**
     * Checks if any scent is currently active.
     * 
     * @return true if a scent is playing
     */
    public boolean hasActiveScent() {
        return activeScent != null;
    }

    /**
     * Gets the remaining duration of the active scent in ticks.
     * 
     * @return remaining ticks, or 0 if no active scent
     */
    public int getRemainingTicks() {
        return remainingTicks;
    }

    /**
     * Gets the remaining duration as a fraction (0.0 to 1.0).
     *
     * @return progress fraction, or 0 if no active scent
     */
    public float getRemainingProgress() {
        if (activeScent == null || activeScent.isIndefinite()) {
            return 0.0f;
        }
        return (float) remainingTicks / activeScent.durationTicks();
    }

    /**
     * Gets the timestamp of the last global trigger.
     * Used by HUD to calculate cooldown progress.
     *
     * @return timestamp in milliseconds
     */
    public long getLastGlobalTriggerTime() {
        return lastGlobalTriggerTime;
    }

    /**
     * Gets the timestamp of the last trigger for a specific scent.
     *
     * @param scentName the scent name
     * @return timestamp in milliseconds, or 0 if never triggered
     */
    public long getLastTriggerTime(String scentName) {
        return lastTriggerTime.getOrDefault(scentName, 0L);
    }
}
