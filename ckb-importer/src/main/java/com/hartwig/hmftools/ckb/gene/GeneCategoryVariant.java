package com.hartwig.hmftools.ckb.gene;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class GeneCategoryVariant {

    @NotNull
    public abstract String id();

    @NotNull
    public abstract String fullName();

    @Nullable
    public abstract String impact();

    @Nullable
    public abstract String proteinEffect();

    @NotNull
    public abstract List<GeneVariantDescription> geneVariantDescription();
}
