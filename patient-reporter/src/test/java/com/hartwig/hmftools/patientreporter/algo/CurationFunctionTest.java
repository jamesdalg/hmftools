package com.hartwig.hmftools.patientreporter.algo;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.linx.GeneDisruption;
import com.hartwig.hmftools.common.linx.HomozygousDisruption;
import com.hartwig.hmftools.common.linx.ImmutableGeneDisruption;
import com.hartwig.hmftools.common.linx.ImmutableHomozygousDisruption;
import com.hartwig.hmftools.common.protect.ImmutableProtectSource;
import com.hartwig.hmftools.common.protect.ProtectEvidence;
import com.hartwig.hmftools.common.protect.ProtectEvidenceType;
import com.hartwig.hmftools.common.protect.ProtectTestFactory;
import com.hartwig.hmftools.common.purple.interpretation.GainLoss;
import com.hartwig.hmftools.common.purple.interpretation.GainLossTestFactory;
import com.hartwig.hmftools.common.purple.interpretation.ImmutableGainLoss;
import com.hartwig.hmftools.common.serve.Knowledgebase;
import com.hartwig.hmftools.common.serve.actionability.EvidenceDirection;
import com.hartwig.hmftools.common.serve.actionability.EvidenceLevel;
import com.hartwig.hmftools.common.variant.ImmutableReportableVariant;
import com.hartwig.hmftools.common.variant.ReportableVariant;
import com.hartwig.hmftools.common.variant.ReportableVariantSource;
import com.hartwig.hmftools.common.variant.ReportableVariantTestFactory;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class CurationFunctionTest {

    private static final String GENE_CDKN2A_CANONICAL = "CDKN2A (p16)";
    private static final String GENE_CDKN2A_NON_CANONICAL = "CDKN2A (p14ARF)";

    @Test
    public void canCurateTumorSpecificEvidence() {
        List<ProtectEvidence> tumorSpecificEvidence = evidence();
        List<ProtectEvidence> curated = CurationFunction.curateEvidence(tumorSpecificEvidence);

        assertEquals(curated.size(), 3);
        assertEquals(findByGeneProtect(curated, "KRAS", false), "KRAS");
        assertEquals(findByGeneProtect(curated, GENE_CDKN2A_CANONICAL, true), GENE_CDKN2A_CANONICAL);
        assertEquals(findByGeneProtect(curated, GENE_CDKN2A_NON_CANONICAL, false), GENE_CDKN2A_NON_CANONICAL);
    }

    @Test
    public void canCurateClinicalTrials() {
        List<ProtectEvidence> clinicalTrials = evidence();
        List<ProtectEvidence> curated = CurationFunction.curateEvidence(clinicalTrials);

        assertEquals(curated.size(), 3);
        assertEquals(findByGeneProtect(curated, "KRAS", false), "KRAS");
        assertEquals(findByGeneProtect(curated, GENE_CDKN2A_CANONICAL, true), GENE_CDKN2A_CANONICAL);
        assertEquals(findByGeneProtect(curated, GENE_CDKN2A_NON_CANONICAL, false), GENE_CDKN2A_NON_CANONICAL);
    }

    @Test
    public void canCurateOffLabelEvidence() {
        List<ProtectEvidence> offLabelEvidence = evidence();
        List<ProtectEvidence> curated = CurationFunction.curateEvidence(offLabelEvidence);

        assertEquals(curated.size(), 3);
        assertEquals(findByGeneProtect(curated, "KRAS", false), "KRAS");
        assertEquals(findByGeneProtect(curated, GENE_CDKN2A_CANONICAL, true), GENE_CDKN2A_CANONICAL);
        assertEquals(findByGeneProtect(curated, GENE_CDKN2A_NON_CANONICAL, false), GENE_CDKN2A_NON_CANONICAL);
    }

    @NotNull
    private static String findByGeneProtect(List<ProtectEvidence> curate, @NotNull String gene, boolean isCanonical) {
        for (ProtectEvidence evidence : curate) {
            if (evidence.gene().equals(gene) && evidence.isCanonical().equals(isCanonical)) {
                return evidence.gene();
            }
        }
        throw new IllegalStateException("Could not find gene with canonical: " + gene + " " + isCanonical);
    }

    @Test
    public void canCurateReportableVariants() {
        List<ReportableVariant> variants = reportableVariants();
        List<ReportableVariant> curated = CurationFunction.curateReportableVariants(variants);

        assertEquals(curated.size(), 3);
        assertEquals(findByGeneVariant(curated, "KRAS", false), "KRAS");
        assertEquals(findByGeneVariant(curated, GENE_CDKN2A_CANONICAL, true), GENE_CDKN2A_CANONICAL);
        assertEquals(findByGeneVariant(curated, GENE_CDKN2A_NON_CANONICAL, false), GENE_CDKN2A_NON_CANONICAL);
    }

    @Test
    public void canCurateNotifyGermlineStatusPerVariant() {
        ReportableVariant somaticVariant1 =
                createTestReportableVariantBuilder().gene("KRAS").isCanonical(false).source(ReportableVariantSource.SOMATIC).build();
        ReportableVariant somaticVariant2 =
                createTestReportableVariantBuilder().gene("CDKN2A").isCanonical(true).source(ReportableVariantSource.SOMATIC).build();
        ReportableVariant germlineVariant1 =
                createTestReportableVariantBuilder().gene("CDKN2A").isCanonical(false).source(ReportableVariantSource.GERMLINE).build();

        Map<ReportableVariant, Boolean> notifyGermlineVariants = Maps.newHashMap();
        notifyGermlineVariants.put(somaticVariant1, false);
        notifyGermlineVariants.put(somaticVariant2, true);
        notifyGermlineVariants.put(germlineVariant1, false);

        Map<ReportableVariant, Boolean> map = CurationFunction.curateNotifyGermlineStatusPerVariant(notifyGermlineVariants);
        List<ReportableVariant> curated = Lists.newArrayList(map.keySet());

        assertEquals(curated.size(), 3);
        assertEquals(findByGeneVariant(curated, "KRAS", false), "KRAS");
        assertEquals(findByGeneVariant(curated, GENE_CDKN2A_CANONICAL, true), GENE_CDKN2A_CANONICAL);
        assertEquals(findByGeneVariant(curated, GENE_CDKN2A_NON_CANONICAL, false), GENE_CDKN2A_NON_CANONICAL);
    }

    @NotNull
    private static String findByGeneVariant(List<ReportableVariant> curate, @NotNull String gene, boolean isCanonical) {
        for (ReportableVariant variant : curate) {
            if (variant.gene().equals(gene) && variant.isCanonical() == isCanonical) {
                return variant.gene();
            }
        }
        throw new IllegalStateException("Could not find gene with canonical: " + gene + " " + isCanonical);
    }

    @Test
    public void canCurateGainsAndLosses() {
        List<GainLoss> gainLoss = gainloss();
        List<GainLoss> curated = CurationFunction.curateGainsAndLosses(gainLoss);

        assertEquals(curated.size(), 3);
        assertEquals(findByGeneGainLoss(curated, "BRAF", true), "BRAF");
        assertEquals(findByGeneGainLoss(curated, GENE_CDKN2A_CANONICAL, true), GENE_CDKN2A_CANONICAL);
        assertEquals(findByGeneGainLoss(curated, GENE_CDKN2A_NON_CANONICAL, false), GENE_CDKN2A_NON_CANONICAL);
    }

    @NotNull
    private static String findByGeneGainLoss(List<GainLoss> curate, @NotNull String gene, boolean isCanonical) {
        for (GainLoss gainLoss : curate) {
            if (gainLoss.gene().equals(gene) && gainLoss.isCanonical() == isCanonical) {
                return gainLoss.gene();
            }
        }
        throw new IllegalStateException("Could not find gene with canonical: " + gene + " " + isCanonical);
    }

    @Test
    public void canCurateGeneDisruptions() {
        List<GeneDisruption> disruptions = geneDisruption();
        List<GeneDisruption> curated = CurationFunction.curateGeneDisruptions(disruptions);

        assertEquals(curated.size(), 3);
        assertEquals(findByGeneGDisruption(curated, "NRAS", true), "NRAS");
        assertEquals(findByGeneGDisruption(curated, GENE_CDKN2A_CANONICAL, true), GENE_CDKN2A_CANONICAL);
        assertEquals(findByGeneGDisruption(curated, GENE_CDKN2A_NON_CANONICAL, false), GENE_CDKN2A_NON_CANONICAL);
    }

    @NotNull
    private static String findByGeneGDisruption(List<GeneDisruption> curate, @NotNull String gene, boolean isCanonical) {
        for (GeneDisruption disruption : curate) {
            if (disruption.gene().equals(gene) && disruption.isCanonical() == isCanonical) {
                return disruption.gene();
            }
        }
        throw new IllegalStateException("Could not find gene with canonical: " + gene + " " + isCanonical);
    }

    @Test
    public void canCurateHomozygousDisruptions() {
        List<HomozygousDisruption> homozygousDisruptions = homozygousDisruptions();
        List<HomozygousDisruption> curated = CurationFunction.curateHomozygousDisruptions(homozygousDisruptions);

        assertEquals(curated.size(), 3);
        assertEquals(findByGeneHomozygousDisruption(curated, "NRAS", true), "NRAS");
        assertEquals(findByGeneHomozygousDisruption(curated, GENE_CDKN2A_CANONICAL, true), GENE_CDKN2A_CANONICAL);
        assertEquals(findByGeneHomozygousDisruption(curated, GENE_CDKN2A_NON_CANONICAL, false), GENE_CDKN2A_NON_CANONICAL);
    }

    @NotNull
    private static String findByGeneHomozygousDisruption(List<HomozygousDisruption> curate, @NotNull String gene,
            boolean isCanonical) {
        for (HomozygousDisruption disruption : curate) {
            if (disruption.gene().equals(gene) && disruption.isCanonical() == isCanonical) {
                return disruption.gene();
            }
        }

        throw new IllegalStateException("Could not find gene with canonical: " + gene + " " + isCanonical);
    }

    @NotNull
    public List<ProtectEvidence> evidence() {
        ProtectEvidence evidence1 = ProtectTestFactory.builder()
                .gene("KRAS")
                .isCanonical(false)
                .event("amp")
                .germline(true)
                .reported(true)
                .treatment("TryMe")
                .onLabel(true)
                .level(EvidenceLevel.B)
                .direction(EvidenceDirection.RESPONSIVE)
                .sources(Sets.newHashSet(ImmutableProtectSource.builder()
                        .name(Knowledgebase.ICLUSION)
                        .sourceEvent(Strings.EMPTY)
                        .sourceUrls(Sets.newHashSet())
                        .evidenceType(ProtectEvidenceType.AMPLIFICATION)
                        .build()))
                .build();

        ProtectEvidence evidence2 = ProtectTestFactory.builder()
                .gene("CDKN2A")
                .isCanonical(true)
                .event("amp")
                .germline(true)
                .reported(true)
                .treatment("TryMe")
                .onLabel(true)
                .level(EvidenceLevel.B)
                .direction(EvidenceDirection.RESPONSIVE)
                .sources(Sets.newHashSet(ImmutableProtectSource.builder()
                        .name(Knowledgebase.ICLUSION)
                        .sourceEvent(Strings.EMPTY)
                        .sourceUrls(Sets.newHashSet())
                        .evidenceType(ProtectEvidenceType.AMPLIFICATION)
                        .build()))
                .build();

        ProtectEvidence evidence3 = ProtectTestFactory.builder()
                .gene("CDKN2A")
                .isCanonical(false)
                .event("amp")
                .germline(true)
                .reported(true)
                .treatment("TryMe")
                .onLabel(true)
                .level(EvidenceLevel.B)
                .direction(EvidenceDirection.RESPONSIVE)
                .sources(Sets.newHashSet(ImmutableProtectSource.builder()
                        .name(Knowledgebase.ICLUSION)
                        .sourceEvent(Strings.EMPTY)
                        .sourceUrls(Sets.newHashSet())
                        .evidenceType(ProtectEvidenceType.AMPLIFICATION)
                        .build()))
                .build();
        return Lists.newArrayList(evidence1, evidence2, evidence3);
    }

    @NotNull
    public List<ReportableVariant> reportableVariants() {
        ReportableVariant somaticVariant1 =
                createTestReportableVariantBuilder().gene("KRAS").isCanonical(false).source(ReportableVariantSource.SOMATIC).build();
        ReportableVariant somaticVariant2 =
                createTestReportableVariantBuilder().gene("CDKN2A").isCanonical(true).source(ReportableVariantSource.SOMATIC).build();
        ReportableVariant germlineVariant1 =
                createTestReportableVariantBuilder().gene("CDKN2A").isCanonical(false).source(ReportableVariantSource.GERMLINE).build();
        return Lists.newArrayList(somaticVariant1, somaticVariant2, germlineVariant1);
    }

    @NotNull
    private static ImmutableReportableVariant.Builder createTestReportableVariantBuilder() {
        return ImmutableReportableVariant.builder().from(ReportableVariantTestFactory.create());
    }

    @NotNull
    public List<GainLoss> gainloss() {
        GainLoss gainLoss1 = ImmutableGainLoss.builder()
                .from(GainLossTestFactory.createTestGainLoss())
                .gene("BRAF")
                .isCanonical(true)
                .build();
        GainLoss gainLoss2 = ImmutableGainLoss.builder()
                .from(GainLossTestFactory.createTestGainLoss())
                .gene("CDKN2A")
                .isCanonical(true)
                .build();
        GainLoss gainLoss3 = ImmutableGainLoss.builder()
                .from(GainLossTestFactory.createTestGainLoss())
                .gene("CDKN2A")
                .isCanonical(false)
                .build();
        return Lists.newArrayList(gainLoss1, gainLoss2, gainLoss3);
    }

    @NotNull
    public List<GeneDisruption> geneDisruption() {
        GeneDisruption disruption1 = createTestReportableGeneDisruptionBuilder().gene("NRAS").isCanonical(true).build();
        GeneDisruption disruption2 = createTestReportableGeneDisruptionBuilder().gene("CDKN2A").isCanonical(true).build();
        GeneDisruption disruption3 = createTestReportableGeneDisruptionBuilder().gene("CDKN2A").isCanonical(false).build();
        return Lists.newArrayList(disruption1, disruption2, disruption3);
    }

    @NotNull
    private static ImmutableGeneDisruption.Builder createTestReportableGeneDisruptionBuilder() {
        return ImmutableGeneDisruption.builder()
                .location(Strings.EMPTY)
                .gene(Strings.EMPTY)
                .range(Strings.EMPTY)
                .type(Strings.EMPTY)
                .junctionCopyNumber(2.012)
                .undisruptedCopyNumber(0.0)
                .firstAffectedExon(5)
                .clusterId(2)
                .transcriptId(Strings.EMPTY);
    }

    @NotNull
    private static List<HomozygousDisruption> homozygousDisruptions() {
        HomozygousDisruption homozygousDisruption1 =
                createTestReportableHomozygousDisruptionBuilder().gene("NRAS").isCanonical(true).build();
        HomozygousDisruption homozygousDisruption2 =
                createTestReportableHomozygousDisruptionBuilder().gene("CDKN2A").isCanonical(true).build();
        HomozygousDisruption homozygousDisruption3 =
                createTestReportableHomozygousDisruptionBuilder().gene("CDKN2A").isCanonical(false).build();
        return Lists.newArrayList(homozygousDisruption1, homozygousDisruption2, homozygousDisruption3);
    }

    @NotNull
    private static ImmutableHomozygousDisruption.Builder createTestReportableHomozygousDisruptionBuilder() {
        return ImmutableHomozygousDisruption.builder()
                .chromosome(Strings.EMPTY)
                .chromosomeBand(Strings.EMPTY)
                .gene(Strings.EMPTY)
                .transcript(Strings.EMPTY);
    }
}