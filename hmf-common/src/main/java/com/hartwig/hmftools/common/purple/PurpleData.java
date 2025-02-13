package com.hartwig.hmftools.common.purple;

import java.util.List;

import com.hartwig.hmftools.common.purple.cnchromosome.CnPerChromosomeArmData;
import com.hartwig.hmftools.common.purple.gene.GeneCopyNumber;
import com.hartwig.hmftools.common.purple.gene.GermlineDeletion;
import com.hartwig.hmftools.common.purple.interpretation.GainLoss;
import com.hartwig.hmftools.common.purple.purity.FittedPurityMethod;
import com.hartwig.hmftools.common.variant.ReportableVariant;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.msi.MicrosatelliteStatus;
import com.hartwig.hmftools.common.variant.tml.TumorMutationalStatus;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface PurpleData {

    @NotNull
    PurpleQC qc();

    boolean hasReliableQuality();

    @NotNull
    FittedPurityMethod fittedPurityMethod();

    boolean hasReliablePurity();

    double purity();

    double minPurity();

    double maxPurity();

    double ploidy();

    double minPloidy();

    double maxPloidy();

    boolean wholeGenomeDuplication();

    double microsatelliteIndelsPerMb();

    @NotNull
    MicrosatelliteStatus microsatelliteStatus();

    double tumorMutationalBurdenPerMb();

    int tumorMutationalLoad();

    @NotNull
    TumorMutationalStatus tumorMutationalLoadStatus();

    int svTumorMutationalBurden();

    @NotNull
    List<SomaticVariant> allSomaticVariants();

    @NotNull
    List<ReportableVariant> reportableSomaticVariants();

    @NotNull
    List<SomaticVariant> allGermlineVariants();

    @NotNull
    List<ReportableVariant> reportableGermlineVariants();

    @NotNull
    List<GeneCopyNumber> allSomaticGeneCopyNumbers();

    @NotNull
    List<GainLoss> allSomaticGainsLosses();

    @NotNull
    List<GainLoss> reportableSomaticGainsLosses();

    @NotNull
    List<GermlineDeletion> allGermlineDeletions();

    @NotNull
    List<GermlineDeletion> reportableGermlineDeletions();

    @NotNull
    List<CnPerChromosomeArmData> copyNumberPerChromosome();
}
