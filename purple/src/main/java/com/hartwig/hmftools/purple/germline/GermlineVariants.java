package com.hartwig.hmftools.purple.germline;

import static com.hartwig.hmftools.common.purple.PurpleCommon.PURPLE_GERMLINE_VCF_SUFFIX;
import static com.hartwig.hmftools.common.variant.impact.VariantTranscriptImpact.VAR_TRANS_IMPACT_ANNOATATION;
import static com.hartwig.hmftools.purple.PurpleCommon.PPL_LOGGER;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.hartwig.hmftools.common.purple.PurityAdjuster;
import com.hartwig.hmftools.common.purple.copynumber.PurpleCopyNumber;
import com.hartwig.hmftools.common.variant.VariantHeader;
import com.hartwig.hmftools.purple.config.PurpleConfig;
import com.hartwig.hmftools.purple.config.ReferenceData;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.Nullable;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;

public class GermlineVariants
{
    private final PurpleConfig mConfig;
    private final ReferenceData mReferenceData;
    private final String mVersion;

    private final List<GermlineVariant> mVariants;
    private final List<GermlineVariant> mReportableVariants;

    public GermlineVariants(final PurpleConfig config, final ReferenceData referenceData, final String version)
    {
        mReferenceData = referenceData;
        mConfig = config;
        mVersion = version;

        mVariants = Lists.newArrayList();
        mReportableVariants = Lists.newArrayList();
    }

    public List<GermlineVariant> reportableVariants()
    {
        return mReportableVariants;
    }

    public void loadReportableVariants(final String germlineVcf)
    {
        loadGermlineVariants(germlineVcf, true);
    }

    private void loadGermlineVariants(final String germlineVcf, boolean checkReported)
    {
        if(germlineVcf.isEmpty())
            return;

        VCFFileReader vcfReader = new VCFFileReader(new File(germlineVcf), false);

        for(VariantContext context : vcfReader)
        {
            boolean isReported = context.getAttributeAsBoolean(VariantHeader.REPORTED_FLAG, false);

            if(checkReported && !isReported)
                continue;

            GermlineVariant variant = new GermlineVariant(context);

            if(checkReported)
                mReportableVariants.add(variant);
            else
                mVariants.add(variant);
        }

        if(checkReported)
        {
            PPL_LOGGER.info("load {} reported germline variants from {}", mReportableVariants.size(), germlineVcf);
        }
        else
        {
            PPL_LOGGER.info("load {} germline variants from {}", mVariants.size(), germlineVcf);
        }
    }

    public void processAndWrite(
            final String referenceId, final String tumorSample, final String germlineVcf, @Nullable final PurityAdjuster purityAdjuster,
            final List<PurpleCopyNumber> copyNumbers, final Set<String> somaticReportedGenes) throws Exception
    {
        mReportableVariants.clear();

        if(germlineVcf.isEmpty())
            return;

        final String outputVCF = mConfig.OutputDir + File.separator + tumorSample + PURPLE_GERMLINE_VCF_SUFFIX;

        loadGermlineVariants(germlineVcf, false);

        VCFFileReader vcfReader = new VCFFileReader(new File(germlineVcf), false);

        boolean isPaveAnnotated = vcfReader.getFileHeader().hasInfoLine(VAR_TRANS_IMPACT_ANNOATATION);

        VariantContextWriter writer = new VariantContextWriterBuilder().setOutputFile(outputVCF)
                .setOption(htsjdk.variant.variantcontext.writer.Options.ALLOW_MISSING_FIELDS_IN_HEADER)
                .build();

        final GermlineVariantEnrichment enrichment = new GermlineVariantEnrichment(
                mVersion, referenceId, tumorSample, mReferenceData, purityAdjuster, copyNumbers,
                mReferenceData.GermlineHotspots, somaticReportedGenes, !isPaveAnnotated);

        VCFHeader header = enrichment.enrichHeader(vcfReader.getFileHeader());
        writer.writeHeader(header);

        for(GermlineVariant variant : mVariants)
        {
            enrichment.enrichVariant(variant);
        }

        enrichment.flush();

        for(GermlineVariant variant : mVariants)
        {
            VariantContext newContext = new VariantContextBuilder(variant.context()).filters(variant.filters()).make();

            if(newContext.getAttributeAsBoolean(VariantHeader.REPORTED_FLAG, false))
                mReportableVariants.add(variant);

            writer.add(newContext);
        }

        writer.close();
    }
}
