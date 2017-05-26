package com.hartwig.hmftools.common.purple.region;

import static org.junit.Assert.assertEquals;

import com.hartwig.hmftools.common.copynumber.freec.FreecStatus;
import com.hartwig.hmftools.common.purple.FittedCopyNumber;
import com.hartwig.hmftools.common.purple.ImmutableFittedCopyNumber;

import org.junit.Test;

public class ConsolidatedRegionTest
{
    private static final double EPSILON = 1e-10;

    @Test
    public void averageOnLengthUntilNonZeroBafCount() {

        ConsolidatedRegionBuilder builder = new ConsolidatedRegionBuilder(create(1, 100_000_000, 3));
        assertAverages(builder, 0, 3);

        builder.extendRegion(create(100_000_001, 200_000_000, 4));
        assertAverages(builder, 0, 3.5);

        builder.extendRegion(create(200_000_001, 200_000_010, 1, 0.5, 3));
        assertAverages(builder, 0.5, 3);

        builder.extendRegion(create(200_000_011, 300_000_000, 3, 1, 4d));
        assertAverages(builder, 0.875, 3.75);
    }

    @Test
    public void averageOnLengthForNonZeroRatio() {
        ConsolidatedRegionBuilder builder = new ConsolidatedRegionBuilder(create(1, 100, 3));
        assertAverages(builder, 0, 3);

        builder.extendRegion(create(101, 200, 0));
        assertAverages(builder, 0, 3);
    }

    private void assertAverages(ConsolidatedRegionBuilder victim, double expectedBAF, double expectedRatio) {
        assertAverages(victim.build(), expectedBAF, expectedRatio);
    }

    private void assertAverages(ConsolidatedRegion victim, double expectedBAF, double expectedRatio) {
        assertEquals(expectedBAF, victim.averageObservedBAF(), EPSILON);
        assertEquals(expectedRatio, victim.averageTumorCopyNumber(), EPSILON);
    }

    private static FittedCopyNumber create(long start, long end, double ratio) {
        return create("1", start, end, 0, 0, ratio);
    }

    private static FittedCopyNumber create(long start, long end, int bafCount, double baf, double ratio) {
        return create("1", start, end, bafCount, baf, ratio);
    }

    private static FittedCopyNumber create(String chromosome, long start, long end, int bafCount, double baf, double tumorCopyNumber) {

        return ImmutableFittedCopyNumber.builder()
                .chromosome(chromosome)
                .start(start)
                .end(end)
                .bafCount(bafCount)
                .observedBAF(baf)
                .purityAdjustedBAF(baf)
                .tumorCopyNumber(tumorCopyNumber)
                .broadBAF(0)
                .broadTumorCopyNumber(0)
                .segmentBAF(0)
                .segmentTumorCopyNumber(0)
                .observedNormalRatio(0)
                .value(0)
                .cnvDeviation(0)
                .deviation(0)
                .fittedPloidy(0)
                .modelBAF(0)
                .observedTumorRatio(0)
                .modelTumorRatio(0)
                .status(FreecStatus.UNKNOWN)
                .refNormalisedCopyNumber(0)
                .bafDeviation(0)
                .build();
    }

}
