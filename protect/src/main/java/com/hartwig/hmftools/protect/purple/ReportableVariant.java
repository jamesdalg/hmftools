package com.hartwig.hmftools.protect.purple;

import com.hartwig.hmftools.common.utils.DataUtil;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.Hotspot;
import com.hartwig.hmftools.common.variant.Variant;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class ReportableVariant implements Variant {

    @NotNull
    public abstract ReportableVariantSource source();

    @NotNull
    @Value.Derived
    public String genomicEvent() {
        String description = canonicalCodingEffect() == CodingEffect.SPLICE ? canonicalHgvsCodingImpact() : canonicalHgvsProteinImpact();
        return this.gene() + " " + description;
    }

    @NotNull
    @Override
    public abstract String gene();

    @NotNull
    @Override
    public abstract String chromosome();

    @Override
    public abstract long position();

    @NotNull
    @Value.Derived
    public String gDNA() {
        return chromosome() + ":" + position();
    }

    @NotNull
    @Override
    public abstract String ref();

    @NotNull
    @Override
    public abstract String alt();

    @NotNull
    @Override
    public abstract String canonicalTranscript();

    @NotNull
    @Override
    public abstract CodingEffect canonicalCodingEffect();

    @NotNull
    @Override
    public abstract String canonicalHgvsCodingImpact();

    @NotNull
    @Override
    public abstract String canonicalHgvsProteinImpact();

    @Override
    public abstract int totalReadCount();

    @Override
    public abstract int alleleReadCount();

    public abstract double totalCopyNumber();

    public abstract double alleleCopyNumber();

    @NotNull
    @Value.Derived
    public String tVAF() {
        double vaf = alleleCopyNumber() / totalCopyNumber();
        return DataUtil.formatPercentage(100 * Math.max(0, Math.min(1, vaf)));
    }

    @NotNull
    public abstract Hotspot hotspot();

    public abstract double clonalLikelihood();

    public abstract double driverLikelihood();

    @NotNull
    @Value.Derived
    public DriverInterpretation driverLikelihoodInterpretation() {
        return DriverInterpretation.interpret(driverLikelihood());
    }

    public abstract boolean biallelic();
}
