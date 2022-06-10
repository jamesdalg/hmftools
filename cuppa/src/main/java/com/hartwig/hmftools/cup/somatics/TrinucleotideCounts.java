package com.hartwig.hmftools.cup.somatics;

import static com.hartwig.hmftools.common.sigs.SnvSigUtils.populateBucketMap;
import static com.hartwig.hmftools.common.sigs.SnvSigUtils.variantContext;
import static com.hartwig.hmftools.common.utils.MatrixUtils.copy;
import static com.hartwig.hmftools.common.variant.VariantType.SNP;
import static com.hartwig.hmftools.cup.CuppaConfig.CUP_LOGGER;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.utils.Matrix;
import com.hartwig.hmftools.cup.common.CupCalcs;

public final class TrinucleotideCounts
{
    public static double[] extractTrinucleotideCounts(final List<SomaticVariant> variants, final Map<String,Integer> bucketNameMap)
    {
        final double[] counts = new double[bucketNameMap.size()];

        for(final SomaticVariant variant : variants)
        {
            if(variant.Type != SNP)
                continue;

            if(variant.Alt.length() != 1)
                continue;

            if(variant.TrinucleotideContext.contains("N"))
                continue;

            String bucketName = variantContext(variant.Ref, variant.Alt, variant.TrinucleotideContext);
            Integer bucketIndex = bucketNameMap.get(bucketName);

            if(bucketIndex == null)
            {
                CUP_LOGGER.error("invalid bucketName({}) from var({}>{}) context={})",
                        bucketName, variant.Ref, variant.Alt, variant.TrinucleotideContext);
                continue;
            }

            ++counts[bucketIndex];
        }

        return counts;
    }

    public static Matrix convertSomaticVariantsToSnvCounts(
            final String sampleId, final List<SomaticVariant> variants, final Map<String,Integer> sampleSnvCountsIndex)
    {
        final Map<String,Integer> triNucBucketNameMap = Maps.newHashMap();
        populateBucketMap(triNucBucketNameMap);

        final double[] triNucCounts = extractTrinucleotideCounts(variants, triNucBucketNameMap);

        final Matrix sampleSnvCounts = new Matrix(1, triNucCounts.length);

        sampleSnvCounts.setRow(0, triNucCounts);
        sampleSnvCountsIndex.put(sampleId, 0);

        return sampleSnvCounts;
    }
}
