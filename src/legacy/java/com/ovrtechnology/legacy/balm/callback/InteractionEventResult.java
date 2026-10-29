package com.ovrtechnology.legacy.balm.callback;

import java.util.Optional;
import net.minecraft.world.InteractionResult;

public interface InteractionEventResult {

    Optional<InteractionResult> interactionResult();

    InteractionEventResult DEFAULT = Optional::empty;
    InteractionEventResult SUCCESS = () -> Optional.of(InteractionResult.SUCCESS);
    InteractionEventResult SUCCESS_SERVER = () -> Optional.of(InteractionResult.SUCCESS);
    InteractionEventResult CONSUME = () -> Optional.of(InteractionResult.CONSUME);
    InteractionEventResult FAIL = () -> Optional.of(InteractionResult.FAIL);
    InteractionEventResult PASS = () -> Optional.of(InteractionResult.PASS);
    InteractionEventResult TRY_WITH_EMPTY_HAND = () -> Optional.of(InteractionResult.PASS);
}
