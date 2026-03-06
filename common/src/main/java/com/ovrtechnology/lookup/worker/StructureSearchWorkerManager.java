package com.ovrtechnology.lookup.worker;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages incremental search workers across server ticks.
 * <p>
 * This manager executes workers during the time remaining in each server tick,
 * preventing game freezes during expensive searches like structure lookups.
 * <p>
 * The system works by:
 * <ol>
 *   <li>Recording the start time at the beginning of each tick</li>
 *   <li>At the end of each tick, calculating remaining time (target: 50ms per tick)</li>
 *   <li>Executing workers within that remaining time budget</li>
 * </ol>
 */
public final class StructureSearchWorkerManager {
    
    private static final StructureSearchWorkerManager INSTANCE = new StructureSearchWorkerManager();
    
    /**
     * Target tick duration in milliseconds.
     * A tick should be 50ms at 20 TPS.
     */
    private static final long TARGET_TICK_MS = 50;
    
    /**
     * Minimum time to allocate for worker processing.
     * Even if the tick is lagging, give workers at least this much time.
     */
    private static final long MIN_WORK_TIME_MS = 10;
    
    /**
     * Maximum time to allocate for worker processing per tick.
     * Allows workers to make reasonable progress without causing lag.
     */
    private static final long MAX_WORK_TIME_MS = 30;
    
    private final List<SearchWorker> workers = new ArrayList<>();
    private long tickStartTime = -1;
    private boolean initialized = false;
    
    private StructureSearchWorkerManager() {}
    
    public static StructureSearchWorkerManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initializes the worker manager.
     * Should be called during mod initialization.
     */
    public static void init() {
        if (INSTANCE.initialized) {
            return;
        }
        
        LifecycleEvent.SERVER_STOPPING.register(INSTANCE::onServerStopping);
        TickEvent.SERVER_PRE.register(INSTANCE::onServerTickPre);
        TickEvent.SERVER_POST.register(INSTANCE::onServerTickPost);
        
        INSTANCE.initialized = true;
        AromaAffect.LOGGER.info("Structure search worker manager initialized");
    }
    
    private void onServerStopping(MinecraftServer server) {
        clear();
    }
    
    private void onServerTickPre(MinecraftServer server) {
        if (ReplayCompat.isReplayServer(server)) return;
        tickStartTime = System.currentTimeMillis();
    }
    
    private synchronized void onServerTickPost(MinecraftServer server) {
        if (ReplayCompat.isReplayServer(server)) return;
        if (workers.isEmpty()) {
            return;
        }
        
        // Calculate how much time we have to work
        long elapsed = System.currentTimeMillis() - tickStartTime;
        long available = TARGET_TICK_MS - elapsed;
        
        // Clamp to reasonable bounds
        long workTime = Math.max(MIN_WORK_TIME_MS, Math.min(available, MAX_WORK_TIME_MS));
        long deadline = System.currentTimeMillis() + workTime;
        
        int workDone = 0;
        
        // Process workers until we run out of time
        int index = 0;
        while (System.currentTimeMillis() < deadline && index < workers.size()) {
            SearchWorker worker = workers.get(index);
            
            if (!worker.hasWork()) {
                workers.remove(index);
                continue;
            }
            
            // Do one unit of work
            boolean continueNow = worker.doWork();
            workDone++;
            
            // Check if worker finished
            if (!worker.hasWork()) {
                workers.remove(index);
                continue;
            }
            
            // If worker wants to continue and we have time, don't increment index
            // Otherwise, move to next worker
            if (!continueNow) {
                index++;
            }
        }
        
        // Log occasionally to show workers are active
        if (workDone > 0 && server.getTickCount() % 20 == 0) {  // Every second
            AromaAffect.LOGGER.debug("Worker tick: {} workers active, {} work units this tick", 
                    workers.size(), workDone);
        }
    }
    
    /**
     * Adds a worker to be processed.
     */
    public synchronized void addWorker(SearchWorker worker) {
        workers.add(worker);
        AromaAffect.LOGGER.debug("Added search worker: {}", worker.getId());
    }
    
    /**
     * Removes a worker from processing.
     */
    public synchronized void removeWorker(SearchWorker worker) {
        if (workers.remove(worker)) {
            AromaAffect.LOGGER.debug("Removed search worker: {}", worker.getId());
        }
    }
    
    /**
     * Cancels and removes all workers.
     */
    public synchronized void clear() {
        for (SearchWorker worker : workers) {
            worker.cancel();
        }
        workers.clear();
        AromaAffect.LOGGER.debug("Cleared all search workers");
    }
    
    /**
     * Gets the number of active workers.
     */
    public int getActiveWorkerCount() {
        return workers.size();
    }
}
