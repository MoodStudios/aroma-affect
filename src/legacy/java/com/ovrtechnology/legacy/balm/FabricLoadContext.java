package com.ovrtechnology.legacy.balm;

import net.blay09.mods.balm.api.BalmRuntimeLoadContext;
import net.blay09.mods.balm.api.EmptyLoadContext;

public final class FabricLoadContext {

    public static final BalmRuntimeLoadContext INSTANCE = EmptyLoadContext.INSTANCE;

    private FabricLoadContext() {}
}
