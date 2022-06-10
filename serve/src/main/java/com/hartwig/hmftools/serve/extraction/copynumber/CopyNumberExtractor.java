package com.hartwig.hmftools.serve.extraction.copynumber;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.drivercatalog.DriverCategory;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGene;
import com.hartwig.hmftools.common.serve.classification.EventType;
import com.hartwig.hmftools.serve.extraction.util.DriverInconsistencyMode;
import com.hartwig.hmftools.serve.extraction.util.GeneChecker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopyNumberExtractor {

    private static final Logger LOGGER = LogManager.getLogger(CopyNumberExtractor.class);

    private static final Set<EventType> COPY_NUMBER_EVENTS =
            Sets.newHashSet(EventType.AMPLIFICATION, EventType.OVER_EXPRESSION, EventType.DELETION, EventType.UNDER_EXPRESSION);

    @NotNull
    private final GeneChecker geneChecker;
    @NotNull
    private final List<DriverGene> driverGenes;
    private final DriverInconsistencyMode driverInconsistencyMode;

    public CopyNumberExtractor(@NotNull final GeneChecker geneChecker, @NotNull final List<DriverGene> driverGenes,
            @NotNull final DriverInconsistencyMode driverInconsistencyMode) {
        this.geneChecker = geneChecker;
        this.driverGenes = driverGenes;
        this.driverInconsistencyMode = driverInconsistencyMode;
    }

    @Nullable
    public KnownCopyNumber extract(@NotNull String gene, @NotNull EventType type) {
        if (COPY_NUMBER_EVENTS.contains(type) && geneChecker.isValidGene(gene)) {
            DriverCategory driverCategory = findByGene(driverGenes, gene);

            if (driverInconsistencyMode.isActive()) {
                if ((driverCategory == DriverCategory.TSG && type == EventType.AMPLIFICATION) || (driverCategory == DriverCategory.TSG
                        && type == EventType.OVER_EXPRESSION) || (driverCategory == DriverCategory.ONCO && type == EventType.DELETION) || (
                        driverCategory == DriverCategory.ONCO && type == EventType.UNDER_EXPRESSION) || driverCategory == null) {
                    if (driverInconsistencyMode == DriverInconsistencyMode.WARN_ONLY) {
                        LOGGER.warn("CopyNumber event mismatch for {} in driver category {} vs event type {}", gene, driverCategory, type);
                    } else if (driverInconsistencyMode == DriverInconsistencyMode.FILTER) {
                        LOGGER.info("CopyNumber event filtered -- Mismatch for {} in driver category {} vs event type {}",
                                gene,
                                driverCategory,
                                type);
                        return null;
                    }
                }
            }

            return ImmutableKnownCopyNumber.builder().gene(gene).type(toCopyNumberType(type)).build();
        }
        return null;
    }

    @Nullable
    private static DriverCategory findByGene(@NotNull List<DriverGene> driverGenes, @NotNull String gene) {
        for (DriverGene driverGene : driverGenes) {
            if (driverGene.gene().equals(gene)) {
                return driverGene.likelihoodType();
            }
        }
        return null;
    }

    @NotNull
    private static CopyNumberType toCopyNumberType(@NotNull EventType eventType) {
        assert COPY_NUMBER_EVENTS.contains(eventType);

        switch (eventType) {
            case AMPLIFICATION:
                return CopyNumberType.AMPLIFICATION;
            case OVER_EXPRESSION:
                return CopyNumberType.OVER_EXPRESSION;
            case DELETION:
                return CopyNumberType.DELETION;
            case UNDER_EXPRESSION:
                return CopyNumberType.UNDER_EXPRESSION;
            default:
                throw new IllegalStateException("Could not convert event type to copy number type: " + eventType);
        }
    }
}