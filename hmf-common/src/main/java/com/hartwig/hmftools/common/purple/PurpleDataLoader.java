package com.hartwig.hmftools.common.purple;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hartwig.hmftools.common.drivercatalog.CNADrivers;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalogFile;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalogKey;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalogMap;
import com.hartwig.hmftools.common.drivercatalog.DriverCategory;
import com.hartwig.hmftools.common.drivercatalog.DriverType;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGene;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGeneGermlineReporting;
import com.hartwig.hmftools.common.drivercatalog.panel.ImmutableDriverGene;
import com.hartwig.hmftools.common.drivercatalog.panel.ImmutableDriverGenePanel;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeCoordinates;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;
import com.hartwig.hmftools.common.purple.cnchromosome.CnPerChromosomeArmData;
import com.hartwig.hmftools.common.purple.cnchromosome.CnPerChromosomeFactory;
import com.hartwig.hmftools.common.purple.gene.GeneCopyNumber;
import com.hartwig.hmftools.common.purple.gene.GeneCopyNumberFile;
import com.hartwig.hmftools.common.purple.gene.GermlineDeletion;
import com.hartwig.hmftools.common.purple.interpretation.CopyNumberInterpretation;
import com.hartwig.hmftools.common.purple.interpretation.GainLoss;
import com.hartwig.hmftools.common.purple.interpretation.ImmutableGainLoss;
import com.hartwig.hmftools.common.purple.purity.PurityContext;
import com.hartwig.hmftools.common.purple.purity.PurityContextFile;
import com.hartwig.hmftools.common.variant.ReportableVariant;
import com.hartwig.hmftools.common.variant.ReportableVariantFactory;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.SomaticVariantFactory;

import org.apache.commons.compress.utils.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PurpleDataLoader {

    private static final Logger LOGGER = LogManager.getLogger(PurpleDataLoader.class);

    private PurpleDataLoader() {
    }

    @NotNull
    public static PurpleData load(@NotNull String tumorSample, @Nullable String referenceSample, @Nullable String rnaSample,
            @NotNull String qcFile, @NotNull String purityTsv, @NotNull String somaticDriverCatalogTsv, @NotNull String somaticVariantVcf,
            @NotNull String germlineDriverCatalogTsv, @NotNull String germlineVariantVcf, @Nullable String purpleGeneCopyNumberTsv,
            @Nullable String purpleSomaticCopyNumberTsv, @Nullable String purpleGermlineDeletionTsv,
            @Nullable RefGenomeVersion refGenomeVersion) throws IOException {
        LOGGER.info("Loading PURPLE data from {}", new File(purityTsv).getParent());

        PurityContext purityContext = readPurityContext(qcFile, purityTsv);

        List<DriverCatalog> somaticDriverCatalog = DriverCatalogFile.read(somaticDriverCatalogTsv);
        LOGGER.info(" Loaded {} somatic driver catalog entries from {}", somaticDriverCatalog.size(), somaticDriverCatalogTsv);

        List<GainLoss> reportableSomaticGainsLosses = somaticGainsLossesFromDrivers(somaticDriverCatalog);
        LOGGER.info("  Extracted {} reportable somatic gains and losses from driver catalog", reportableSomaticGainsLosses.size());

        List<GeneCopyNumber> allSomaticGeneCopyNumbers = Lists.newArrayList();
        List<GainLoss> allSomaticGainsLosses = Lists.newArrayList();
        if (purpleGeneCopyNumberTsv != null) {
            allSomaticGeneCopyNumbers = GeneCopyNumberFile.read(purpleGeneCopyNumberTsv);
            LOGGER.debug(" Loaded {} gene copy numbers entries from {}", allSomaticGeneCopyNumbers.size(), purpleGeneCopyNumberTsv);

            allSomaticGainsLosses =
                    extractAllGainsLosses(purityContext.qc().status(), purityContext.bestFit().ploidy(), allSomaticGeneCopyNumbers);
            LOGGER.info("  Extracted {} somatic gains and losses from gene copy numbers", allSomaticGainsLosses.size());
        }

        List<CnPerChromosomeArmData> copyNumberPerChromosome = Lists.newArrayList();
        if (purpleSomaticCopyNumberTsv != null && refGenomeVersion != null) {
            RefGenomeCoordinates refGenomeCoordinates =
                    refGenomeVersion == RefGenomeVersion.V37 ? RefGenomeCoordinates.COORDS_37 : RefGenomeCoordinates.COORDS_38;
            copyNumberPerChromosome = CnPerChromosomeFactory.generate(purpleSomaticCopyNumberTsv, refGenomeCoordinates);
            LOGGER.debug(" Generated chromosomal arm copy numbers from {}", purpleSomaticCopyNumberTsv);
        }

        List<SomaticVariant> allGermlineVariants = Lists.newArrayList();
        List<ReportableVariant> reportableGermlineVariants = Lists.newArrayList();
        if (referenceSample != null) {
            List<DriverCatalog> germlineDriverCatalog = DriverCatalogFile.read(germlineDriverCatalogTsv);
            LOGGER.info(" Loaded {} germline driver catalog entries from {}", germlineDriverCatalog.size(), germlineDriverCatalogTsv);

            /// TODO Pass RNA sample once germline variants can be RNA-annotated.
            allGermlineVariants = new SomaticVariantFactory().fromVCFFile(tumorSample, referenceSample, germlineVariantVcf);
            reportableGermlineVariants = ReportableVariantFactory.toReportableGermlineVariants(allGermlineVariants, germlineDriverCatalog);
            LOGGER.info(" Loaded {} germline variants (of which {} are reportable) from {}",
                    allGermlineVariants.size(),
                    reportableGermlineVariants.size(),
                    germlineVariantVcf);
        } else {
            LOGGER.debug(" Skipped loading germline variants since no reference sample configured");
        }

        List<GermlineDeletion> allGermlineDeletions = Lists.newArrayList();
        List<GermlineDeletion> reportableGermlineDeletions = Lists.newArrayList();
        if (purpleGermlineDeletionTsv != null) {
            allGermlineDeletions = GermlineDeletion.read(purpleGermlineDeletionTsv);
            reportableGermlineDeletions = selectReportedDeletions(allGermlineDeletions);

            LOGGER.info(" Loaded {} germline deletions (of which {} are reportable) from {}",
                    allGermlineDeletions.size(),
                    reportableGermlineDeletions.size(),
                    purpleGermlineDeletionTsv);
        }

        List<SomaticVariant> allSomaticVariants =
                SomaticVariantFactory.passOnlyInstance().fromVCFFile(tumorSample, referenceSample, rnaSample, somaticVariantVcf);
        List<ReportableVariant> reportableSomaticVariants =
                ReportableVariantFactory.toReportableSomaticVariants(allSomaticVariants, somaticDriverCatalog);
        LOGGER.info(" Loaded {} somatic variants (of which {} are reportable) from {}",
                allSomaticVariants.size(),
                reportableSomaticVariants.size(),
                somaticVariantVcf);

        return ImmutablePurpleData.builder()
                .qc(purityContext.qc())
                .hasReliableQuality(purityContext.qc().pass())
                .fittedPurityMethod(purityContext.method())
                .hasReliablePurity(CheckPurpleQuality.checkHasReliablePurity(purityContext))
                .purity(purityContext.bestFit().purity())
                .minPurity(purityContext.score().minPurity())
                .maxPurity(purityContext.score().maxPurity())
                .ploidy(purityContext.bestFit().ploidy())
                .minPloidy(purityContext.score().minPloidy())
                .maxPloidy(purityContext.score().maxPloidy())
                .wholeGenomeDuplication(purityContext.wholeGenomeDuplication())
                .microsatelliteIndelsPerMb(purityContext.microsatelliteIndelsPerMb())
                .microsatelliteStatus(purityContext.microsatelliteStatus())
                .tumorMutationalBurdenPerMb(purityContext.tumorMutationalBurdenPerMb())
                .tumorMutationalLoad(purityContext.tumorMutationalLoad())
                .tumorMutationalLoadStatus(purityContext.tumorMutationalLoadStatus())
                .svTumorMutationalBurden(purityContext.svTumorMutationalBurden())
                .allSomaticVariants(allSomaticVariants)
                .reportableSomaticVariants(reportableSomaticVariants)
                .allGermlineVariants(allGermlineVariants)
                .reportableGermlineVariants(reportableGermlineVariants)
                .allSomaticGeneCopyNumbers(allSomaticGeneCopyNumbers)
                .allSomaticGainsLosses(allSomaticGainsLosses)
                .reportableSomaticGainsLosses(reportableSomaticGainsLosses)
                .allGermlineDeletions(allGermlineDeletions)
                .reportableGermlineDeletions(reportableGermlineDeletions)
                .copyNumberPerChromosome(copyNumberPerChromosome)
                .build();
    }

    @NotNull
    private static PurityContext readPurityContext(@NotNull String qcFile, @NotNull String purityTsv) throws IOException {
        PurityContext purityContext = PurityContextFile.readWithQC(qcFile, purityTsv);

        DecimalFormat purityFormat = new DecimalFormat("#'%'");
        LOGGER.info("  QC status: {}", purityContext.qc().toString());
        LOGGER.info("  Tumor purity: {} ({}-{})",
                purityFormat.format(purityContext.bestFit().purity() * 100),
                purityFormat.format(purityContext.score().minPurity() * 100),
                purityFormat.format(purityContext.score().maxPurity() * 100));
        LOGGER.info("  Tumor ploidy: {} ({}-{})",
                purityContext.bestFit().ploidy(),
                purityContext.score().minPloidy(),
                purityContext.score().maxPloidy());
        LOGGER.info("  Fit method: {}", purityContext.method());
        LOGGER.info("  Whole genome duplication: {}", purityContext.wholeGenomeDuplication() ? "yes" : "no");
        LOGGER.info("  Microsatellite status: {}", purityContext.microsatelliteStatus().display());
        LOGGER.info("  Tumor mutational load status: {}", purityContext.tumorMutationalLoadStatus().display());

        return purityContext;
    }

    @NotNull
    private static List<GainLoss> somaticGainsLossesFromDrivers(@NotNull List<DriverCatalog> drivers) {
        List<GainLoss> gainsLosses = Lists.newArrayList();

        Map<DriverCatalogKey, DriverCatalog> geneDriverMap = DriverCatalogMap.toDriverMap(drivers);
        for (DriverCatalogKey key : geneDriverMap.keySet()) {
            DriverCatalog geneDriver = geneDriverMap.get(key);

            if (geneDriver.driver() == DriverType.AMP || geneDriver.driver() == DriverType.PARTIAL_AMP
                    || geneDriver.driver() == DriverType.DEL) {
                gainsLosses.add(toGainLoss(geneDriver));
            }
        }
        return gainsLosses;
    }

    @NotNull
    private static List<GainLoss> extractAllGainsLosses(@NotNull Set<PurpleQCStatus> qcStatus, double ploidy,
            @NotNull List<GeneCopyNumber> geneCopyNumbers) {
        List<DriverGene> allGenes = Lists.newArrayList();
        for (GeneCopyNumber geneCopyNumber : geneCopyNumbers) {
            allGenes.add(ImmutableDriverGene.builder()
                    .gene(geneCopyNumber.geneName())
                    .reportMissenseAndInframe(false)
                    .reportNonsenseAndFrameshift(false)
                    .reportSplice(false)
                    .reportDeletion(true)
                    .reportDisruption(false)
                    .reportAmplification(true)
                    .reportSomaticHotspot(false)
                    .reportGermlineVariant(DriverGeneGermlineReporting.NONE)
                    .reportGermlineHotspot(DriverGeneGermlineReporting.NONE)
                    .reportGermlineDisruption(false)
                    .likelihoodType(DriverCategory.ONCO)
                    .build());
        }
        CNADrivers drivers = new CNADrivers(qcStatus, ImmutableDriverGenePanel.builder().driverGenes(allGenes).build());

        List<DriverCatalog> allGainLosses = Lists.newArrayList();
        allGainLosses.addAll(drivers.amplifications(ploidy, geneCopyNumbers));
        allGainLosses.addAll(drivers.deletions(geneCopyNumbers));

        return somaticGainsLossesFromDrivers(allGainLosses);
    }

    @NotNull
    private static GainLoss toGainLoss(@NotNull DriverCatalog driver) {
        return ImmutableGainLoss.builder()
                .chromosome(driver.chromosome())
                .chromosomeBand(driver.chromosomeBand())
                .gene(driver.gene())
                .transcript(driver.transcript())
                .isCanonical(driver.isCanonical())
                .interpretation(CopyNumberInterpretation.fromCNADriver(driver))
                .minCopies(Math.round(Math.max(0, driver.minCopyNumber())))
                .maxCopies(Math.round(Math.max(0, driver.maxCopyNumber())))
                .build();
    }

    @NotNull
    private static List<GermlineDeletion> selectReportedDeletions(@NotNull List<GermlineDeletion> allGermlineDeletions) {
        List<GermlineDeletion> reported = Lists.newArrayList();
        for (GermlineDeletion deletion : allGermlineDeletions) {
            if (deletion.Reported) {
                reported.add(deletion);
            }
        }
        return reported;
    }
}
