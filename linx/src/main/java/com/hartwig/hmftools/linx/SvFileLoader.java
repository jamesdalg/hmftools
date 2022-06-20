package com.hartwig.hmftools.linx;

import static com.hartwig.hmftools.common.sv.StructuralVariantData.convertSvData;
import static com.hartwig.hmftools.common.sv.StructuralVariantFactory.INFERRED;
import static com.hartwig.hmftools.common.sv.StructuralVariantFactory.PASS;
import static com.hartwig.hmftools.common.sv.StructuralVariantFactory.PON_COUNT;
import static com.hartwig.hmftools.linx.LinxConfig.LNX_LOGGER;
import static com.hartwig.hmftools.patientdb.dao.DatabaseUtil.valueNotNull;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.filter.AlwaysPassFilter;
import com.hartwig.hmftools.common.sv.EnrichedStructuralVariant;
import com.hartwig.hmftools.common.sv.EnrichedStructuralVariantFactory;
import com.hartwig.hmftools.common.sv.ImmutableStructuralVariantData;
import com.hartwig.hmftools.common.sv.StructuralVariant;
import com.hartwig.hmftools.common.sv.StructuralVariantData;
import com.hartwig.hmftools.common.sv.StructuralVariantFile;
import com.hartwig.hmftools.common.sv.StructuralVariantFileLoader;
import com.hartwig.hmftools.linx.germline.GermlineFilter;
import com.hartwig.hmftools.linx.types.SvVarData;
import com.hartwig.hmftools.patientdb.dao.DatabaseUtil;

import org.apache.commons.cli.CommandLine;

public final class SvFileLoader
{
    public static List<StructuralVariantData> loadSampleSvDataFromFile(
            final LinxConfig config, final String sampleId, final CommandLine cmd)
    {
        String vcfFile = config.SvVcfFile.contains("*") ? config.SvVcfFile.replaceAll("\\*", sampleId) : config.SvVcfFile;

        if(config.IsGermline)
            return loadSvDataFromGermlineVcf(vcfFile);
        else
            return loadSvDataFromVcf(vcfFile);
    }

    private static List<StructuralVariantData> loadSvDataFromVcf(final String vcfFile)
    {
        final List<StructuralVariantData> svDataList = Lists.newArrayList();

        try
        {
            final List<StructuralVariant> variants = StructuralVariantFileLoader.fromFile(vcfFile, new AlwaysPassFilter());
            final List<EnrichedStructuralVariant> enrichedVariants = new EnrichedStructuralVariantFactory().enrich(variants);

            // generate a unique ID for each SV record
            int svId = 0;

            for (EnrichedStructuralVariant var : enrichedVariants)
            {
                svDataList.add(convertSvData(var, svId++));
            }

            LNX_LOGGER.info("loaded {} SV data records from VCF file: {}", svDataList.size(), vcfFile);
        }
        catch(IOException e)
        {
            LNX_LOGGER.error("failed to load SVs from VCF: {}", e.toString());
        }

        return svDataList;
    }

    private static List<StructuralVariantData> loadSvDataFromGermlineVcf(final String vcfFile)
    {
        final List<StructuralVariantData> svDataList = Lists.newArrayList();

        try
        {
            final List<StructuralVariant> variants = StructuralVariantFileLoader.fromFile(vcfFile, new GermlineFilter());

            int svId = 0;

            for (StructuralVariant var : variants)
            {
                svDataList.add(convertGermlineSvData(var, svId++));
            }

            LNX_LOGGER.info("loaded {} germline SV data records from VCF file: {}", svDataList.size(), vcfFile);
        }
        catch(Exception e)
        {
            LNX_LOGGER.error("failed to load SVs from VCF: {}", e.toString());
        }

        return svDataList;
    }

    public static List<SvVarData> createSvData(final List<StructuralVariantData> svRecords, final LinxConfig config)
    {
        List<SvVarData> svDataItems = Lists.newArrayList();

        for (final StructuralVariantData svRecord : svRecords)
        {
            final String filter = svRecord.filter();

            if(filter.isEmpty() || filter.equals(PASS) || filter.equals(INFERRED) || config.IsGermline)
            {
                svDataItems.add(new SvVarData(svRecord));
            }
        }

        return svDataItems;
    }

    public static StructuralVariantData convertGermlineSvData(final StructuralVariant var, int svId)
    {
        return ImmutableStructuralVariantData.builder()
                .id(svId)
                .startChromosome(var.chromosome(true))
                .endChromosome(var.end() == null ? "0" : var.chromosome(false))
                .startPosition(var.position(true).intValue())
                .endPosition(var.end() == null ? -1 : var.position(false).intValue())
                .startOrientation(var.orientation(true))
                .endOrientation(var.end() == null ? (byte) 0 : var.orientation(false))
                .startHomologySequence(var.start().homology())
                .endHomologySequence(var.end() == null ? "" : var.end().homology())
                .junctionCopyNumber(1)
                .startAF(DatabaseUtil.valueNotNull(var.start().alleleFrequency()))
                .endAF(var.end() == null ? 0 : DatabaseUtil.valueNotNull(var.end().alleleFrequency()))
                .adjustedStartAF(DatabaseUtil.valueNotNull(var.start().alleleFrequency()))
                .adjustedEndAF(var.end() == null ? 0 : DatabaseUtil.valueNotNull(var.end().alleleFrequency()))
                .adjustedStartCopyNumber(DatabaseUtil.valueNotNull(1))
                .adjustedEndCopyNumber(var.end() == null ? 0 : 1)
                .adjustedStartCopyNumberChange(1)
                .adjustedEndCopyNumberChange(var.end() == null ? 0 : 1)
                .insertSequence(var.insertSequence())
                .type(var.type())
                .filter(var.filter())
                .imprecise(var.imprecise())
                .qualityScore(DatabaseUtil.valueNotNull(var.qualityScore()))
                .event(valueNotNull(var.event()))
                .startTumorVariantFragmentCount(DatabaseUtil.valueNotNull(var.start().tumorVariantFragmentCount()))
                .startTumorReferenceFragmentCount(DatabaseUtil.valueNotNull(var.start().tumorReferenceFragmentCount()))
                .startNormalVariantFragmentCount(DatabaseUtil.valueNotNull(var.start().normalVariantFragmentCount()))
                .startNormalReferenceFragmentCount(DatabaseUtil.valueNotNull(var.start().normalReferenceFragmentCount()))
                .endTumorVariantFragmentCount(var.end() == null ? 0 : DatabaseUtil.valueNotNull(var.end().tumorVariantFragmentCount()))
                .endTumorReferenceFragmentCount(var.end() == null ? 0 : DatabaseUtil.valueNotNull(var.end().tumorReferenceFragmentCount()))
                .endNormalVariantFragmentCount(var.end() == null ? 0 : DatabaseUtil.valueNotNull(var.end().normalVariantFragmentCount()))
                .endNormalReferenceFragmentCount(var.end() == null ? 0 : DatabaseUtil.valueNotNull(var.end().normalReferenceFragmentCount()))
                .startIntervalOffsetStart(DatabaseUtil.valueNotNull(var.start().startOffset()))
                .startIntervalOffsetEnd(DatabaseUtil.valueNotNull(var.start().endOffset()))
                .endIntervalOffsetStart(var.end() == null ? 0 : DatabaseUtil.valueNotNull(var.end().startOffset()))
                .endIntervalOffsetEnd(var.end() == null ? 0 : DatabaseUtil.valueNotNull(var.end().endOffset()))
                .inexactHomologyOffsetStart(DatabaseUtil.valueNotNull(var.start().inexactHomologyOffsetStart()))
                .inexactHomologyOffsetEnd(DatabaseUtil.valueNotNull(var.start().inexactHomologyOffsetEnd()))
                .startLinkedBy(valueNotNull(var.startLinkedBy()))
                .endLinkedBy(valueNotNull(var.endLinkedBy()))
                .vcfId(valueNotNull(var.id()))
                .startRefContext("") // getValueNotNull(var.start().refGenomeContext())
                .endRefContext(var.end() == null ? "" : "") // getValueNotNull(var.end().refGenomeContext())
                .recovered(var.recovered())
                .recoveryMethod((valueNotNull(var.recoveryMethod())))
                .recoveryFilter(valueNotNull(var.recoveryFilter()))
                .insertSequenceAlignments(valueNotNull(var.insertSequenceAlignments()))
                .insertSequenceRepeatClass(valueNotNull(var.insertSequenceRepeatClass()))
                .insertSequenceRepeatType(valueNotNull(var.insertSequenceRepeatType()))
                .insertSequenceRepeatOrientation(DatabaseUtil.valueNotNull(var.insertSequenceRepeatOrientation()))
                .insertSequenceRepeatCoverage(DatabaseUtil.valueNotNull(var.insertSequenceRepeatCoverage()))
                .startAnchoringSupportDistance(var.start().anchoringSupportDistance())
                .endAnchoringSupportDistance(var.end() == null ? 0 : var.end().anchoringSupportDistance())
                .ponCount(var.startContext().getAttributeAsInt(PON_COUNT, 0))
                .build();
    }
}
