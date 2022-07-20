package com.hartwig.hmftools.purple.somatic;

import static com.hartwig.hmftools.common.variant.Hotspot.HOTSPOT_FLAG;

import java.util.Set;

import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.common.variant.AllelicDepth;
import com.hartwig.hmftools.common.variant.VariantContextDecorator;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.common.variant.impact.VariantImpact;

import htsjdk.variant.variantcontext.VariantContext;

public class SomaticVariant implements GenomePosition
{
    private VariantContext mContext;

    private final String mChromosome;
    private final int mPosition;
    private VariantContextDecorator mDecorator;
    private final AllelicDepth mTumorAllelicDepth;

    public SomaticVariant(final VariantContext context, final String sampleId)
    {
        mContext = context;
        mDecorator = new VariantContextDecorator(mContext);

        mChromosome = mContext.getContig();
        mPosition = mContext.getStart();
        mTumorAllelicDepth = sampleId != null ? mDecorator.allelicDepth(sampleId) :  null;
    }

    public VariantContext context() { return mContext; }

    public void setContext(final VariantContext context)
    {
        mContext = context;
        mDecorator = new VariantContextDecorator(mContext);
    }

    @Override
    public String chromosome() { return mChromosome; }

    @Override
    public int position() { return mPosition; }

    // convenience methods
    public VariantContextDecorator decorator() { return mDecorator; }
    public VariantImpact variantImpact() { return mDecorator.variantImpact(); }

    public VariantType type() { return mDecorator.type(); }

    public boolean isPass() { return mDecorator.isPass(); }
    public boolean isFiltered() { return !isPass(); }

    public double copyNumber() { return mDecorator.variantCopyNumber(); }

    public boolean isHotspot() { return mContext.hasAttribute(HOTSPOT_FLAG); }
    public boolean biallelic() { return mDecorator.biallelic(); }
    public String gene() { return mDecorator.variantImpact().CanonicalGeneName; }

    public boolean hasAlleleDepth() { return mTumorAllelicDepth != null; }
    public AllelicDepth tumorAlleleDepth() { return mTumorAllelicDepth; }
    public double alleleFrequency() { return mTumorAllelicDepth != null ? mTumorAllelicDepth.alleleFrequency() : 0; }
    public int totalReadCount() { return mTumorAllelicDepth != null ? mTumorAllelicDepth.totalReadCount() : 0; }
    public int alleleReadCount() { return mTumorAllelicDepth != null ? mTumorAllelicDepth.alleleReadCount() : 0; }

    public String toString()
    {
        return String.format("%s %s:%d %s>%s filter(%s)",
                type(), chromosome(), position(), mDecorator.ref(), mDecorator.alt(), mDecorator.filter());
    }
}
