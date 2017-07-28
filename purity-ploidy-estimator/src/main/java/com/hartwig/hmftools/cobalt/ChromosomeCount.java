package com.hartwig.hmftools.cobalt;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.purple.ratio.ImmutableReadCount;
import com.hartwig.hmftools.common.purple.ratio.ReadCount;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.SAMRecord;

class ChromosomeCount {

    private final String contig;
    private final String chromosome;
    private final long chromosomeLength;
    private final int windowSize;
    private final List<ReadCount> result = Lists.newArrayList();

    private long start;
    private int count;

    ChromosomeCount(@NotNull final String contig, @NotNull final String chromosome, final long chromosomeLength, final int windowSize) {
        this.contig = contig;
        this.chromosome = chromosome;
        this.chromosomeLength = chromosomeLength;
        this.windowSize = windowSize;

        start = 1;
        count = -1;
    }

    String contig() {
        return contig;
    }

    String chromosome() {
        return chromosome;
    }

    void addRecord(SAMRecord record) {
        if (isEligible(record)) {

            long window = windowPosition(record.getAlignmentStart());
            if (start != window) {
                addReadCount(start, count);
                start = window;
                count = 0;
            }
            count++;
        }
    }

    public List<ReadCount> readCount() {

        // Finalise last record
        addReadCount(start, count);

        // Add chromosome length
        long lastWindowPosition = lastWindowPosition();
        if (result.get(result.size() - 1).position() < lastWindowPosition) {
            addReadCount(lastWindowPosition, -1);
        }

        return result;
    }

    private void addReadCount(long position, int count) {
        result.add(ImmutableReadCount.builder().chromosome(chromosome).position(position).readCount(count).build());
    }

    private boolean isEligible(SAMRecord record) {
        return !(record.getReadUnmappedFlag() || record.getDuplicateReadFlag() || record.getSecondOfPairFlag());
    }

    @VisibleForTesting
    long lastWindowPosition() {
        return windowPosition(chromosomeLength);
    }

    @VisibleForTesting
    long windowPosition(long position) {
        return (position - 1) / windowSize * windowSize + 1;
    }

}
