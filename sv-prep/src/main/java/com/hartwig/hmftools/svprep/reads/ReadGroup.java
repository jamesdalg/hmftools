package com.hartwig.hmftools.svprep.reads;

import static com.hartwig.hmftools.svprep.CombinedReadGroups.formChromosomePartition;
import static com.hartwig.hmftools.svprep.reads.ReadType.JUNCTION;

import java.util.List;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.samtools.SupplementaryReadData;
import com.hartwig.hmftools.common.utils.sv.ChrBaseRegion;

import htsjdk.samtools.SAMRecord;

public class ReadGroup
{
    private final List<ReadRecord> mReads;

    private ReadGroupStatus mStatus;
    private final Set<String> mRemotePartitions; // given that supplementaries are no longer included, this is now 0 or 1 entries
    private int mExpectedReadCount;
    private Set<Integer> mJunctionPositions;
    private boolean mIsRemoteExpected;

    public static int MAX_GROUP_READ_COUNT = 4;

    public ReadGroup(final ReadRecord read)
    {
        mReads = Lists.newArrayListWithCapacity(2);
        mStatus = ReadGroupStatus.UNSET;
        mRemotePartitions = Sets.newHashSet();
        mExpectedReadCount = 0;
        mJunctionPositions = null;
        mIsRemoteExpected = false;
        addRead(read);
    }

    public final String id() { return mReads.get(0).id(); }
    public List<ReadRecord> reads() { return mReads; }
    public int size() { return mReads.size(); }

    public boolean isComplete() { return mStatus == ReadGroupStatus.COMPLETE; }

    public boolean spansPartitions() { return !mRemotePartitions.isEmpty() || mIsRemoteExpected; }
    public int partitionCount() { return mRemotePartitions.size() + 1; }
    public int expectedReadCount() { return mExpectedReadCount; }
    public Set<String> remotePartitions() { return mRemotePartitions; }

    public Set<Integer> junctionPositions() { return mJunctionPositions; }

    public void addRead(final ReadRecord read)
    {
        mReads.add(read);
    }

    public void addJunctionPosition(int position)
    {
        if(mJunctionPositions == null)
            mJunctionPositions = Sets.newHashSet();

        mJunctionPositions.add(position);
    }

    public ReadGroupStatus groupStatus() { return mStatus; }

    public boolean isRemoteExpected() { return mIsRemoteExpected; }
    public void markRemoteExpected() { mIsRemoteExpected = true; }

    public boolean isSimpleComplete()
    {
        // no supplementaries and both reads received
        return mReads.size() == 2 && mReads.stream().allMatch(x -> !x.hasSuppAlignment() && !x.isSupplementaryAlignment());
    }

    public boolean hasTypeAndMapQual(final ReadType type, int minMapQual)
    {
        return mReads.stream().anyMatch(x -> x.readType() == type && x.mapQuality() >= minMapQual);
    }

    public boolean allNoSupport() { return mReads.stream().allMatch(x -> x.readType() == ReadType.NO_SUPPORT); }

    public boolean isJunctionFragment()
    {
        return mReads.stream().anyMatch(x -> x.readType() == JUNCTION);
    }

    public void setGroupState()
    {
        if(onlySupplementaries())
        {
            if(mReads.size() == 2)
                mStatus = ReadGroupStatus.PAIRED;
            else
                mStatus = ReadGroupStatus.SUPPLEMENTARY;

            mExpectedReadCount = mReads.size();
            return;
        }

        boolean firstHasSupp = false;
        boolean secondHasSupp = false;
        boolean nonSuppPaired = false;

        for(ReadRecord read : mReads)
        {
            if(!read.isSupplementaryAlignment() && !nonSuppPaired)
            {
                nonSuppPaired = hasReadMate(read);
            }

            if(read.isFirstOfPair())
            {
                if(read.hasSuppAlignment() || read.isSupplementaryAlignment())
                    firstHasSupp = true;
            }
            else
            {
                if(read.hasSuppAlignment() || read.isSupplementaryAlignment())
                    secondHasSupp = true;
            }
        }

        int suppCount = (firstHasSupp ? 1 : 0) + (secondHasSupp ? 1 : 0);
        mExpectedReadCount = 2 + suppCount;

        if(mReads.size() >= mExpectedReadCount)
            mStatus = ReadGroupStatus.COMPLETE;
        else if(nonSuppPaired)
            mStatus = ReadGroupStatus.PAIRED;
        else
            mStatus = ReadGroupStatus.INCOMPLETE;
    }

    public boolean onlySupplementaries() { return mReads.stream().allMatch(x -> x.isSupplementaryAlignment()); }

    public void setPartitionCount(final ChrBaseRegion region, int partitionSize)
    {
        // only count non-supplementaries towards remote partitions
        for(ReadRecord read : mReads)
        {
            if(read.isUnmapped())
                continue;

            /*
            if(read.hasSuppAlignment())
            {
                SupplementaryReadData suppData = read.supplementaryAlignment();

                if(!supplementaryInRegion(suppData, region))
                {
                    mRemotePartitions.add(formChromosomePartition(suppData.Chromosome, suppData.Position, partitionSize));
                }
            }
            */

            if(HumanChromosome.contains(read.MateChromosome) && !region.containsPosition(read.MateChromosome, read.MatePosStart))
            {
                mRemotePartitions.add(formChromosomePartition(read.MateChromosome, read.MatePosStart, partitionSize));
            }
        }
    }

    public boolean hasReadMate(final ReadRecord read)
    {
        for(ReadRecord otherRead : mReads)
        {
            if(otherRead == read)
                continue;

            if(read.isFirstOfPair() == otherRead.isFirstOfPair())
                continue;

            if(read.isSupplementaryAlignment() != otherRead.isSupplementaryAlignment())
                continue;

            if(read.MateChromosome.equals(otherRead.Chromosome) && read.MatePosStart == otherRead.start())
                return true;
        }

        return false;
    }

    public String toString()
    {
        return String.format("%s reads(%d) state(%s) partitions(%d)", id(), mReads.size(), mStatus, partitionCount());
    }

    public boolean hasSupplementaryMatch(final SupplementaryReadData suppData)
    {
        for(ReadRecord read : mReads)
        {
            if(read.supplementaryAlignment() == suppData)
                continue;

            if(read.Chromosome.equals(suppData.Chromosome) && read.start() == suppData.Position && read.cigar().toString().equals(suppData.Cigar))
                return true;
        }

        return false;
    }

    private static boolean supplementaryInRegion(final SupplementaryReadData suppData, final ChrBaseRegion region)
    {
        return suppData != null && region.containsPosition(suppData.Chromosome, suppData.Position);
    }
}
