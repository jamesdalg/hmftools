package com.hartwig.hmftools.orange.report.chapters;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.chord.ChordAnalysis;
import com.hartwig.hmftools.common.chord.ChordStatus;
import com.hartwig.hmftools.common.cuppa.CuppaData;
import com.hartwig.hmftools.common.doid.DoidNode;
import com.hartwig.hmftools.common.linx.ReportableHomozygousDisruption;
import com.hartwig.hmftools.common.protect.ProtectEvidence;
import com.hartwig.hmftools.common.purple.PurpleData;
import com.hartwig.hmftools.common.purple.PurpleQCStatus;
import com.hartwig.hmftools.common.purple.copynumber.ReportableGainLoss;
import com.hartwig.hmftools.common.serve.actionability.EvidenceLevel;
import com.hartwig.hmftools.common.sv.linx.LinxFusion;
import com.hartwig.hmftools.common.variant.DriverInterpretation;
import com.hartwig.hmftools.common.variant.ReportableVariant;
import com.hartwig.hmftools.common.virus.AnnotatedVirus;
import com.hartwig.hmftools.orange.algo.OrangeReport;
import com.hartwig.hmftools.orange.cohort.datamodel.Evaluation;
import com.hartwig.hmftools.orange.cohort.mapping.CohortConstants;
import com.hartwig.hmftools.orange.cohort.percentile.PercentileType;
import com.hartwig.hmftools.orange.report.ReportResources;
import com.hartwig.hmftools.orange.report.util.CellUtil;
import com.hartwig.hmftools.orange.report.util.ImageUtil;
import com.hartwig.hmftools.orange.report.util.TableUtil;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.UnitValue;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public class FrontPageChapter implements ReportChapter {

    private static final DecimalFormat SINGLE_DIGIT = ReportResources.decimalFormat("#.#");
    private static final DecimalFormat TWO_DIGITS = ReportResources.decimalFormat("#.##");
    private static final DecimalFormat PERCENTAGE = ReportResources.decimalFormat("#'%'");

    private static final String NONE = "None";

    @NotNull
    private final OrangeReport report;
    private final boolean reportGermline;

    public FrontPageChapter(@NotNull final OrangeReport report, final boolean reportGermline) {
        this.report = report;
        this.reportGermline = reportGermline;
    }

    @NotNull
    @Override
    public String name() {
        return "Front Page";
    }

    @NotNull
    @Override
    public PageSize pageSize() {
        return PageSize.A4;
    }

    @Override
    public void render(@NotNull Document document) {
        addSummaryTable(document);
        addDetailsAndPlots(document);
    }

    private void addSummaryTable(@NotNull Document document) {
        Table table = TableUtil.createContent(contentWidth(),
                new float[] { 3, 2, 1 },
                new Cell[] { CellUtil.createHeader("Configured Primary Tumor"), CellUtil.createHeader("Cuppa Cancer Type"),
                        CellUtil.createHeader("QC") });

        table.addCell(CellUtil.createContent(toConfiguredPrimaryTumor(report.configuredPrimaryTumor())));
        table.addCell(CellUtil.createContent(toCuppaCancerType(report.cuppa())));
        table.addCell(CellUtil.createContent(purpleQCString()));
        document.add(TableUtil.createWrapping(table));
    }

    @NotNull
    private static String toCuppaCancerType(@NotNull CuppaData cuppa) {
        return cuppa.predictedCancerType() + " (" + PERCENTAGE.format(cuppa.bestPredictionLikelihood() * 100) + ")";
    }

    @NotNull
    private static String toConfiguredPrimaryTumor(@NotNull Set<DoidNode> nodes) {
        StringJoiner joiner = new StringJoiner(", ");

        for (DoidNode node : nodes) {
            joiner.add(node.doidTerm() + " (DOID " + node.doid() + ")");
        }

        return joiner.toString();
    }

    @NotNull
    private String purpleQCString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (PurpleQCStatus status : report.purple().qc().status()) {
            joiner.add(status.toString());
        }
        return joiner.toString();
    }

    private void addDetailsAndPlots(@NotNull Document document) {
        Table topTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 })).setWidth(contentWidth() - 5);

        Table summary = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }));
        summary.addCell(CellUtil.createKey("Purity:"));
        summary.addCell(CellUtil.createValue(purityString()));
        summary.addCell(CellUtil.createKey("Ploidy:"));
        summary.addCell(CellUtil.createValue(ploidyString()));
        summary.addCell(CellUtil.createKey("Somatic variant drivers:"));
        summary.addCell(CellUtil.createValue(somaticDriverString()));
        summary.addCell(CellUtil.createKey("Germline variant drivers:"));
        summary.addCell(CellUtil.createValue(germlineDriverString()));
        summary.addCell(CellUtil.createKey("Copy number drivers:"));
        summary.addCell(CellUtil.createValue(copyNumberDriverString()));
        summary.addCell(CellUtil.createKey("Disruption drivers:"));
        summary.addCell(CellUtil.createValue(disruptionDriverString()));
        summary.addCell(CellUtil.createKey("Fusion drivers:"));
        summary.addCell(CellUtil.createValue(fusionDriverString()));
        summary.addCell(CellUtil.createKey("Viral presence:"));
        summary.addCell(CellUtil.createValue(virusString()));
        summary.addCell(CellUtil.createKey("Whole genome duplicated:"));
        summary.addCell(CellUtil.createValue(report.purple().wholeGenomeDuplication() ? "Yes" : "No"));
        summary.addCell(CellUtil.createKey("Microsatellite indels per Mb:"));
        summary.addCell(CellUtil.createValue(msiString()));
        summary.addCell(CellUtil.createKey("Tumor mutations per Mb:"));
        summary.addCell(CellUtil.createValue(SINGLE_DIGIT.format(report.purple().tumorMutationalBurdenPerMb())));
        summary.addCell(CellUtil.createKey("Tumor mutational load:"));
        summary.addCell(CellUtil.createValue(tmlString()));
        summary.addCell(CellUtil.createKey("HR deficiency score:"));
        summary.addCell(CellUtil.createValue(hrDeficiencyString()));
        summary.addCell(CellUtil.createKey("Number of SVs:"));
        summary.addCell(CellUtil.createValue(svTmbString()));
        summary.addCell(CellUtil.createKey("Max complex cluster size:"));
        summary.addCell(CellUtil.createValue(Integer.toString(report.cuppa().maxComplexSize())));
        summary.addCell(CellUtil.createKey("Telomeric SGLs:"));
        summary.addCell(CellUtil.createValue(Integer.toString(report.cuppa().telomericSGLs())));
        summary.addCell(CellUtil.createKey("Number of LINE insertions:"));
        summary.addCell(CellUtil.createValue(Integer.toString(report.cuppa().LINECount())));
        summary.addCell(CellUtil.createKey("On-label treatments:"));
        summary.addCell(CellUtil.createValue(onLabelTreatmentString()));
        summary.addCell(CellUtil.createKey("Off-label treatments:"));
        summary.addCell(CellUtil.createValue(offLabelTreatmentString()));

        Image circosImage = ImageUtil.build(report.plots().purpleFinalCircosPlot());
        circosImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
        circosImage.setMaxHeight(280);

        topTable.addCell(summary);
        topTable.addCell(circosImage);

        Table table = new Table(UnitValue.createPercentArray(new float[] { 1 })).setWidth(contentWidth()).setPadding(0);
        table.addCell(topTable);

        Image clonalityImage = ImageUtil.build(report.plots().purpleClonalityPlot());
        clonalityImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
        clonalityImage.setMaxHeight(280);

        table.addCell(clonalityImage);
        document.add(table);
    }

    @NotNull
    private String purityString() {
        return String.format("%s (%s-%s)",
                PERCENTAGE.format(report.purple().purity() * 100),
                PERCENTAGE.format(report.purple().minPurity() * 100),
                PERCENTAGE.format(report.purple().maxPurity() * 100));
    }

    @NotNull
    private String ploidyString() {
        return String.format("%s (%s-%s)",
                TWO_DIGITS.format(report.purple().ploidy()),
                TWO_DIGITS.format(report.purple().minPloidy()),
                TWO_DIGITS.format(report.purple().maxPloidy()));
    }

    @NotNull
    private String somaticDriverString() {
        return variantDriverString(report.purple().reportableSomaticVariants());
    }

    @NotNull
    private String germlineDriverString() {
        if (reportGermline) {
            return variantDriverString(report.purple().reportableGermlineVariants());
        } else {
            return ReportResources.NOT_AVAILABLE;
        }
    }

    @NotNull
    private static String variantDriverString(@NotNull List<ReportableVariant> variants) {
        if (variants.isEmpty()) {
            return NONE;
        } else {
            Set<String> highDriverGenes = Sets.newTreeSet(Comparator.naturalOrder());
            for (ReportableVariant variant : variants) {
                if (variant.driverLikelihoodInterpretation() == DriverInterpretation.HIGH) {
                    highDriverGenes.add(variant.gene());
                }
            }

            if (highDriverGenes.isEmpty()) {
                return String.valueOf(variants.size());
            } else {
                StringJoiner joiner = new StringJoiner(", ");
                for (String gene : highDriverGenes) {
                    joiner.add(gene);
                }

                return variants.size() + " (" + joiner.toString() + ")";
            }
        }
    }

    @NotNull
    private String copyNumberDriverString() {
        if (report.purple().reportableGainsLosses().isEmpty()) {
            return NONE;
        } else {
            StringJoiner joiner = new StringJoiner(", ");
            for (ReportableGainLoss gainLoss : report.purple().reportableGainsLosses()) {
                joiner.add(gainLoss.gene());
            }
            return report.purple().reportableGainsLosses().size() + " (" + joiner.toString() + ")";
        }
    }

    @NotNull
    private String disruptionDriverString() {
        if (report.linx().homozygousDisruptions().isEmpty()) {
            return NONE;
        } else {
            StringJoiner joiner = new StringJoiner(", ");
            for (ReportableHomozygousDisruption disruption : report.linx().homozygousDisruptions()) {
                joiner.add(disruption.gene());
            }
            return report.linx().homozygousDisruptions().size() + " (" + joiner.toString() + ")";
        }
    }

    @NotNull
    private String fusionDriverString() {
        if (report.linx().reportableFusions().isEmpty()) {
            return NONE;
        } else {
            StringJoiner joiner = new StringJoiner(", ");
            for (LinxFusion fusion : report.linx().reportableFusions()) {
                joiner.add(fusion.name());
            }
            return report.linx().reportableFusions().size() + " (" + joiner.toString() + ")";
        }
    }

    @NotNull
    private String virusString() {
        if (report.virusInterpreter().reportableViruses().isEmpty()) {
            return NONE;
        } else {
            Set<String> viruses = Sets.newTreeSet(Comparator.naturalOrder());
            for (AnnotatedVirus virus : report.virusInterpreter().reportableViruses()) {
                if (virus.interpretation() != null) {
                    viruses.add(virus.interpretation());
                } else {
                    viruses.add(virus.name());
                }
            }

            StringJoiner joiner = new StringJoiner(", ");
            for (String virus : viruses) {
                joiner.add(virus);
            }
            return report.virusInterpreter().reportableViruses().size() + " (" + joiner.toString() + ")";
        }
    }

    @NotNull
    private String msiString() {
        PurpleData purple = report.purple();
        return SINGLE_DIGIT.format(purple.microsatelliteIndelsPerMb()) + " (" + purple.microsatelliteStatus().display() + ")";
    }

    @NotNull
    private String tmlString() {
        PurpleData purple = report.purple();
        return purple.tumorMutationalLoad() + " (" + purple.tumorMutationalLoadStatus().display() + ")";
    }

    @NotNull
    private String hrDeficiencyString() {
        ChordAnalysis chord = report.chord();
        if (chord.hrStatus() == ChordStatus.CANNOT_BE_DETERMINED) {
            return ChordStatus.CANNOT_BE_DETERMINED.display();
        } else {
            String addon = Strings.EMPTY;
            if (chord.hrStatus() == ChordStatus.HR_DEFICIENT) {
                if (chord.hrdType().contains("BRCA1")) {
                    addon = " - BRCA1 (" + TWO_DIGITS.format(chord.BRCA1Value()) + ")";
                } else if (chord.hrdType().contains("BRCA2")) {
                    addon = " - BRCA2 (" + TWO_DIGITS.format(chord.BRCA2Value()) + ")";
                } else {
                    addon = chord.hrdType();
                }
            }
            return SINGLE_DIGIT.format(chord.hrdValue()) + " (" + chord.hrStatus().display() + addon + ")";
        }
    }

    @NotNull
    private String svTmbString() {
        String svTmb = String.valueOf(report.purple().svTumorMutationalBurden());

        Evaluation evaluation = report.cohortEvaluations().get(PercentileType.SV_TMB);
        String addon = Strings.EMPTY;
        if (evaluation != null) {
            String panCancerPercentile = PERCENTAGE.format(evaluation.panCancerPercentile() * 100);
            addon = " (Pan " + panCancerPercentile;
            if (!evaluation.cancerType().equals(CohortConstants.COHORT_OTHER) && !evaluation.cancerType()
                    .equals(CohortConstants.COHORT_UNKNOWN)) {
                String cancerTypePercentile = PERCENTAGE.format(evaluation.cancerTypePercentile() * 100);
                addon = addon + " | " + evaluation.cancerType() + " " + cancerTypePercentile;
            }
            addon = addon + ")";
        }

        return svTmb + addon;
    }

    @NotNull
    private String onLabelTreatmentString() {
        return treatmentString(report.protect(), true, reportGermline);
    }

    @NotNull
    private String offLabelTreatmentString() {
        return treatmentString(report.protect(), false, reportGermline);
    }

    @NotNull
    private static String treatmentString(@NotNull List<ProtectEvidence> evidences, boolean requireOnLabel, boolean reportGermline) {
        Set<EvidenceLevel> levels = Sets.newTreeSet(Comparator.naturalOrder());
        Set<String> treatments = Sets.newHashSet();
        for (ProtectEvidence evidence : evidences) {
            if (evidence.onLabel() == requireOnLabel && (reportGermline || !evidence.germline())) {
                treatments.add(evidence.treatment());
                levels.add(evidence.level());
            }
        }

        if (treatments.isEmpty()) {
            return NONE;
        } else {
            StringJoiner joiner = new StringJoiner(", ");
            for (EvidenceLevel level : levels) {
                joiner.add(level.toString());
            }

            return treatments.size() + " (" + joiner.toString() + ")";
        }
    }
}
