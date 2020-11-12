package com.hartwig.hmftools.common.drivercatalog.panel;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.genome.bed.NamedBed;
import com.hartwig.hmftools.common.genome.bed.NamedBedFile;
import com.hartwig.hmftools.common.genome.genepanel.HmfExonPanelBed;
import com.hartwig.hmftools.common.genome.genepanel.HmfGenePanelSupplier;
import com.hartwig.hmftools.common.genome.region.HmfTranscriptRegion;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

public class DriverGenePanelConversion {

    public static void main(String[] args) throws IOException {
        String templateFile = "/Users/jon/hmf/resources/DriverGenePanel.template.txt";
        String outputFile19 = "/Users/jon/hmf/resources/DriverGenePanel.hg19.tsv";
        String outputFile38 = "/Users/jon/hmf/resources/DriverGenePanel.hg38.tsv";

        DndsGeneNameMap geneNameMap = new DndsGeneNameMap();
        List<DriverGene> inputDriverGenes = DriverGeneFile.read(templateFile);
        List<DriverGene> outputDriverGenes = Lists.newArrayList();
        for (DriverGene input : inputDriverGenes) {
            final String hg19Gene = input.gene();
            final String hg38Gene = geneNameMap.hg38Gene(input.gene());
            if (hg19Gene.equals("LINC00290") || hg19Gene.equals("LINC01001")) {
                outputDriverGenes.add(input);
            } else if (hg38Gene.equals("NA")) {
                System.out.println("Excluding: " + hg19Gene);
            } else {
                DriverGene converted = ImmutableDriverGene.builder().from(input).gene(hg38Gene).build();
                outputDriverGenes.add(converted);
            }
        }

        // Sort
        Collections.sort(inputDriverGenes);
        Collections.sort(outputDriverGenes);

        // Validate
        DriverGenePanelFactory.create(DriverGenePanelAssembly.HG19, inputDriverGenes);
        DriverGenePanelFactory.create(DriverGenePanelAssembly.HG38, outputDriverGenes);

        // Write out driver gene panel
        DriverGeneFile.write(outputFile19, inputDriverGenes);
        DriverGeneFile.write(outputFile38, outputDriverGenes);

        // Write out actionable bed files
        final List<HmfTranscriptRegion> hg19Transcripts = HmfGenePanelSupplier.allGeneList37();
        final List<HmfTranscriptRegion> hg38Transcripts = HmfGenePanelSupplier.allGeneList38();
        createBedFiles("/Users/jon/hmf/resources/ActionableCodingPanel.somatic.hg19.bed", somaticGenes(inputDriverGenes), hg19Transcripts);
        createBedFiles("/Users/jon/hmf/resources/ActionableCodingPanel.germline.hg19.bed", germlineGenes(inputDriverGenes), hg19Transcripts);
        createBedFiles("/Users/jon/hmf/resources/ActionableCodingPanel.somatic.hg38.bed", somaticGenes(inputDriverGenes), hg38Transcripts);
        createBedFiles("/Users/jon/hmf/resources/ActionableCodingPanel.germline.hg38.bed", germlineGenes(inputDriverGenes), hg38Transcripts);
    }

    private static void createBedFiles(String file, Set<String> genes, List<HmfTranscriptRegion> transcripts) throws IOException {
        final List<NamedBed> somaticBed = HmfExonPanelBed.createRegions(genes, transcripts);
        NamedBedFile.toBedFile(file, somaticBed);
    }

    @NotNull
    static Set<String> somaticGenes(@NotNull final List<DriverGene> genePanel) {
        final Set<String> actionableGenes = Sets.newHashSet();
        for (DriverGene driverGene : genePanel) {
            if (driverGene.reportVariant()) {
                actionableGenes.add(driverGene.gene());
            }
        }

        return actionableGenes;
    }

    @NotNull
    static Set<String> germlineGenes(@NotNull final List<DriverGene> genePanel) {
        final Set<String> actionableGenes = Sets.newHashSet();
        for (DriverGene driverGene : genePanel) {
            if (driverGene.reportGermlineBiallelic() || driverGene.reportGermlineNonBiallelic()) {
                actionableGenes.add(driverGene.gene());
            }

        }

        return actionableGenes;
    }

}
