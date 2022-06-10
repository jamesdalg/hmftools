package com.hartwig.hmftools.orange.algo.selection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.protect.ProtectEvidence;
import com.hartwig.hmftools.common.protect.ProtectSource;
import com.hartwig.hmftools.common.serve.Knowledgebase;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EvidenceSelector {

    private static final Set<Knowledgebase> TRIAL_SOURCES = Sets.newHashSet(Knowledgebase.ACTIN, Knowledgebase.ICLUSION);

    private EvidenceSelector() {
    }

    public static boolean hasEvidence(@NotNull List<ProtectEvidence> evidences, @Nullable String gene, @NotNull String event) {
        for (ProtectEvidence evidence : evidences) {
            if ((gene == null || gene.equals(evidence.gene())) && evidence.event().equals(event)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static List<ProtectEvidence> reported(@NotNull List<ProtectEvidence> evidences) {
        List<ProtectEvidence> filtered = Lists.newArrayList();
        for (ProtectEvidence evidence : evidences) {
            if (evidence.reported()) {
                filtered.add(evidence);
            }
        }
        return filtered;
    }

    @NotNull
    public static List<ProtectEvidence> unreported(@NotNull List<ProtectEvidence> evidences) {
        List<ProtectEvidence> filtered = Lists.newArrayList();
        for (ProtectEvidence evidence : evidences) {
            if (!evidence.reported()) {
                filtered.add(evidence);
            }
        }
        return filtered;
    }

    @NotNull
    public static List<ProtectEvidence> trialsOnly(@NotNull List<ProtectEvidence> evidences) {
        List<ProtectEvidence> filtered = Lists.newArrayList();
        for (ProtectEvidence evidence : evidences) {
            if (hasTrialSource(evidence.sources())) {
                filtered.add(evidence);
            }
        }
        return filtered;
    }

    @NotNull
    public static List<ProtectEvidence> noTrials(@NotNull List<ProtectEvidence> evidences) {
        List<ProtectEvidence> filtered = Lists.newArrayList();
        for (ProtectEvidence evidence : evidences) {
            if (!hasTrialSource(evidence.sources())) {
                filtered.add(evidence);
            }
        }
        return filtered;
    }

    private static boolean hasTrialSource(@NotNull Iterable<ProtectSource> sources) {
        for (ProtectSource source : sources) {
            if (TRIAL_SOURCES.contains(source.name())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static Map<String, List<ProtectEvidence>> buildTreatmentMap(@NotNull List<ProtectEvidence> evidences, boolean requireOnLabel) {
        Map<String, List<ProtectEvidence>> evidencePerTreatmentMap = Maps.newHashMap();

        for (ProtectEvidence evidence : evidences) {
            if (evidence.onLabel() == requireOnLabel) {
                List<ProtectEvidence> treatmentEvidences = evidencePerTreatmentMap.get(evidence.treatment());
                if (treatmentEvidences == null) {
                    treatmentEvidences = Lists.newArrayList();
                }

                if (!hasHigherOrEqualEvidence(treatmentEvidences, evidence)) {
                    treatmentEvidences.add(evidence);
                }

                evidencePerTreatmentMap.put(evidence.treatment(), treatmentEvidences);
            }
        }
        return evidencePerTreatmentMap;
    }

    private static boolean hasHigherOrEqualEvidence(@NotNull List<ProtectEvidence> evidences, @NotNull ProtectEvidence evidenceToCheck) {
        for (ProtectEvidence evidence : evidences) {
            if (evidence.treatment().equals(evidenceToCheck.treatment()) && StringUtils.equals(evidence.gene(), evidenceToCheck.gene())
                    && evidence.event().equals(evidenceToCheck.event())) {
                if (!evidenceToCheck.level().isHigher(evidence.level())) {
                    return true;
                }
            }
        }
        return false;
    }
}