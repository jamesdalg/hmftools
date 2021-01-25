package com.hartwig.hmftools.ckb.reference;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class ReferenceIndication {

    @NotNull
    public abstract String id();

    @NotNull
    public abstract String name();

    @NotNull
    public abstract String source();
}
