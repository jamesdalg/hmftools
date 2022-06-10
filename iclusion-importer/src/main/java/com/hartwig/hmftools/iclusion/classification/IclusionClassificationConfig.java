package com.hartwig.hmftools.iclusion.classification;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.serve.classification.EventClassifierConfig;
import com.hartwig.hmftools.common.serve.classification.ImmutableEventClassifierConfig;

import org.jetbrains.annotations.NotNull;

public final class IclusionClassificationConfig {

    private static final Set<String> EXON_IDENTIFIERS = exonIdentifiers();
    private static final Set<String> EXON_KEYWORDS = exonKeywords();
    private static final Set<String> EXON_BLACKLIST_KEY_PHRASES = exonBlacklistKeyPhrases();
    private static final Set<String> SPECIFIC_EXON_EVENTS = specificExonEvents();
    private static final Map<String, Set<String>> FUSION_PAIR_AND_EXONS_PER_GENE = fusionPairAndExonsPerGene();
    private static final Set<String> GENE_LEVEL_BLACKLIST_KEY_PHRASES = geneLevelBlacklistKeyPhrases();
    private static final Set<String> GENERIC_GENE_LEVEL_KEY_PHRASES = genericGeneLevelKeyPhrases();
    private static final Set<String> ACTIVATING_GENE_LEVEL_KEY_PHRASES = activatingGeneLevelKeyPhrases();
    private static final Set<String> INACTIVATING_GENE_LEVEL_KEY_PHRASES = inactivatingGeneLevelKeyPhrases();
    private static final Set<String> WILD_TYPE_KEY_PHRASES = wildTypeKeyPhrases();
    private static final Set<String> AMPLIFICATION_KEYWORDS = amplificationKeywords();
    private static final Set<String> AMPLIFICATION_KEY_PHRASES = amplificationKeyPhrases();

    private static final Set<String> OVER_EXPRESSION_KEYWORDS = overExpressionKeywords();

    private static final Set<String> OVER_EXPRESSION_KEY_PHRASES = overExpressionKeyPhrases();
    private static final Set<String> DELETION_BLACKLIST_KEY_PHRASES = deletionBlacklistKeyPhrases();
    private static final Set<String> DELETION_KEYWORDS = deletionKeywords();
    private static final Set<String> DELETION_KEY_PHRASES = deletionKeyPhrases();

    private static final Set<String> UNDER_EXPRESSION_KEYWORDS = underExpressionKeywords();

    private static final Set<String> UNDER_EXPRESSION_KEY_PHRASES = underExpressionKeyPhrases();
    private static final Set<String> EXONIC_DEL_DUP_FUSION_KEY_PHRASES = exonicDelDupFusionKeyPhrases();
    private static final Set<String> EXONIC_DEL_DUP_FUSION_EVENTS = exonicDelDupFusionEvents();
    private static final Set<String> FUSION_PAIR_EVENTS_TO_SKIP = fusionPairEventsToSkip();
    private static final Set<String> PROMISCUOUS_FUSION_KEY_PHRASES = promiscuousFusionKeyPhrases();
    private static final Set<String> MICROSATELLITE_UNSTABLE_KEY_PHRASES = microsatelliteUnstableKeyPhrases();
    private static final Set<String> MICROSATELLITE_STABLE_KEY_PHRASES = microsatelliteStableKeyPhrases();
    private static final Set<String> HIGH_TUMOR_MUTATIONAL_LOAD_KEY_PHRASES = highTumorMutationalLoadKeyPhrases();
    private static final Set<String> LOW_TUMOR_MUTATIONAL_LOAD_KEY_PHRASES = lowTumorMutationalLoadKeyPhrases();
    private static final Set<String> HIGH_TUMOR_MUTATIONAL_BURDEN_KEY_PHRASES = highTumorMutationalBurdenKeyPhrases();
    private static final Set<String> LOW_TUMOR_MUTATIONAL_BURDEN_KEY_PHRASES = lowTumorMutationalBurdenKeyPhrases();
    private static final Set<String> HR_DEFICIENCY_KEY_PHRASES = hrDeficiencyKeyPhrases();
    private static final Set<String> HLA_KEY_PHRASES = hlaKeyPhrases();
    private static final Set<String> HPV_POSITIVE_EVENTS = hpvPositiveEvents();
    private static final Set<String> EBV_POSITIVE_EVENTS = ebvPositiveEvents();
    private static final Map<String, Set<String>> COMBINED_EVENTS_PER_GENE = combinedEventsPerGene();
    private static final Map<String, Set<String>> COMPLEX_EVENTS_PER_GENE = complexEventsPerGene();

    private IclusionClassificationConfig() {
    }

    @NotNull
    public static EventClassifierConfig build() {
        return ImmutableEventClassifierConfig.builder()
                .proteinAnnotationExtractor(new IclusionProteinAnnotationExtractor())
                .exonIdentifiers(EXON_IDENTIFIERS)
                .exonKeywords(EXON_KEYWORDS)
                .exonBlacklistKeyPhrases(EXON_BLACKLIST_KEY_PHRASES)
                .specificExonEvents(SPECIFIC_EXON_EVENTS)
                .fusionPairAndExonsPerGene(FUSION_PAIR_AND_EXONS_PER_GENE)
                .geneLevelBlacklistKeyPhrases(GENE_LEVEL_BLACKLIST_KEY_PHRASES)
                .genericGeneLevelKeyPhrases(GENERIC_GENE_LEVEL_KEY_PHRASES)
                .activatingGeneLevelKeyPhrases(ACTIVATING_GENE_LEVEL_KEY_PHRASES)
                .inactivatingGeneLevelKeyPhrases(INACTIVATING_GENE_LEVEL_KEY_PHRASES)
                .wildTypeKeyPhrases(WILD_TYPE_KEY_PHRASES)
                .amplificationKeywords(AMPLIFICATION_KEYWORDS)
                .amplificationKeyPhrases(AMPLIFICATION_KEY_PHRASES)
                .overExpressionKeywords(OVER_EXPRESSION_KEYWORDS)
                .overExpressionKeyPhrases(OVER_EXPRESSION_KEY_PHRASES)
                .deletionBlacklistKeyPhrases(DELETION_BLACKLIST_KEY_PHRASES)
                .deletionKeywords(DELETION_KEYWORDS)
                .deletionKeyPhrases(DELETION_KEY_PHRASES)
                .underExpressionKeywords(UNDER_EXPRESSION_KEYWORDS)
                .underExpressionKeyPhrases(UNDER_EXPRESSION_KEY_PHRASES)
                .exonicDelDupFusionKeyPhrases(EXONIC_DEL_DUP_FUSION_KEY_PHRASES)
                .exonicDelDupFusionEvents(EXONIC_DEL_DUP_FUSION_EVENTS)
                .fusionPairEventsToSkip(FUSION_PAIR_EVENTS_TO_SKIP)
                .promiscuousFusionKeyPhrases(PROMISCUOUS_FUSION_KEY_PHRASES)
                .microsatelliteUnstableKeyPhrases(MICROSATELLITE_UNSTABLE_KEY_PHRASES)
                .microsatelliteStableKeyPhrases(MICROSATELLITE_STABLE_KEY_PHRASES)
                .highTumorMutationalLoadKeyPhrases(HIGH_TUMOR_MUTATIONAL_LOAD_KEY_PHRASES)
                .lowTumorMutationalLoadKeyPhrases(LOW_TUMOR_MUTATIONAL_LOAD_KEY_PHRASES)
                .highTumorMutationalBurdenKeyPhrases(HIGH_TUMOR_MUTATIONAL_BURDEN_KEY_PHRASES)
                .lowTumorMutationalBurdenKeyPhrases(LOW_TUMOR_MUTATIONAL_BURDEN_KEY_PHRASES)
                .hrDeficiencyKeyPhrases(HR_DEFICIENCY_KEY_PHRASES)
                .hlaKeyPhrases(HLA_KEY_PHRASES)
                .hpvPositiveEvents(HPV_POSITIVE_EVENTS)
                .ebvPositiveEvents(EBV_POSITIVE_EVENTS)
                .combinedEventsPerGene(COMBINED_EVENTS_PER_GENE)
                .complexEventsPerGene(COMPLEX_EVENTS_PER_GENE)
                .build();
    }

    @NotNull
    private static Set<String> exonIdentifiers() {
        Set<String> set = Sets.newHashSet();
        set.add("EXON");
        set.add("Exon");
        return set;
    }

    @NotNull
    private static Set<String> exonKeywords() {
        Set<String> set = Sets.newHashSet();
        set.add("DELETION");
        set.add("INSERTION");
        set.add("MUTATION");
        set.add("ACTIVATING MUTATION");
        set.add("mutation");
        return set;
    }

    @NotNull
    private static Set<String> exonBlacklistKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> specificExonEvents() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Map<String, Set<String>> fusionPairAndExonsPerGene() {
        Map<String, Set<String>> map = Maps.newHashMap();

        map.put("MET", Sets.newHashSet("EXON 14 SKIPPING MUTATION"));
        return map;
    }

    @NotNull
    private static Set<String> geneLevelBlacklistKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("EXON");
        set.add("Exon");
        return set;
    }

    @NotNull
    private static Set<String> genericGeneLevelKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("MUTATION");
        set.add("mutation");
        return set;
    }

    @NotNull
    private static Set<String> activatingGeneLevelKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("ACTIVATING MUTATION");
        return set;
    }

    @NotNull
    private static Set<String> inactivatingGeneLevelKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("INACTIVATING MUTATION");
        return set;
    }

    @NotNull
    private static Set<String> wildTypeKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> amplificationKeywords() {
        Set<String> set = Sets.newHashSet();
        set.add("AMPLIFICATION");
        set.add("COPY-GAIN");
        return set;
    }

    @NotNull
    private static Set<String> amplificationKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> overExpressionKeywords() {
        Set<String> set = Sets.newHashSet();
        set.add("OVEREXPRESSION");
        return set;
    }

    @NotNull
    private static Set<String> overExpressionKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> deletionBlacklistKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("EXON");
        return set;
    }

    @NotNull
    private static Set<String> deletionKeywords() {
        Set<String> set = Sets.newHashSet();
        set.add("LOSS");
        set.add("LOSS-OF-FUNCTION");
        return set;
    }

    @NotNull
    private static Set<String> deletionKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> underExpressionKeywords() {
        Set<String> set = Sets.newHashSet();
        set.add("NON-EXPRESSION");
        return set;
    }

    @NotNull
    private static Set<String> underExpressionKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> exonicDelDupFusionKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("VIII");
        set.add("EXON 2-8 LOSS");
        set.add("EXON 12 LOSS");
        set.add("EXON 14 LOSS");
        return set;
    }

    @NotNull
    private static Set<String> exonicDelDupFusionEvents() {
        Set<String> set = Sets.newHashSet();
        set.add("KINASE DOMAIN DUPLICATION (EXON 18-25)");
        return set;
    }

    @NotNull
    private static Set<String> fusionPairEventsToSkip() {
        Set<String> set = Sets.newHashSet();
        set.add("CO-DELETION");
        set.add("COPY-GAIN");
        set.add("LOSS-OF-FUNCTION");
        set.add("FLT3-ITD");
        set.add("NON-EXPRESSION");
        set.add("P780-Y781insGSP");
        return set;
    }

    @NotNull
    private static Set<String> promiscuousFusionKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("FUSION");
        set.add("REARRANGEMENT");
        return set;
    }

    @NotNull
    private static Set<String> microsatelliteUnstableKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("MSI_HIGH");
        return set;
    }

    @NotNull
    private static Set<String> microsatelliteStableKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> highTumorMutationalLoadKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("TumMutLoad_HIGH");
        return set;
    }

    @NotNull
    private static Set<String> lowTumorMutationalLoadKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> lowTumorMutationalBurdenKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> highTumorMutationalBurdenKeyPhrases() {
        return Sets.newHashSet();
    }

    @NotNull
    private static Set<String> hrDeficiencyKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("HRD_POSITIVE");
        return set;
    }

    @NotNull
    private static Set<String> hlaKeyPhrases() {
        Set<String> set = Sets.newHashSet();
        set.add("A*02");
        return set;
    }

    @NotNull
    private static Set<String> hpvPositiveEvents() {
        Set<String> set = Sets.newHashSet();
        set.add("HPV_POSITIVE");
        return set;
    }

    @NotNull
    private static Set<String> ebvPositiveEvents() {
        Set<String> set = Sets.newHashSet();
        set.add("EBV_POSITIVE");
        return set;
    }

    @NotNull
    private static Map<String, Set<String>> combinedEventsPerGene() {
        Map<String, Set<String>> map = Maps.newHashMap();

        map.put("-", Sets.newHashSet("1p & 19q CO-DELETION"));

        return map;
    }

    @NotNull
    private static Map<String, Set<String>> complexEventsPerGene() {
        Map<String, Set<String>> map = Maps.newHashMap();

        Set<String> erbb2Set = Sets.newHashSet("DEL 755-759", "P780INS", "Exon 20 mutation (non-T790M)");
        map.put("ERBB2", erbb2Set);

        Set<String> brafSet = Sets.newHashSet("non-V600 ACTIVATING MUTATION");
        map.put("BRAF", brafSet);

        Set<String> egfrSet = Sets.newHashSet("Exon 20 mutation (non-T790M)");
        map.put("EGFR", egfrSet);

        Set<String> flt3Set = Sets.newHashSet("FLT3-ITD");
        map.put("FLT3", flt3Set);

        Set<String> ccnd2Set = Sets.newHashSet("3'UTR LOSS");
        map.put("CCND1", ccnd2Set);

        Set<String> poleSet = Sets.newHashSet("EDM MUTATION");
        map.put("POLE", poleSet);

        return map;
    }
}