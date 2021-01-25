package com.hartwig.hmftools.ckb.therapy;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class TherapyReference {

    @NotNull
    public abstract String id();

    @Nullable
    public abstract String pubMedId();

    @Nullable
    public abstract String title();

    @Nullable
    public abstract String url();
}
