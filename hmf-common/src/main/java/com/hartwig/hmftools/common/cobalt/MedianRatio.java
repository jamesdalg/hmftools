package com.hartwig.hmftools.common.cobalt;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface MedianRatio
{
    @NotNull
    String chromosome();

    double medianRatio();

    int count();
}
