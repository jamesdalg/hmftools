package com.hartwig.hmftools.ckb.classification;

import com.hartwig.hmftools.ckb.datamodel.variant.Variant;
import com.hartwig.hmftools.common.genome.refgenome.GeneNameMapping;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class EventAndGeneExtractor {

    private static final Logger LOGGER = LogManager.getLogger(EventAndGeneExtractor.class);

    @NotNull
    private final GeneNameMapping geneNameMapping;

    public EventAndGeneExtractor() {
        this.geneNameMapping = GeneNameMapping.loadFromEmbeddedResource();
    }

    @NotNull
    public String extractGene(@NotNull Variant variant) {
        if (isFusion(variant) && !isPromiscuousFusion(variant)) {
            return variant.fullName();
        } else {
            String primaryGene = variant.gene().geneSymbol();
            if (!geneNameMapping.isValidV38Gene(primaryGene)) {
                for (String synonym : variant.gene().synonyms()) {
                    if (geneNameMapping.isValidV38Gene(synonym)) {
                        LOGGER.debug("Swapping CKB gene '{}' with synonym '{}'", primaryGene, synonym);
                        return synonym;
                    }
                }

                // If we can't find a mappable gene, just return the original gene again.
                LOGGER.warn("Could not find synonym for '{}' that exists in HMF v38 gene model", primaryGene);
            }
            return primaryGene;
        }
    }

    @NotNull
    public String extractEvent(@NotNull Variant variant) {
        if (isPromiscuousFusion(variant)) {
            return "fusion promiscuous";
        } else if (isFusion(variant)) {
            return removeAllSpaces(variant.variant());
        } else if (variant.variant().contains("exon") && !variant.variant().contains("exon ")) {
            // Some exon variants do not contain a space after the "exon" keyword
            return variant.variant().replace("exon", "exon ");
        } else {
            return variant.variant();
        }
    }

    private static boolean isFusion(@NotNull Variant variant) {
        return variant.impact() != null && variant.impact().equals("fusion");
    }

    private static boolean isPromiscuousFusion(@NotNull Variant variant) {
        return isFusion(variant) && variant.variant().equals("fusion");
    }

    @NotNull
    private static String removeAllSpaces(@NotNull String value) {
        return value.replaceAll("\\s+", "");
    }
}
