package com.hartwig.hmftools.knowledgebaseimporter.cosmic

import com.hartwig.hmftools.extensions.csv.CsvReader
import com.hartwig.hmftools.knowledgebaseimporter.Knowledgebase
import com.hartwig.hmftools.knowledgebaseimporter.cosmic.input.CosmicKnownInput
import com.hartwig.hmftools.knowledgebaseimporter.diseaseOntology.Doid
import com.hartwig.hmftools.knowledgebaseimporter.output.*
import com.hartwig.hmftools.knowledgebaseimporter.readCSVRecords
import org.apache.commons.csv.CSVRecord
import org.apache.logging.log4j.LogManager

class Cosmic(fusionsLocation: String) : Knowledgebase {
    override val source = "cosmic"
    override val knownVariants: List<KnownVariantOutput> = listOf()
    override val knownFusionPairs by lazy { readCSVRecords(fusionsLocation) { readFusion(it) }.distinct() }
    override val promiscuousGenes: List<PromiscuousGene> = listOf()
    override val actionableVariants: List<ActionableVariantOutput> = listOf()
    override val actionableCNVs: List<ActionableCNVOutput> = listOf()
    override val actionableFusionPairs: List<ActionableFusionPairOutput> = listOf()
    override val actionablePromiscuousGenes: List<ActionablePromiscuousGeneOutput> = listOf()
    override val actionableRanges: List<ActionableGenomicRangeOutput> = listOf()
    override val cancerTypes: Map<String, Set<Doid>> = mapOf()
    private val logger = LogManager.getLogger("cosmic")

    private fun readFusion(csvRecord: CSVRecord): FusionPair {
        val fiveGene = csvRecord["5' Partner"].split("_").first()
        val threeGene = csvRecord["3' Partner"].split("_").first()
        return FusionPair(fiveGene, threeGene)
    }

//    override val actionableKbRecords by lazy {
//        logger.info("Reading cosmic actionable records.")
//        CsvReader.readTSVByName<CosmicKnownInput>(fusionsLocation).mapNotNull { it.corrected() }.map { }
//    }
}
