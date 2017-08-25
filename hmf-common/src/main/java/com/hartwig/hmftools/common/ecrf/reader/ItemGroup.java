package com.hartwig.hmftools.common.ecrf.reader;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(of = "new",
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class ItemGroup implements OIDObject {

    @Override
    @Value.Parameter
    @NotNull
    public abstract String oid();

    @Override
    @Value.Parameter
    @NotNull
    public abstract String name();

    @Value.Parameter
    @NotNull
    public abstract List<String> itemOIDs();
}
