package com.ovrtechnology.lookup.worker;

/**
 * Interface for incremental search workers.
 * <p>
 * Workers perform searches incrementally across multiple server ticks,
 * preventing the game from freezing during expensive operations like
 * structure lookups.
 * <p>
 * Inspired by Explorer's Compass's approach.
 */
public interface SearchWorker {
    
    /**
     * Checks if this worker has more work to do.
     * 
     * @return true if there is more work, false if finished
     */
    boolean hasWork();
    
    /**
     * Performs one unit of work.
     * <p>
     * This method should do a small, bounded amount of work (e.g., check one chunk)
     * and return quickly. The worker manager will call this repeatedly while
     * there is time remaining in the tick.
     * 
     * @return true to continue working in this tick, false to wait for next tick
     */
    boolean doWork();
    
    /**
     * Called when the search is cancelled externally.
     */
    void cancel();
    
    /**
     * Gets a unique identifier for this worker (for logging).
     */
    String getId();
}
