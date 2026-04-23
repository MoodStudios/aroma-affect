package com.ovrtechnology.lookup.worker;

public interface SearchWorker {

    boolean hasWork();

    boolean doWork();

    void cancel();

    String getId();
}
