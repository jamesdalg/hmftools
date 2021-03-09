package com.hartwig.hmftools.common.variant.enrich;

import static com.hartwig.hmftools.common.variant.VariantHeader.PURPLE_VARIANT_CN_INFO;

import java.util.Set;
import java.util.function.Consumer;

import com.hartwig.hmftools.common.utils.Doubles;
import com.hartwig.hmftools.common.variant.AllelicDepth;

import org.jetbrains.annotations.NotNull;

import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.vcf.VCFHeader;

public class GermlineRescueLowVAFEnrichment implements VariantContextEnrichment {

    private static final double MIN_VCN = 0.5;
    private static final int MIN_ALLELE_READ_COUNT = 3;

    private final String germlineSample;
    private final Consumer<VariantContext> consumer;

    public GermlineRescueLowVAFEnrichment(final String germlineSample, final Consumer<VariantContext> consumer) {
        this.germlineSample = germlineSample;
        this.consumer = consumer;
    }

    @Override
    public void accept(@NotNull final VariantContext context) {
        consumer.accept(process(germlineSample, context));
    }

    @NotNull
    static VariantContext process(@NotNull String germlineSample, @NotNull VariantContext context) {
        Set<String> filters = context.getFilters();
        if (filters.size() == 1 && filters.contains(GermlineGenotypeEnrichment.LOW_VAF_FILTER)) {
            Genotype germlineGenotype = context.getGenotype(germlineSample);
            AllelicDepth germlineDepth = AllelicDepth.fromGenotype(germlineGenotype);
            double variantCopyNumber = context.getAttributeAsDouble(PURPLE_VARIANT_CN_INFO, 0.0);
            if (germlineDepth.alleleReadCount() >= MIN_ALLELE_READ_COUNT && Doubles.greaterOrEqual(variantCopyNumber, MIN_VCN)) {
                return new VariantContextBuilder(context).passFilters().make();
            }
        }

        return context;
    }

    @Override
    public void flush() {

    }

    @NotNull
    @Override
    public VCFHeader enrichHeader(@NotNull final VCFHeader template) {
        return template;
    }
}
