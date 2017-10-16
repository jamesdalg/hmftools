package com.hartwig.hmftools.cobalt.segment;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.cobalt.CobaltPositionFile;
import com.hartwig.hmftools.common.pcf.PCFFile;
import com.hartwig.hmftools.common.r.RExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class PCFSegment {

    private static final Logger LOGGER = LogManager.getLogger(PCFSegment.class);

    private final String outputDirectory;
    private final ExecutorService executorService;

    public PCFSegment(final ExecutorService executorService, final String outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.executorService = executorService;
    }

    public void applySegmentation(@NotNull final String reference, @NotNull final String tumor)
            throws ExecutionException, InterruptedException {
        final String ratioFile = CobaltPositionFile.generateFilename(outputDirectory, tumor);
        final List<Future<Object>> futures = Lists.newArrayList();
        futures.add(executorService.submit(() -> ratioSegmentation(ratioFile, reference, "ReferenceGCDiploidRatio")));
        futures.add(executorService.submit(() -> ratioSegmentation(ratioFile, tumor, "TumorGCRatio")));

        for (Future<Object> future : futures) {
            future.get();
        }

        LOGGER.info("Segmentation Complete");
    }

    private Object ratioSegmentation(@NotNull final String ratioFile, @NotNull final String sample, @NotNull final String column) throws IOException, InterruptedException {
        final String pcfFile = PCFFile.generateRatioFilename(outputDirectory, sample);
        int result = RExecutor.executeFromClasspath("r/ratioSegmentation.R", ratioFile, column, pcfFile);
        if (result != 0) {
            throw new IOException("R execution failed. Unable to complete segmentation.");
        }

        return null;
    }

}
