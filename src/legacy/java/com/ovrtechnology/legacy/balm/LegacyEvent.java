package com.ovrtechnology.legacy.balm;

import java.util.function.Consumer;

public final class LegacyEvent<T> {

    private final Consumer<T> binder;

    public LegacyEvent(Consumer<T> binder) {
        this.binder = binder;
    }

    public void register(T handler) {
        binder.accept(handler);
    }
}
