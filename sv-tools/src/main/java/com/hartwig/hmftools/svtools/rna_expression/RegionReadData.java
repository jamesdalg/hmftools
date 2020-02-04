package com.hartwig.hmftools.svtools.rna_expression;

import static com.hartwig.hmftools.svtools.rna_expression.ReadRecord.MATCH_TYPE_EXON_BOUNDARY;
import static com.hartwig.hmftools.svtools.rna_expression.ReadRecord.MATCH_TYPE_INTRONIC;
import static com.hartwig.hmftools.svtools.rna_expression.ReadRecord.MATCH_TYPE_WITHIN_EXON;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.genome.region.GenomeRegion;
import com.hartwig.hmftools.common.genome.region.GenomeRegions;

public class RegionReadData
{
    public GenomeRegion Region;

    private final List<String> mRefRegions; // identifiers for this region, eg transcriptId

    private String mRefBases;
    private int[] mRefBasesMatched;
    private int[] mMatchTypeCounts;
    private final Map<RegionReadData,Integer> mLinkedRegions; // count of reads covering this region and another next to it
    private List<RegionReadData> mPreRegions; // references to adjacent regions with a lower position
    private List<RegionReadData> mPostRegions;

    public RegionReadData(final GenomeRegion region)
    {
        Region = region;

        mRefRegions = Lists.newArrayList();

        mRefBases = "";
        mMatchTypeCounts = new int[MATCH_TYPE_INTRONIC+1];

        mPreRegions = Lists.newArrayList();
        mPostRegions = Lists.newArrayList();
        mLinkedRegions = Maps.newHashMap();
    }

    public String chromosome() { return Region.chromosome(); }
    public long start() { return Region.start(); }
    public long end() { return Region.end(); }

    public void resetRegionBounds(long posStart, long posEnd)
    {
        Region = GenomeRegions.create(Region.chromosome(), posStart, posEnd);
    }

    public static final int NO_EXON = -1;
    public static final int TRANS_ID = 0;
    public static final int EXON_RANK = 1;

    private static String formExonRefId(final String transId, int exonRank) {  return String.format("%s:%d", transId, exonRank); }
    public static String extractTransId(final String ref) { return ref.split(":")[TRANS_ID]; }
    public static int extractExonRank(final String ref) { return Integer.valueOf(ref.split(":")[EXON_RANK]); }

    public int getExonRank(final String transId)
    {
        final String exonRefId = mRefRegions.stream().filter(x -> x.contains(transId)).findFirst().orElse(null);
        return exonRefId != null ? extractExonRank(exonRefId) : NO_EXON;
    }

    public final List<String> getRefRegions() { return mRefRegions; }

    public void addExonRef(final String transId, int exonRank)
    {
        if(!mRefRegions.contains(transId))
            mRefRegions.add(formExonRefId(transId, exonRank));
    }

    public void addMatchedRead(int matchType) { ++mMatchTypeCounts[matchType]; }
    public int matchedReadCount(int matchType) { return mMatchTypeCounts[matchType]; }

    public final String refBases() { return mRefBases; }

    public void setRefBases(final String bases)
    {
        mRefBases = bases;
        mRefBasesMatched = new int[(int)mRefBases.length()+1];
    }

    public int length() { return mRefBases.length(); }
    public int[] refBasesMatched() { return mRefBasesMatched; }

    public List<RegionReadData> getPreRegions() { return mPreRegions; }
    public List<RegionReadData> getPostRegions() { return mPostRegions; }

    public void addPreRegion(final RegionReadData region)
    {
        if(!mPreRegions.contains(region))
            mPreRegions.add(region);
    }

    public void addPostRegion(final RegionReadData region)
    {
        if(!mPostRegions.contains(region))
            mPostRegions.add(region);
    }

    public final Map<RegionReadData, Integer> getLinkedRegions() { return mLinkedRegions; }

    public void addLinkedRegion(final RegionReadData region)
    {
        Integer linkCount = mLinkedRegions.get(region);

        if(linkCount != null)
            mLinkedRegions.put(region, linkCount + 1);
        else
            mLinkedRegions.put(region, 1);
    }

    public double averageDepth()
    {
        if(mRefBasesMatched == null)
            return 0;

        int depthTotal = Arrays.stream(mRefBasesMatched).sum();
        return depthTotal/(double)mRefBasesMatched.length;
    }

    public int baseCoverage(int minReadCount)
    {
        if(mRefBasesMatched == null)
            return 0;

        // percent of bases covered by a read
        return (int)Arrays.stream(mRefBasesMatched).filter(x -> x >= minReadCount).count();
    }

    public String toString()
    {
        return String.format("%s %s:%d -> %d refs(%d) %s",
                !mRefRegions.isEmpty() ? mRefRegions.get(0) : "unknown", chromosome(), start(), end(), mRefRegions.size(),
                mRefBases != null ? String.format("reads(sj=?? e=%d eb=%d i=%d)",
                mMatchTypeCounts[MATCH_TYPE_WITHIN_EXON],
                mMatchTypeCounts[MATCH_TYPE_EXON_BOUNDARY], mMatchTypeCounts[MATCH_TYPE_INTRONIC]) : "intron");
    }

    public void clearState()
    {
        if(mRefBasesMatched != null)
        {
            for (int i = 0; i < mRefBasesMatched.length; ++i)
                mRefBasesMatched[i] = 0;
        }

        mLinkedRegions.clear();

        for(int i = 0; i < mMatchTypeCounts.length; ++i)
            mMatchTypeCounts[i] = 0;
    }
}
