package com.hartwig.hmftools.common.protect;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.serve.Knowledgebase;
import com.hartwig.hmftools.common.serve.actionability.EvidenceDirection;
import com.hartwig.hmftools.common.serve.actionability.EvidenceLevel;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public final class ProtectTestFactory {

    private ProtectTestFactory() {
    }

    @NotNull
    public static ImmutableProtectEvidence.Builder builder() {
        return ImmutableProtectEvidence.builder()
                .event(Strings.EMPTY)
                .germline(false)
                .reported(true)
                .treatment(Strings.EMPTY)
                .onLabel(false)
                .level(EvidenceLevel.A)
                .direction(EvidenceDirection.RESPONSIVE)
                .sources(Sets.newHashSet(createSource(Knowledgebase.VICC_CGI)));
    }

    @NotNull
    public static KnowledgebaseSource createSource(@NotNull Knowledgebase knowledgebase) {
        return ImmutableKnowledgebaseSource.builder()
                .name(knowledgebase)
                .sourceEvent("any mutation")
                .evidenceType(EvidenceType.ANY_MUTATION)
                .build();
    }
}