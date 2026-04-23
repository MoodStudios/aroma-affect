package com.ovrtechnology.lookup.worker;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class StructureSearchWorkerManager {

    private static final StructureSearchWorkerManager INSTANCE = new StructureSearchWorkerManager();

    private static final long TARGET_TICK_MS = 50;

    private static final long MIN_WORK_TIME_MS = 10;

    private static final long MAX_WORK_TIME_MS = 30;

    private final List<SearchWorker> workers = new ArrayList<>();
    private long tickStartTime = -1;
    private boolean initialized = false;

    private StructureSearchWorkerManager() {}

    public static StructureSearchWorkerManager getInstance() {
        return INSTANCE;
    }

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
        tickStartTime = System.currentTimeMillis();
    }

    private synchronized void onServerTickPost(MinecraftServer server) {
        if (workers.isEmpty()) {
            return;
        }

        long elapsed = System.currentTimeMillis() - tickStartTime;
        long available = TARGET_TICK_MS - elapsed;

        long workTime = Math.max(MIN_WORK_TIME_MS, Math.min(available, MAX_WORK_TIME_MS));
        long deadline = System.currentTimeMillis() + workTime;

        int workDone = 0;

        int index = 0;
        while (System.currentTimeMillis() < deadline && index < workers.size()) {
            SearchWorker worker = workers.get(index);

            if (!worker.hasWork()) {
                workers.remove(index);
                continue;
            }

            boolean continueNow = worker.doWork();
            workDone++;

            if (!worker.hasWork()) {
                workers.remove(index);
                continue;
            }

            if (!continueNow) {
                index++;
            }
        }

        if (workDone > 0 && server.getTickCount() % 20 == 0) {
            AromaAffect.LOGGER.debug(
                    "Worker tick: {} workers active, {} work units this tick",
                    workers.size(),
                    workDone);
        }
    }

    public synchronized void addWorker(SearchWorker worker) {
        workers.add(worker);
        AromaAffect.LOGGER.debug("Added search worker: {}", worker.getId());
    }

    public synchronized void clear() {
        for (SearchWorker worker : workers) {
            worker.cancel();
        }
        workers.clear();
        AromaAffect.LOGGER.debug("Cleared all search workers");
    }
}
