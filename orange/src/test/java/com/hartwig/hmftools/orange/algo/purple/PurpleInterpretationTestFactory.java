package com.hartwig.hmftools.orange.algo.purple;

import com.hartwig.hmftools.common.purple.Gender;
import com.hartwig.hmftools.common.purple.ImmutablePurpleQC;
import com.hartwig.hmftools.common.purple.PurpleQC;
import com.hartwig.hmftools.common.purple.purity.FittedPurityMethod;
import com.hartwig.hmftools.common.variant.msi.MicrosatelliteStatus;
import com.hartwig.hmftools.common.variant.tml.TumorMutationalStatus;

import org.jetbrains.annotations.NotNull;

public final class PurpleInterpretationTestFactory {

    private PurpleInterpretationTestFactory() {
    }

    @NotNull
    public static PurpleInterpretedData createMinimalTestPurpleData() {
        return ImmutablePurpleInterpretedData.builder()
                .fit(createMinimalTestFitData())
                .characteristics(createMinimalTestCharacteristicsData())
                .build();
    }

    @NotNull
    private static PurityPloidyFit createMinimalTestFitData() {
        return ImmutablePurityPloidyFit.builder()
                .qc(qcPass())
                .fittedPurityMethod(FittedPurityMethod.NORMAL)
                .purity(0D)
                .minPurity(0D)
                .maxPurity(0D)
                .hasReliablePurity(true)
                .hasReliableQuality(true)
                .ploidy(0D)
                .minPloidy(0D)
                .maxPloidy(0D)
                .build();
    }

    @NotNull
    private static PurpleQC qcPass() {
        return ImmutablePurpleQC.builder()
                .method(FittedPurityMethod.NORMAL)
                .amberMeanDepth(0)
                .copyNumberSegments(1)
                .unsupportedCopyNumberSegments(0)
                .deletedGenes(0)
                .purity(0.5)
                .contamination(0D)
                .cobaltGender(Gender.FEMALE)
                .amberGender(Gender.FEMALE)
                .build();
    }

    @NotNull
    private static PurpleCharacteristics createMinimalTestCharacteristicsData() {
        return ImmutablePurpleCharacteristics.builder()
                .wholeGenomeDuplication(false)
                .microsatelliteIndelsPerMb(0D)
                .microsatelliteStatus(MicrosatelliteStatus.UNKNOWN)
                .tumorMutationalBurdenPerMb(0D)
                .tumorMutationalLoad(0)
                .tumorMutationalLoadStatus(TumorMutationalStatus.UNKNOWN)
                .svTumorMutationalBurden(0)
                .build();
    }
}
