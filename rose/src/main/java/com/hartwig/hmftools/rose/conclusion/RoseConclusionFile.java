package com.hartwig.hmftools.rose.conclusion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.StringJoiner;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public class RoseConclusionFile {

    private static final String EXTENSION = ".rose.tsv";
    private static final String FIELD_DELIMITER = "\t";

    private RoseConclusionFile() {
    }

    @NotNull
    public static String generateFilename(@NotNull String basePath, @NotNull String sample) {
        return basePath + File.separator + sample + EXTENSION;
    }

    public static void write(@NotNull String file, @NotNull ActionabilityConclusion actionabilityConclusion, @NotNull String sampleId)
            throws IOException {
        List<String> lines = Lists.newArrayList();
        lines.add(toLine(actionabilityConclusion.conclusion(), sampleId));
        Files.write(new File(file).toPath(), lines);
    }

    @NotNull
    private static String toLine(@NotNull String conclusion, @NotNull String sampleId) {
        return new StringJoiner(FIELD_DELIMITER).add(sampleId).add(conclusion).toString();
    }
}