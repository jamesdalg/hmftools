package com.hartwig.hmftools.common.linx;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.hmftools.common.sv.linx.LinxBreakend;
import com.hartwig.hmftools.common.sv.linx.LinxDriver;
import com.hartwig.hmftools.common.sv.linx.LinxFusion;
import com.hartwig.hmftools.common.sv.linx.LinxGermlineSv;
import com.hartwig.hmftools.common.sv.linx.LinxSvAnnotation;

import org.apache.commons.compress.utils.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LinxDataLoader {

    private static final Logger LOGGER = LogManager.getLogger(LinxDataLoader.class);

    private LinxDataLoader() {
    }

    @NotNull
    public static LinxData load(@NotNull String linxFusionTsv, @NotNull String linxBreakendTsv, @NotNull String linxDriverCatalogTsv)
            throws IOException {
        return load(null, linxFusionTsv, linxBreakendTsv, linxDriverCatalogTsv, null, null);
    }

    @NotNull
    public static LinxData load(@Nullable String linxStructuralVariantTsv, @NotNull String linxFusionTsv, @NotNull String linxBreakendTsv,
            @NotNull String linxDriverCatalogTsv, @Nullable String linxDriverTsv, @Nullable String linxGermlineDisruptionTsv)
            throws IOException {
        LOGGER.info("Loading LINX data from {}", new File(linxFusionTsv).getParent());
        List<LinxFusion> allFusions = LinxFusion.read(linxFusionTsv);

        List<LinxFusion> reportableFusions = Lists.newArrayList();
        for (LinxFusion fusion : allFusions) {
            if (fusion.reported()) {
                reportableFusions.add(fusion);
            }
        }
        LOGGER.info(" Loaded {} fusions (of which {} are reportable) from {}", allFusions.size(), reportableFusions.size(), linxFusionTsv);

        List<LinxSvAnnotation> allStructuralVariants = Lists.newArrayList();
        if (linxStructuralVariantTsv != null) {
            allStructuralVariants = LinxSvAnnotation.read(linxStructuralVariantTsv);
            LOGGER.info(" Loaded {} structural variants from {}", allStructuralVariants.size(), linxStructuralVariantTsv);
        }

        List<LinxBreakend> allBreakends =
                LinxBreakend.read(linxBreakendTsv).stream().filter(LinxBreakend::reportedDisruption).collect(Collectors.toList());
        List<ReportableGeneDisruption> reportableGeneDisruptions =
                ReportableGeneDisruptionFactory.convert(allBreakends, allStructuralVariants);
        LOGGER.debug(" Generated {} reportable disruptions based on {} breakends", reportableGeneDisruptions.size(), allBreakends.size());
        LOGGER.info(" Loaded {} reportable disruptions from {}", reportableGeneDisruptions.size(), linxBreakendTsv);

        List<ReportableHomozygousDisruption> homozygousDisruptions =
                ReportableHomozygousDisruptionFactory.extractFromLinxDriverCatalogTsv(linxDriverCatalogTsv);
        LOGGER.info(" Loaded {} reportable homozygous disruptions from {}", homozygousDisruptions.size(), linxDriverCatalogTsv);

        List<LinxDriver> allDrivers = Lists.newArrayList();
        if (linxDriverTsv != null) {
            allDrivers = LinxDriver.read(linxDriverTsv);
            LOGGER.info(" Loaded {} drivers from {}", allDrivers.size(), linxDriverTsv);
        }

        List<LinxGermlineSv> allGermlineDisruptions = Lists.newArrayList();
        List<LinxGermlineSv> reportableGermlineDisruptions = Lists.newArrayList();
        if (linxGermlineDisruptionTsv != null) {
            allGermlineDisruptions = LinxGermlineSv.read(linxGermlineDisruptionTsv);

            for (LinxGermlineSv germlineDisruption : allGermlineDisruptions) {
                if (germlineDisruption.Reported) {
                    reportableGermlineDisruptions.add(germlineDisruption);
                }
            }
            LOGGER.info(" Loaded {} germline disruptions (of which {} are reportable) from {}",
                    allGermlineDisruptions.size(),
                    reportableGermlineDisruptions.size(),
                    linxGermlineDisruptionTsv);
        }

        return ImmutableLinxData.builder()
                .allFusions(allFusions)
                .reportableFusions(reportableFusions)
                .allBreakends(allBreakends)
                .reportableGeneDisruptions(reportableGeneDisruptions)
                .homozygousDisruptions(homozygousDisruptions)
                .drivers(allDrivers)
                .allGermlineDisruptions(allGermlineDisruptions)
                .reportableGermlineDisruptions(reportableGermlineDisruptions)
                .build();
    }
}
