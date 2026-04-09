package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.AromaAffect;

/**
 * Dumps all thread stack traces 5 seconds after being triggered.
 * This will show EXACTLY where the hang is occurring.
 */
public final class HangDebugger {

    private HangDebugger() {}

    /**
     * Starts a background thread that will dump all thread stacks after a delay.
     * Call this right before disconnect().
     */
    public static void scheduleThreadDump() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds for the hang to manifest
            } catch (InterruptedException ignored) {}

            AromaAffect.LOGGER.error("========== HANG DEBUGGER: THREAD DUMP ==========");
            for (var entry : Thread.getAllStackTraces().entrySet()) {
                Thread thread = entry.getKey();
                StackTraceElement[] stack = entry.getValue();

                if (thread.getName().contains("Server") || thread.getName().contains("main") || thread.getName().contains("Render")) {
                    AromaAffect.LOGGER.error("--- Thread: {} (state: {}) ---", thread.getName(), thread.getState());
                    for (StackTraceElement element : stack) {
                        AromaAffect.LOGGER.error("    at {}", element);
                    }
                }
            }
            AromaAffect.LOGGER.error("========== END THREAD DUMP ==========");
        }, "HangDebugger").start();
    }
}
