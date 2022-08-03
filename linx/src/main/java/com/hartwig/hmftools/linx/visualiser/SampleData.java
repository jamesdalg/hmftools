package com.hartwig.hmftools.linx.visualiser;

import static java.util.stream.Collectors.toList;

import static com.hartwig.hmftools.linx.LinxOutput.ITEM_DELIM;
import static com.hartwig.hmftools.linx.visualiser.SvVisualiser.VIS_LOGGER;
import static com.hartwig.hmftools.linx.visualiser.file.VisDataWriter.COHORT_VIS_COPY_NUMBER_FILE;
import static com.hartwig.hmftools.linx.visualiser.file.VisDataWriter.COHORT_VIS_FUSIONS_FILE;
import static com.hartwig.hmftools.linx.visualiser.file.VisDataWriter.COHORT_VIS_GENE_EXONS_FILE;
import static com.hartwig.hmftools.linx.visualiser.file.VisDataWriter.COHORT_VIS_PROTEIN_FILE;
import static com.hartwig.hmftools.linx.visualiser.file.VisDataWriter.COHORT_VIS_LINKS_FILE;
import static com.hartwig.hmftools.linx.visualiser.file.VisDataWriter.COHORT_VIS_SVS_FILE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache;
import com.hartwig.hmftools.common.gene.GeneData;
import com.hartwig.hmftools.common.gene.TranscriptData;
import com.hartwig.hmftools.common.sv.linx.LinxBreakend;
import com.hartwig.hmftools.common.sv.linx.LinxDriver;
import com.hartwig.hmftools.common.sv.linx.LinxSvAnnotation;
import com.hartwig.hmftools.linx.visualiser.data.VisCopyNumbers;
import com.hartwig.hmftools.linx.visualiser.data.VisExons;
import com.hartwig.hmftools.linx.visualiser.data.VisProteinDomains;
import com.hartwig.hmftools.linx.visualiser.data.VisSegments;
import com.hartwig.hmftools.linx.visualiser.file.VisCopyNumber;
import com.hartwig.hmftools.linx.visualiser.file.VisFusion;
import com.hartwig.hmftools.linx.visualiser.file.VisGeneExon;
import com.hartwig.hmftools.linx.visualiser.file.VisProteinDomain;
import com.hartwig.hmftools.linx.visualiser.file.VisSegment;
import com.hartwig.hmftools.linx.visualiser.file.VisSvData;

public class SampleData
{
    public final List<VisSegment> Segments;
    public final List<VisSvData> SvData;
    public final List<VisCopyNumber> CopyNumbers;
    public final List<VisProteinDomain> ProteinDomains;
    public final List<VisFusion> Fusions;
    public final List<VisGeneExon> Exons;

    private final VisualiserConfig mConfig;

    public SampleData(final VisualiserConfig config) throws Exception
    {
        mConfig = config;
        
        final String svDataFile = mConfig.UseCohortFiles ?
                mConfig.SampleDataDir + COHORT_VIS_SVS_FILE : VisSvData.generateFilename(mConfig.SampleDataDir, mConfig.Sample);

        final String linksFile = mConfig.UseCohortFiles ?
                mConfig.SampleDataDir + COHORT_VIS_LINKS_FILE : VisSegment.generateFilename(mConfig.SampleDataDir, mConfig.Sample);

        final String cnaFile = mConfig.UseCohortFiles ?
                mConfig.SampleDataDir + COHORT_VIS_COPY_NUMBER_FILE : VisCopyNumber.generateFilename(mConfig.SampleDataDir, mConfig.Sample);

        final String geneExonFile = mConfig.UseCohortFiles ?
                mConfig.SampleDataDir + COHORT_VIS_GENE_EXONS_FILE : VisGeneExon.generateFilename(mConfig.SampleDataDir, mConfig.Sample);

        final String proteinFile = mConfig.UseCohortFiles ?
                mConfig.SampleDataDir + COHORT_VIS_PROTEIN_FILE : VisProteinDomain.generateFilename(mConfig.SampleDataDir, mConfig.Sample);

        final String fusionFile = mConfig.UseCohortFiles ?
                mConfig.SampleDataDir + COHORT_VIS_FUSIONS_FILE : VisFusion.generateFilename(mConfig.SampleDataDir, mConfig.Sample);

        List<VisSvData> svData = VisSvData.read(svDataFile).stream().filter(x -> x.SampleId.equals(mConfig.Sample)).collect(toList());

        if(!mConfig.SpecificRegions.isEmpty())
        {
            List<VisSvData> svsInRegions = svData.stream()
                    .filter(x -> mConfig.SpecificRegions.stream()
                            .anyMatch(y -> y.containsPosition(x.ChrStart, x.PosStart) || y.containsPosition(x.ChrEnd, x.PosEnd)))
                    .collect(toList());

            if(mConfig.Clusters.isEmpty())
            {
                // if clusters have not been specified, then add these to the set to be plotted
                svsInRegions.forEach(x -> addClusterId(x.ClusterId));
            }
            else
            {
                // otherwise only show SVs for the specified clusters which are also in the specified regions
                svData = svsInRegions;
            }
        }

        if(!mConfig.Clusters.isEmpty())
        {
            svData = svData.stream().filter(x -> mConfig.Clusters.contains(x.ClusterId)).collect(toList());
        }

        SvData = svData;

        Fusions = loadFusions(fusionFile).stream().filter(x -> x.SampleId.equals(mConfig.Sample)).collect(toList());
        Exons = VisExons.readExons(geneExonFile).stream().filter(x -> x.SampleId.equals(mConfig.Sample)).collect(toList());
        Segments = VisSegments.readSegments(linksFile).stream().filter(x -> x.SampleId.equals(mConfig.Sample)).collect(toList());

        CopyNumbers = VisCopyNumbers.read(cnaFile)
                .stream().filter(x -> x.SampleId.equals(mConfig.Sample)).collect(toList());

        ProteinDomains = VisProteinDomains.readProteinDomains(proteinFile, Fusions).stream()
                .filter(x -> x.SampleId.equals(mConfig.Sample)).collect(toList());

        if(Segments.isEmpty() || SvData.isEmpty() || CopyNumbers.isEmpty())
        {
            VIS_LOGGER.warn("sample({}) empty segments, SVs or copy-number files", mConfig.Sample);
            return;
        }

        boolean loadSvData = !mConfig.UseCohortFiles
                && (mConfig.PlotClusterGenes || mConfig.RestrictClusterByGene || !mConfig.SpecificRegions.isEmpty());

        if(loadSvData)
        {
            final String svAnnotationsFile = LinxSvAnnotation.generateFilename(mConfig.SampleDataDir, mConfig.Sample, false);
            final String svAnnotationsFileGermline = LinxSvAnnotation.generateFilename(mConfig.SampleDataDir, mConfig.Sample, true);

            List<LinxSvAnnotation> svAnnotations = Files.exists(Paths.get(svAnnotationsFile)) ?
                    LinxSvAnnotation.read(svAnnotationsFile) : LinxSvAnnotation.read(svAnnotationsFileGermline);

            if(!mConfig.SpecificRegions.isEmpty())
            {
                svAnnotations = svAnnotations.stream().filter(x -> SvData.stream().anyMatch(y -> y.SvId == x.svId())).collect(toList());
            }

            if(mConfig.PlotClusterGenes && !mConfig.Clusters.isEmpty())
            {
                for(LinxSvAnnotation svAnnotation : svAnnotations)
                {
                    if(mConfig.Clusters.contains(svAnnotation.clusterId()))
                    {
                        if(!svAnnotation.geneStart().isEmpty())
                            Arrays.stream(svAnnotation.geneStart().split(ITEM_DELIM)).forEach(x -> mConfig.Genes.add(x));

                        if(!svAnnotation.geneEnd().isEmpty())
                            Arrays.stream(svAnnotation.geneEnd().split(ITEM_DELIM)).forEach(x -> mConfig.Genes.add(x));
                    }
                }
            }
            else if(!mConfig.Genes.isEmpty() && mConfig.Clusters.isEmpty() && mConfig.RestrictClusterByGene)
            {
                for(LinxSvAnnotation svAnnotation : svAnnotations)
                {
                    if(mConfig.Genes.stream().anyMatch(x -> x.equals(svAnnotation.geneStart()) || x.equals(svAnnotation.geneEnd())))
                    {
                        addClusterId(svAnnotation.clusterId());
                    }
                }

                List<LinxDriver> linxDrivers = LinxDriver.read(LinxDriver.generateFilename(mConfig.SampleDataDir, mConfig.Sample));

                for(LinxDriver linxDriver : linxDrivers)
                {
                    if(mConfig.Genes.stream().anyMatch(x -> x.equals(linxDriver.gene())))
                    {
                        addClusterId(linxDriver.clusterId());
                    }
                }
            }

            if(!mConfig.SpecificRegions.isEmpty() && mConfig.Clusters.isEmpty())
            {
                // limit to those clusters covered by an SV in the specific regions
                for(LinxSvAnnotation svAnnotation : svAnnotations)
                {
                    addClusterId(svAnnotation.clusterId());
                }
            }
        }

        Exons.addAll(additionalExons(mConfig, mConfig.Genes, Exons, mConfig.Clusters));
    }

    private void addClusterId(final int clusterId)
    {
        if(!mConfig.Clusters.contains(clusterId))
            mConfig.Clusters.add(clusterId);
    }

    public Set<Integer> findReportableClusters()
    {
        Set<Integer> clusterIds = Sets.newHashSet();

        Fusions.stream().forEach(x -> clusterIds.add(x.ClusterId));

        if(mConfig.SampleDataDir != null)
        {
            try
            {
                // reportable disruptions
                final List<LinxBreakend> breakends = LinxBreakend.read(LinxBreakend.generateFilename(mConfig.SampleDataDir, mConfig.Sample));

                final List<Integer> svIds = breakends.stream()
                        .filter(x -> x.reportedDisruption()).map(x -> x.svId()).collect(toList());

                for(Integer svId : svIds)
                {
                    VisSvData svData = SvData.stream().filter(x -> x.SvId == svId).findFirst().orElse(null);
                    if(svData != null)
                         clusterIds.add(svData.ClusterId);
                }

                final List<LinxDriver> drivers = LinxDriver.read(LinxDriver.generateFilename(mConfig.SampleDataDir, mConfig.Sample));
                drivers.stream().filter(x -> x.clusterId() >= 0).forEach(x -> clusterIds.add(x.clusterId()));
            }
            catch(Exception e)
            {
                VIS_LOGGER.error("sample({}) could not read breakends or drivers: {}", mConfig.Sample, e.toString());
            }
        }

        return clusterIds;
    }

    private List<VisFusion> loadFusions(final String fileName) throws IOException
    {
        if(!Files.exists(Paths.get(fileName)))
            return Lists.newArrayList();

        return VisFusion.read(fileName);
    }

    private static List<VisGeneExon> additionalExons(
            final VisualiserConfig config, final Set<String> geneList, final List<VisGeneExon> currentExons, final List<Integer> clusterIds)
    {
        final List<VisGeneExon> exonList = Lists.newArrayList();

        if(geneList.isEmpty())
            return exonList;

        if(config.EnsemblDataDir == null)
            return exonList;

        final List<Integer> allClusterIds = clusterIds.isEmpty() ? Lists.newArrayList(0) : clusterIds;

        EnsemblDataCache geneTransCache = new EnsemblDataCache(config.EnsemblDataDir, config.RefGenVersion);
        geneTransCache.setRequiredData(true, false, false, true);
        geneTransCache.load(false);

        for(final String geneName : geneList)
        {
            if(currentExons.stream().anyMatch(x -> x.Gene.equals(geneName) && clusterIds.contains(x.ClusterId)))
                continue;

            VIS_LOGGER.info("loading exon data for additional gene({})", geneName);

            GeneData geneData = geneTransCache.getGeneDataByName(geneName);
            TranscriptData transcriptData = geneData != null ? geneTransCache.getCanonicalTranscriptData(geneData.GeneId) : null;

            if(transcriptData == null)
            {
                VIS_LOGGER.warn("data not found for specified gene({})", geneName);
                continue;
            }

            for(Integer clusterId : allClusterIds)
            {
                exonList.addAll(VisExons.extractExonList(config.Sample, clusterId, geneData, transcriptData));
            }
        }

        return exonList;
    }
}
