package com.hartwig.hmftools.serve.extraction.fusion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Set;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.serve.classification.EventType;
import com.hartwig.hmftools.serve.extraction.util.DriverInconsistencyMode;
import com.hartwig.hmftools.serve.extraction.util.GeneChecker;
import com.hartwig.hmftools.serve.refgenome.RefGenomeManagerFactoryTest;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class FusionExtractorTest {

    private static final GeneChecker GENE_CHECKER = new GeneChecker(Sets.newHashSet("EGFR", "PDGFRA", "BCR", "MET", "NTRK3"));

    @Test
    public void canFilterInCatalog() {
        FusionExtractor fusionExtractorIgnore = buildTestFusionExtractor(GENE_CHECKER, Sets.newHashSet(), DriverInconsistencyMode.IGNORE);
        KnownFusionPair fusionIgnore = fusionExtractorIgnore.extract("NTRK3", EventType.FUSION_PAIR, "BCR-NTRK3 Fusion");
        assertNotNull(fusionIgnore);

        FusionExtractor fusionExtractorFilter = buildTestFusionExtractor(GENE_CHECKER, Sets.newHashSet(), DriverInconsistencyMode.FILTER);
        KnownFusionPair fusionFilter = fusionExtractorFilter.extract("NTRK3", EventType.FUSION_PAIR, "BCR-NTRK3 Fusion");
        assertNotNull(fusionFilter);

        FusionExtractor fusionExtractorWarn = buildTestFusionExtractor(GENE_CHECKER, Sets.newHashSet(), DriverInconsistencyMode.WARN_ONLY);
        KnownFusionPair fusionWarn = fusionExtractorWarn.extract("NTRK3", EventType.FUSION_PAIR, "BCR-NTRK3 Fusion");
        assertNotNull(fusionWarn);
    }

    @Test
    public void canFilterNotInCatalog() {
        FusionExtractor fusionExtractorIgnore = buildTestFusionExtractor(GENE_CHECKER, Sets.newHashSet(), DriverInconsistencyMode.IGNORE);
        KnownFusionPair fusionIgnore = fusionExtractorIgnore.extract("PDGFRA", EventType.FUSION_PAIR, "BCR-PDGFRA Fusion");
        assertNotNull(fusionIgnore);

        FusionExtractor fusionExtractorFilter = buildTestFusionExtractor(GENE_CHECKER, Sets.newHashSet(), DriverInconsistencyMode.FILTER);
        KnownFusionPair fusionFilter = fusionExtractorFilter.extract("PDGFRA", EventType.FUSION_PAIR, "BCR-PDGFRA Fusion");
        assertNull(fusionFilter);

        FusionExtractor fusionExtractorWarn = buildTestFusionExtractor(GENE_CHECKER, Sets.newHashSet(), DriverInconsistencyMode.WARN_ONLY);
        KnownFusionPair fusionWarn = fusionExtractorWarn.extract("PDGFRA", EventType.FUSION_PAIR, "BCR-PDGFRA Fusion");
        assertNotNull(fusionWarn);
    }

    @Test
    public void canExtractSimpleFusionPair() {
        FusionExtractor fusionExtractor = testFusionExtractor();
        KnownFusionPair fusion = fusionExtractor.extract("PDGFRA", EventType.FUSION_PAIR, "BCR-PDGFRA Fusion");

        assertNotNull(fusion);
        assertEquals("BCR", fusion.geneUp());
        assertEquals("PDGFRA", fusion.geneDown());
    }

    @Test
    public void ignoresFusionsOnUnknownGenes() {
        FusionExtractor fusionExtractor = testFusionExtractor();
        KnownFusionPair fusion = fusionExtractor.extract("IG", EventType.FUSION_PAIR, "IG-BCL2");

        assertNull(fusion);
    }

    @Test
    public void canExtractFusionPairsWithExonsUpDown() {
        FusionExtractor fusionExtractor = testFusionExtractor();
        KnownFusionPair fusion = fusionExtractor.extract("EGFR", EventType.FUSION_PAIR, "EGFRvII");

        assertNotNull(fusion);
        assertEquals("EGFR", fusion.geneUp());
        assertEquals(13, (int) fusion.minExonUp());
        assertEquals(13, (int) fusion.maxExonUp());
        assertEquals("EGFR", fusion.geneDown());
        assertEquals(16, (int) fusion.minExonDown());
        assertEquals(16, (int) fusion.maxExonDown());
    }

    @Test
    public void canExtractFusionPairsWithOddNames() {
        FusionExtractor fusionExtractor =
                testFusionExtractorWithGeneChecker(new GeneChecker(Sets.newHashSet("IGH", "NKX2-1", "HLA-A", "ROS1")));
        KnownFusionPair fusion1 = fusionExtractor.extract("NKX2-1", EventType.FUSION_PAIR, "IGH-NKX2-1 Fusion");

        assertNotNull(fusion1);
        assertEquals("IGH", fusion1.geneUp());
        assertEquals("NKX2-1", fusion1.geneDown());

        KnownFusionPair fusion2 = fusionExtractor.extract("ROS1", EventType.FUSION_PAIR, "HLA-A-ROS1 Fusion");
        assertEquals("HLA-A", fusion2.geneUp());
        assertEquals("ROS1", fusion2.geneDown());

        KnownFusionPair fusion3 = fusionExtractor.extract("ROS1", EventType.FUSION_PAIR, "HLA-A-HLA-A");
        assertEquals("HLA-A", fusion3.geneUp());
        assertEquals("HLA-A", fusion3.geneDown());
    }

    @Test
    public void canExtractFusionPairsWithExons() {
        FusionExtractor fusionExtractor = testFusionExtractor();
        KnownFusionPair fusion = fusionExtractor.extract("MET", EventType.FUSION_PAIR_AND_EXON, "EXON 14 SKIPPING MUTATION");

        assertNotNull(fusion);
        assertEquals("MET", fusion.geneUp());
        assertEquals(13, (int) fusion.minExonUp());
        assertEquals(13, (int) fusion.maxExonUp());
        assertEquals("MET", fusion.geneDown());
        assertEquals(15, (int) fusion.minExonDown());
        assertEquals(15, (int) fusion.maxExonDown());
    }

    @Test
    public void canExtractExonicDelDupFusions() {
        FusionExtractor fusionExtractor = testFusionExtractorWithExonicDelDupKeyPhrases(Sets.newHashSet("skip this"));
        KnownFusionPair fusion = fusionExtractor.extract("EGFR", EventType.FUSION_PAIR, "KINASE DOMAIN DUPLICATION (EXON 18-25)");

        assertNotNull(fusion);
        assertEquals("EGFR", fusion.geneUp());
        assertEquals(25, (int) fusion.minExonUp());
        assertEquals(26, (int) fusion.maxExonUp());
        assertEquals("EGFR", fusion.geneDown());
        assertEquals(14, (int) fusion.minExonDown());
        assertEquals(18, (int) fusion.maxExonDown());
    }

    @Test
    public void canFilterFusionPairsWithExonsOnWrongGenes() {
        FusionExtractor fusionExtractor = testFusionExtractor();
        assertNull(fusionExtractor.extract("BRAF", EventType.FUSION_PAIR_AND_EXON, "EXON 14 SKIPPING MUTATION"));
    }

    @Test
    public void canFilterNonConfiguredFusionPairsWithExons() {
        FusionExtractor fusionExtractor = testFusionExtractor();
        assertNull(fusionExtractor.extract("MET", EventType.FUSION_PAIR_AND_EXON, "Does not exist"));
    }

    @NotNull
    private static FusionExtractor testFusionExtractor() {
        return buildTestFusionExtractor(GENE_CHECKER, Sets.newHashSet(), DriverInconsistencyMode.IGNORE);
    }

    @NotNull
    private static FusionExtractor testFusionExtractorWithGeneChecker(@NotNull GeneChecker geneChecker) {
        return buildTestFusionExtractor(geneChecker, Sets.newHashSet(), DriverInconsistencyMode.IGNORE);
    }

    @NotNull
    private static FusionExtractor testFusionExtractorWithExonicDelDupKeyPhrases(@NotNull Set<String> exonicDelDupKeyPhrases) {
        return buildTestFusionExtractor(GENE_CHECKER, exonicDelDupKeyPhrases, DriverInconsistencyMode.IGNORE);
    }

    @NotNull
    private static FusionExtractor buildTestFusionExtractor(@NotNull GeneChecker geneChecker, @NotNull Set<String> exonicDelDupKeyPhrases,
            @NotNull DriverInconsistencyMode annotation) {
        return new FusionExtractor(geneChecker, RefGenomeManagerFactoryTest.knownFusionCache(), exonicDelDupKeyPhrases, annotation);
    }
}