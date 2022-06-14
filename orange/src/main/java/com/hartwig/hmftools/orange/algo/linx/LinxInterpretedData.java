package com.hartwig.hmftools.orange.algo.linx;

import java.util.List;

import com.hartwig.hmftools.common.linx.ReportableGeneDisruption;
import com.hartwig.hmftools.common.linx.ReportableHomozygousDisruption;
import com.hartwig.hmftools.common.sv.linx.LinxDriver;
import com.hartwig.hmftools.common.sv.linx.LinxFusion;
import com.hartwig.hmftools.common.sv.linx.LinxGermlineSv;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class LinxInterpretedData {

    @NotNull
    public abstract List<LinxFusion> allFusions();

    @NotNull
    public abstract List<LinxFusion> reportableFusions();

    @NotNull
    public abstract List<LinxFusion> potentiallyInterestingFusions();

    @NotNull
    public abstract List<ReportableGeneDisruption> geneDisruptions();

    @NotNull
    public abstract List<ReportableHomozygousDisruption> homozygousDisruptions();

    @NotNull
    public abstract List<LinxDriver> drivers();

    @NotNull
    public abstract List<LinxGermlineSv> allGermlineDisruptions();

    @NotNull
    public abstract List<LinxGermlineSv> reportableGermlineDisruptions();
}
