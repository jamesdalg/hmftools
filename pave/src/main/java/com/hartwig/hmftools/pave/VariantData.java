package com.hartwig.hmftools.pave;

import static java.lang.Math.abs;

import static com.hartwig.hmftools.common.variant.VariantType.INDEL;
import static com.hartwig.hmftools.common.variant.VariantType.MNP;
import static com.hartwig.hmftools.common.variant.VariantType.SNP;
import static com.hartwig.hmftools.pave.PaveConstants.DELIM;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.variant.VariantType;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;

public class VariantData
{
    public final String Chromosome;

    public final int Position; // as per the variant definition

    // position and end position should be symmetrical, so that EndPosition functions like Position on the negative strand
    // pos for an SNV
    // pos + alt-length - 1 for MNV
    // pos + ref-length - 1 for DEL
    // pos + 1 for an INS
    public final int EndPosition;

    public final String Ref;
    public final String Alt;

    // other key data
    public boolean mPhasedInframeIndel;
    public String mMicrohomology;
    public String mRepeatSequence;

    private final int mIndelBaseDiff;

    // compute and cache base positions which have changed due to this variant:
    // for an insert, none
    // for a delete, the deleted positions, so starting after the first (ref) position
    // for SNVs and MNVs, all of them
    private final List<Integer> mNonRefPositions;

    private final Map<String,List<VariantTransImpact>> mGeneImpacts;

    public VariantData(final String chromosome, final int position, final String ref, final String alt)
    {
        Chromosome = chromosome;
        Position = position;
        Ref = ref;
        Alt = alt;

        mPhasedInframeIndel = false;
        mMicrohomology = "";
        mRepeatSequence = "";

        mIndelBaseDiff = Alt.length() - Ref.length();

        if(isInsert())
        {
            mNonRefPositions = Lists.newArrayListWithExpectedSize(0);
            EndPosition = Position + 1;
        }
        else if(isDeletion())
        {
            int count = abs(mIndelBaseDiff);
            mNonRefPositions = Lists.newArrayListWithExpectedSize(count);

            for(int i = 1; i < Ref.length(); ++i)
            {
                mNonRefPositions.add(Position + i);
            }

            EndPosition = Position + count; // the first based after the deleted section
        }
        else
        {
            int count = Ref.length();
            mNonRefPositions = Lists.newArrayListWithExpectedSize(count);

            for(int i = 0; i < Ref.length(); ++i)
            {
                mNonRefPositions.add(Position + i);
            }

            EndPosition = Position + count - 1;
        }

        mGeneImpacts = Maps.newHashMap();
    }

    public static VariantData fromContext(final VariantContext variantContext)
    {
        int variantPosition = variantContext.getStart();
        String chromosome = variantContext.getContig();

        String ref = variantContext.getReference().getBaseString();
        String alt = variantContext.getAlternateAlleles().stream().map(Allele::toString).collect(Collectors.joining(","));

        return new VariantData(chromosome, variantPosition, ref, alt);
    }

    public VariantType type()
    {
        if(mIndelBaseDiff == 0)
            return Ref.length() == 1 ? SNP : MNP;

        return INDEL;
    }

    public int baseDiff() { return mIndelBaseDiff; }
    public boolean isBaseChange() { return mIndelBaseDiff == 0; }
    public boolean isIndel() { return mIndelBaseDiff != 0; }
    public boolean isInsert() { return mIndelBaseDiff > 0; }
    public boolean isDeletion() { return mIndelBaseDiff < 0; }

    public int[] positions() { return new int[] { Position, EndPosition }; }

    public List<Integer> nonRefPositions() { return mNonRefPositions; }

    public boolean phasedInframeIndel() { return mPhasedInframeIndel; }

    public void setVariantDetails(boolean piIndel, final String mh, final String reqSeq)
    {
        mPhasedInframeIndel = piIndel;
        mMicrohomology = mh;
        mRepeatSequence = reqSeq;
    }

    public String microhomology() { return mMicrohomology; }
    public String repeatSequence() { return mRepeatSequence; }

    public Map<String,List<VariantTransImpact>> getImpacts() { return mGeneImpacts; }

    public void addImpact(final String geneName, final VariantTransImpact impact)
    {
        List<VariantTransImpact> geneImpacts = mGeneImpacts.get(geneName);

        if(geneImpacts == null)
        {
            geneImpacts = Lists.newArrayList(impact);
            mGeneImpacts.put(geneName, geneImpacts);
            return;
        }

        if(geneImpacts.stream().filter(x -> x.TransData != null).anyMatch(x -> x.TransData.TransId == impact.TransData.TransId))
            return;

        geneImpacts.add(impact);
    }

    public String toString()
    {
        return String.format("pos(%s:%d) variant(%s>%s)", Chromosome, Position, Ref, Alt);
    }

    public static String csvCommonHeader()
    {
        StringJoiner sj = new StringJoiner(DELIM);
        sj.add("Chromosome").add("Position").add("Type").add("Ref").add("Alt");
        return sj.toString();
    }

    public String toCsv()
    {
        StringJoiner sj = new StringJoiner(DELIM);
        sj.add(Chromosome);
        sj.add(String.valueOf(Position));
        sj.add(String.valueOf(type()));
        sj.add(Ref);
        sj.add(Alt);

        return sj.toString();
    }


}
