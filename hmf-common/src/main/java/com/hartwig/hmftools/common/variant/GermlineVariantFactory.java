package com.hartwig.hmftools.common.variant;

import static com.hartwig.hmftools.common.variant.VariantHeader.PURPLE_GERMLINE_INFO;

import static htsjdk.tribble.AbstractFeatureReader.getFeatureReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.genotype.GenotypeStatus;
import com.hartwig.hmftools.common.pathogenic.PathogenicSummary;
import com.hartwig.hmftools.common.purple.GermlineStatus;
import com.hartwig.hmftools.common.sage.SageMetaData;
import com.hartwig.hmftools.common.variant.filter.HumanChromosomeFilter;
import com.hartwig.hmftools.common.variant.filter.NTFilter;
import com.hartwig.hmftools.common.variant.impact.VariantImpact;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.filter.CompoundFilter;
import htsjdk.variant.variantcontext.filter.PassingVariantFilter;
import htsjdk.variant.variantcontext.filter.VariantContextFilter;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFHeader;

public final class GermlineVariantFactory
{
    public static List<GermlineVariant> fromVCFFile(final String tumor, final String vcfFile) throws Exception
    {
        List<GermlineVariant> variants = Lists.newArrayList();

        final AbstractFeatureReader<VariantContext, LineIterator> reader = getFeatureReader(vcfFile, new VCFCodec(), false);

        // final VCFHeader header = (VCFHeader) reader.getHeader();

        for(VariantContext variantContext : reader.iterator())
        {
            GermlineVariant variant = createVariant(tumor, "", variantContext);
            variants.add(variant);
        }

        return variants;
    }

    public static GermlineVariant createVariant(final String sample, final String reference, final VariantContext context)
    {
        // final Genotype genotype = context.getGenotype(sample);

        final VariantContextDecorator decorator = new VariantContextDecorator(context);
        final GenotypeStatus genotypeStatus = reference != null ? decorator.genotypeStatus(reference) : null;

        final AllelicDepth tumorDepth = AllelicDepth.fromGenotype(context.getGenotype(sample));

        /*
        final Optional<AllelicDepth> referenceDepth = Optional.ofNullable(reference)
                .flatMap(x -> Optional.ofNullable(context.getGenotype(x)))
                .filter(AllelicDepth::containsAllelicDepth)
                .map(AllelicDepth::fromGenotype);

        final Optional<AllelicDepth> rnaDepth = Optional.ofNullable(rna)
                .flatMap(x -> Optional.ofNullable(context.getGenotype(x)))
                .filter(AllelicDepth::containsAllelicDepth)
                .map(AllelicDepth::fromGenotype);
        */

        final VariantImpact variantImpact = decorator.variantImpact();

        final PathogenicSummary pathogenicSummary = decorator.pathogenicSummary();

        ImmutableGermlineVariantImpl.Builder builder = ImmutableGermlineVariantImpl.builder()
                .qual(decorator.qual())
                .type(decorator.type())
                .filter(decorator.filter())
                .chromosome(decorator.chromosome())
                .position(decorator.position())
                .ref(decorator.ref())
                .alt(decorator.alt())
                .alleleReadCount(tumorDepth.alleleReadCount())
                .totalReadCount(tumorDepth.totalReadCount())
                .hotspot(decorator.hotspot())
                .minorAlleleCopyNumber(decorator.minorAlleleCopyNumber())
                .adjustedCopyNumber(decorator.adjustedCopyNumber())
                .adjustedVAF(decorator.adjustedVaf())
                .variantCopyNumber(decorator.variantCopyNumber())
                .mappability(decorator.mappability())
                .tier(decorator.tier())
                .trinucleotideContext(decorator.trinucleotideContext())
                .microhomology(decorator.microhomology())
                .repeatCount(decorator.repeatCount())
                .repeatSequence(decorator.repeatSequence())
                .reported(decorator.reported())
                .biallelic(decorator.biallelic())
                .gene(variantImpact.CanonicalGeneName)
                .canonicalTranscript(variantImpact.CanonicalTranscript)
                .canonicalEffect(variantImpact.CanonicalEffect)
                .canonicalCodingEffect(variantImpact.CanonicalCodingEffect)
                .canonicalHgvsCodingImpact(variantImpact.CanonicalHgvsCoding)
                .canonicalHgvsProteinImpact(variantImpact.CanonicalHgvsProtein)
                .spliceRegion(variantImpact.CanonicalSpliceRegion)
                .otherReportedEffects(variantImpact.OtherReportableEffects)
                .worstCodingEffect(variantImpact.WorstCodingEffect)
                .genesAffected(variantImpact.GenesAffected)
                .germlineStatus(GermlineStatus.valueOf(context.getAttributeAsString(PURPLE_GERMLINE_INFO, "UNKNOWN")))
                .clinvarInfo(pathogenicSummary.clinvarInfo())
                .pathogenic(pathogenicSummary.pathogenicity().isPathogenic())
                .pathogenicity(pathogenicSummary.pathogenicity().toString());

        builder.genotypeStatus(genotypeStatus != null ? genotypeStatus : GenotypeStatus.UNKNOWN);

        return builder.build();
    }

}
