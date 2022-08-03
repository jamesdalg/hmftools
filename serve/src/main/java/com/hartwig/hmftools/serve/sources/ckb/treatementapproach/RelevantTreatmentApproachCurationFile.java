package com.hartwig.hmftools.serve.sources.ckb.treatementapproach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.serve.actionability.EvidenceDirection;
import com.hartwig.hmftools.common.serve.actionability.EvidenceLevel;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public class RelevantTreatmentApproachCurationFile {

    private static final String FIELD_DELIMITER = "\t";

    @NotNull
    public static Map<RelevantTreatmentApprochCurationEntryKey, RelevantTreatmentApprochCurationEntry> read(
            @NotNull String treatmentApproachCurationFile) throws IOException {
        List<String> lines = Files.readAllLines(new File(treatmentApproachCurationFile).toPath());
        // Skip header
        return fromLines(lines.subList(1, lines.size()));
    }

    @NotNull
    private static Map<RelevantTreatmentApprochCurationEntryKey, RelevantTreatmentApprochCurationEntry> fromLines(
            @NotNull List<String> lines) {
        Map<RelevantTreatmentApprochCurationEntryKey, RelevantTreatmentApprochCurationEntry> mapEntry = Maps.newHashMap();
        for (String line : lines) {
            String[] values = line.split(FIELD_DELIMITER);

            RelevantTreatmentApprochCurationEntryKey entryKey = ImmutableRelevantTreatmentApprochCurationEntryKey.builder()
                    .treatment(values[1])
                    .treatmentApproach(values[2].isEmpty() ? null : values[2])
                    .event(values[3])
                    .direction(EvidenceDirection.valueOf(values[4]))
                    .build();

            mapEntry.put(entryKey,
                    ImmutableRelevantTreatmentApprochCurationEntry.builder()
                            .curationType(RelevantTreatmentApproachCurationType.valueOf(values[0]))
                            .curationKey(entryKey)
                            .curatedTreatmentApproach(values.length == 6 ? values[5] : null)
                            .build());
        }
        return mapEntry;
    }
}