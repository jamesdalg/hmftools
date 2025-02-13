package com.hartwig.hmftools.lilac;

import static com.hartwig.hmftools.common.utils.ConfigUtils.setLogLevel;
import static com.hartwig.hmftools.lilac.LilacConfig.LL_LOGGER;
import static com.hartwig.hmftools.lilac.LilacConstants.A_EXON_BOUNDARIES;
import static com.hartwig.hmftools.lilac.LilacConstants.BASE_QUAL_PERCENTILE;
import static com.hartwig.hmftools.lilac.LilacConstants.B_EXON_BOUNDARIES;
import static com.hartwig.hmftools.lilac.LilacConstants.C_EXON_BOUNDARIES;
import static com.hartwig.hmftools.lilac.LilacConstants.FATAL_LOW_COVERAGE_THRESHOLD;
import static com.hartwig.hmftools.lilac.LilacConstants.GENE_A;
import static com.hartwig.hmftools.lilac.LilacConstants.GENE_B;
import static com.hartwig.hmftools.lilac.LilacConstants.GENE_C;
import static com.hartwig.hmftools.lilac.LilacConstants.HLA_A;
import static com.hartwig.hmftools.lilac.LilacConstants.HLA_B;
import static com.hartwig.hmftools.lilac.LilacConstants.HLA_C;
import static com.hartwig.hmftools.lilac.LilacConstants.ITEM_DELIM;
import static com.hartwig.hmftools.lilac.LilacConstants.LILAC_FILE_ALL_FRAGMENTS;
import static com.hartwig.hmftools.lilac.LilacConstants.LILAC_FILE_CANDIDATE_AA;
import static com.hartwig.hmftools.lilac.LilacConstants.LILAC_FILE_CANDIDATE_COVERAGE;
import static com.hartwig.hmftools.lilac.LilacConstants.LILAC_FILE_CANDIDATE_FRAGS;
import static com.hartwig.hmftools.lilac.LilacConstants.LILAC_FILE_CANDIDATE_NUC;
import static com.hartwig.hmftools.lilac.LilacConstants.LILAC_FILE_SOMATIC_VCF;
import static com.hartwig.hmftools.lilac.LilacConstants.WARN_LOW_COVERAGE_DEPTH;
import static com.hartwig.hmftools.lilac.evidence.Candidates.addPhasedCandidates;
import static com.hartwig.hmftools.lilac.fragment.NucleotideFragmentFactory.calculateGeneCoverage;
import static com.hartwig.hmftools.lilac.seq.SequenceCount.extractHeterozygousLociSequences;
import static com.hartwig.hmftools.lilac.evidence.NucleotideFiltering.calcNucleotideHeterogygousLoci;
import static com.hartwig.hmftools.lilac.coverage.HlaComplex.findDuplicates;
import static com.hartwig.hmftools.lilac.fragment.FragmentScope.CANDIDATE;
import static com.hartwig.hmftools.lilac.fragment.FragmentScope.SOLUTION;
import static com.hartwig.hmftools.lilac.variant.SomaticCodingCount.addVariant;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.hla.LilacAllele;
import com.hartwig.hmftools.common.hla.LilacQcData;
import com.hartwig.hmftools.common.utils.version.VersionInfo;
import com.hartwig.hmftools.lilac.coverage.HlaYCoverage;
import com.hartwig.hmftools.lilac.evidence.Candidates;
import com.hartwig.hmftools.lilac.coverage.FragmentAlleleMapper;
import com.hartwig.hmftools.lilac.coverage.AlleleCoverage;
import com.hartwig.hmftools.lilac.coverage.HlaComplex;
import com.hartwig.hmftools.lilac.coverage.ComplexBuilder;
import com.hartwig.hmftools.lilac.coverage.ComplexCoverage;
import com.hartwig.hmftools.lilac.coverage.ComplexCoverageCalculator;
import com.hartwig.hmftools.lilac.coverage.ComplexCoverageRanking;
import com.hartwig.hmftools.lilac.coverage.HlaComplexFile;
import com.hartwig.hmftools.lilac.evidence.PhasedEvidence;
import com.hartwig.hmftools.lilac.evidence.PhasedEvidenceFactory;
import com.hartwig.hmftools.lilac.fragment.AminoAcidFragmentPipeline;
import com.hartwig.hmftools.lilac.fragment.FragmentUtils;
import com.hartwig.hmftools.lilac.fragment.NucleotideGeneEnrichment;
import com.hartwig.hmftools.lilac.hla.HlaAllele;
import com.hartwig.hmftools.lilac.hla.HlaContext;
import com.hartwig.hmftools.lilac.hla.HlaContextFactory;
import com.hartwig.hmftools.lilac.fragment.Fragment;
import com.hartwig.hmftools.lilac.fragment.NucleotideFragmentFactory;
import com.hartwig.hmftools.lilac.read.BamReader;
import com.hartwig.hmftools.lilac.read.Indel;
import com.hartwig.hmftools.lilac.seq.SequenceCount;
import com.hartwig.hmftools.lilac.variant.CopyNumberAssignment;
import com.hartwig.hmftools.lilac.qc.AminoAcidQC;
import com.hartwig.hmftools.lilac.qc.BamQC;
import com.hartwig.hmftools.lilac.qc.CoverageQC;
import com.hartwig.hmftools.lilac.qc.HaplotypeQC;
import com.hartwig.hmftools.lilac.qc.SolutionSummary;
import com.hartwig.hmftools.lilac.qc.LilacQC;
import com.hartwig.hmftools.lilac.qc.SomaticVariantQC;
import com.hartwig.hmftools.lilac.coverage.FragmentAlleles;
import com.hartwig.hmftools.lilac.read.BamRecordReader;
import com.hartwig.hmftools.lilac.seq.HlaSequenceLoci;
import com.hartwig.hmftools.lilac.variant.LilacVCF;
import com.hartwig.hmftools.lilac.variant.SomaticCodingCount;
import com.hartwig.hmftools.lilac.variant.SomaticVariant;
import com.hartwig.hmftools.lilac.variant.SomaticVariantAnnotation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;

public class LilacApplication
{
    private final LilacConfig mConfig;
    private final ReferenceData mRefData;

    private BamReader mRefBamReader;
    private BamReader mTumorBamReader;

    private AminoAcidFragmentPipeline mAminoAcidPipeline;
    private final NucleotideGeneEnrichment mNucleotideGeneEnrichment;
    private NucleotideFragmentFactory mNucleotideFragFactory;

    private FragmentAlleleMapper mFragAlleleMapper;
    private HlaYCoverage mHlaYCoverage;

    // key state and results
    private SequenceCount mRefAminoAcidCounts;
    private SequenceCount mRefNucleotideCounts;

    private List<ComplexCoverage> mRankedComplexes;
    private List<Fragment> mRefNucleotideFrags;
    private List<FragmentAlleles> mRefFragAlleles;
    private final List<SomaticVariant> mSomaticVariants;
    private final List<SomaticCodingCount> mSomaticCodingCounts;

    private ComplexCoverage mTumorCoverage;
    private final List<Double> mTumorCopyNumber;
    private ComplexCoverage mRnaCoverage;

    private LilacQC mSummaryMetrics;
    private SolutionSummary mSolutionSummary;

    public LilacApplication(final LilacConfig config)
    {
        mConfig = config;
        mRefData = new ReferenceData(mConfig.ResourceDir, mConfig);

        mAminoAcidPipeline = null;
        mNucleotideGeneEnrichment = new NucleotideGeneEnrichment(A_EXON_BOUNDARIES, B_EXON_BOUNDARIES, C_EXON_BOUNDARIES);
        mNucleotideFragFactory = null;
        mFragAlleleMapper = null;
        mHlaYCoverage = null;

        mRefBamReader = null;
        mTumorBamReader = null;

        // key state and results
        mRefAminoAcidCounts = null;
        mRefNucleotideCounts = null;

        mRankedComplexes = Lists.newArrayList();
        mRefNucleotideFrags = Lists.newArrayList();
        mRefFragAlleles = Lists.newArrayList();

        mSomaticVariants = Lists.newArrayList();
        mSomaticCodingCounts = Lists.newArrayList();

        mTumorCoverage = null;
        mTumorCopyNumber = Lists.newArrayList(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        mRnaCoverage = null;
    }

    public void setBamReaders(final BamReader refBamReader, final BamReader tumorBamReader)
    {
        mRefBamReader = refBamReader;
        mTumorBamReader = tumorBamReader;
    }

    public ReferenceData getReferenceData() { return mRefData; }

    public LilacQC getSummaryMetrics() { return mSummaryMetrics; }
    public SolutionSummary getSolutionSummary() { return mSolutionSummary; }
    public List<ComplexCoverage> getRankedComplexes() { return mRankedComplexes; }

    public boolean run()
    {
        if(!mConfig.isValid())
        {
            LL_LOGGER.warn("invalid config, exiting");
            System.exit(1);
        }

        final VersionInfo version = new VersionInfo("lilac.version");
        LL_LOGGER.info("Lilac version({}), key parameters:", version.version());
        mConfig.logParams();

        HlaContextFactory hlaContextFactory = new HlaContextFactory(A_EXON_BOUNDARIES, B_EXON_BOUNDARIES, C_EXON_BOUNDARIES);
        HlaContext hlaAContext = hlaContextFactory.hlaA();
        HlaContext hlaBContext = hlaContextFactory.hlaB();
        HlaContext hlaCContext = hlaContextFactory.hlaC();

        if(!mRefData.load())
        {
            LL_LOGGER.error("reference data loading failed");
            System.exit(1);
        }

        boolean allValid = true;

        LL_LOGGER.info("querying records from reference bam {}", mConfig.ReferenceBam);

        mNucleotideFragFactory = new NucleotideFragmentFactory(
                mConfig.MinBaseQual, mRefData.AminoAcidSequencesWithInserts, mRefData.AminoAcidSequencesWithDeletes,
                mRefData.LociPositionFinder);

        if(mRefBamReader == null)
            mRefBamReader = new BamRecordReader(mConfig.ReferenceBam, mConfig.RefGenome, mRefData.HlaTranscriptData, mNucleotideFragFactory);

        if(mTumorBamReader == null)
            mTumorBamReader = new BamRecordReader(mConfig.TumorBam, mConfig.RefGenome, mRefData.HlaTranscriptData, mNucleotideFragFactory);

        mRefNucleotideFrags.addAll(mNucleotideGeneEnrichment.enrich(mRefBamReader.readFromBam()));

        int medianBaseQuality = mNucleotideFragFactory.calculatePercentileBaseQuality(mRefNucleotideFrags, BASE_QUAL_PERCENTILE);

        if(medianBaseQuality < mConfig.MinBaseQual)
        {
            LL_LOGGER.info("lowering min base quality({}) to median({})", mConfig.MinBaseQual, medianBaseQuality);
            mConfig.MinBaseQual = medianBaseQuality;
        }

        final Map<String,int[]> geneBaseDepth = calculateGeneCoverage(mRefNucleotideFrags);
        if(!hasSufficientGeneDepth(geneBaseDepth))
        {
            writeFailedSampleFileOutputs(geneBaseDepth, medianBaseQuality);
            return false;
        }

        allValid &= validateFragments(mRefNucleotideFrags);

        mAminoAcidPipeline = new AminoAcidFragmentPipeline(mConfig, mRefNucleotideFrags);

        List<Fragment> refAminoAcidFrags = mAminoAcidPipeline.getReferenceFragments();
        int totalFragmentCount = refAminoAcidFrags.size();

        double minEvidence = mConfig.calcMinEvidence(totalFragmentCount);

        LL_LOGGER.info(String.format("totalFrags(%d) minEvidence(%.1f) minHighQualEvidence(%.1f)",
                totalFragmentCount, minEvidence, mConfig.calcMinHighQualEvidence(totalFragmentCount)));

        // apply special filtering and splice checks on fragments, just for use in phasing
        List<Fragment> aCandidateFrags = mAminoAcidPipeline.referencePhasingFragments(hlaAContext);
        List<Fragment> bCandidateFrags = mAminoAcidPipeline.referencePhasingFragments(hlaBContext);
        List<Fragment> cCandidateFrags = mAminoAcidPipeline.referencePhasingFragments(hlaCContext);

        // determine un-phased Candidates
        Candidates candidateFactory = new Candidates(minEvidence, mRefData.NucleotideSequences, mRefData.AminoAcidSequences);
        List<HlaAllele> aUnphasedCandidates = candidateFactory.unphasedCandidates(hlaAContext, aCandidateFrags, mRefData.CommonAlleles);
        List<HlaAllele> bUnphasedCandidates = candidateFactory.unphasedCandidates(hlaBContext, bCandidateFrags, mRefData.CommonAlleles);
        List<HlaAllele> cUnphasedCandidates = candidateFactory.unphasedCandidates(hlaCContext, cCandidateFrags, mRefData.CommonAlleles);

        // determine phasing of amino acids
        PhasedEvidenceFactory phasedEvidenceFactory = new PhasedEvidenceFactory(mConfig, minEvidence);
        List<PhasedEvidence> aPhasedEvidence = phasedEvidenceFactory.evidence(hlaAContext, aCandidateFrags);
        List<PhasedEvidence> bPhasedEvidence = phasedEvidenceFactory.evidence(hlaBContext, bCandidateFrags);
        List<PhasedEvidence> cPhasedEvidence = phasedEvidenceFactory.evidence(hlaCContext, cCandidateFrags);

        // validate phasing against expected sequences
        if(!mConfig.ActualAlleles.isEmpty() && LL_LOGGER.isDebugEnabled())
        {
            List<HlaSequenceLoci> actualSequences = mRefData.AminoAcidSequences.stream()
                    .filter(x -> mConfig.ActualAlleles.contains(x.Allele.asFourDigit())).collect(Collectors.toList());

            PhasedEvidence.logInconsistentEvidence(GENE_A, aPhasedEvidence, actualSequences);
            PhasedEvidence.logInconsistentEvidence(GENE_B, bPhasedEvidence, actualSequences);
            PhasedEvidence.logInconsistentEvidence(GENE_C, cPhasedEvidence, actualSequences);
        }

        // gather up all phased candidates
        List<HlaAllele> candidateAlleles = Lists.newArrayList();
        List<HlaAllele> aCandidates = candidateFactory.phasedCandidates(hlaAContext, aUnphasedCandidates, aPhasedEvidence);
        List<HlaAllele> bCandidates = candidateFactory.phasedCandidates(hlaBContext, bUnphasedCandidates, bPhasedEvidence);
        List<HlaAllele> cCandidates = candidateFactory.phasedCandidates(hlaCContext, cUnphasedCandidates, cPhasedEvidence);

        addPhasedCandidates(candidateAlleles, aCandidates, mConfig, mRefData);
        addPhasedCandidates(candidateAlleles, bCandidates, mConfig, mRefData);
        addPhasedCandidates(candidateAlleles, cCandidates, mConfig, mRefData);

        List<HlaAllele> recoveredAlleles = Lists.newArrayList();

        // make special note of the known stop-loss INDEL on HLA-C
        Map<HlaAllele,List<Fragment>> knownStopLossFragments = Maps.newHashMap();

        for(Map.Entry<Indel,List<Fragment>> entry : mRefBamReader.getKnownStopLossFragments().entrySet())
        {
            HlaAllele allele = mRefData.KnownStopLossIndelAlleles.get(entry.getKey());

            if(allele != null)
            {
                LL_LOGGER.info("recovering stop loss allele({}) with {} fragments", allele, entry.getValue().size());

                knownStopLossFragments.put(allele, entry.getValue());

                if(!candidateAlleles.contains(allele))
                    recoveredAlleles.add(allele);
            }
        }

        if(mConfig.RestrictedAlleles.isEmpty())
        {
            // add common alleles - either if supported or forced for inclusion
            List<HlaAllele> missedCommonAlleles = mRefData.CommonAlleles.stream()
                    .filter(x -> !candidateAlleles.contains(x))
                    .filter(x -> !recoveredAlleles.contains(x))
                    .collect(Collectors.toList());

            if(!missedCommonAlleles.isEmpty())
            {
                Collections.sort(missedCommonAlleles);
                LL_LOGGER.info("recovering common alleles: {}", HlaAllele.toString(missedCommonAlleles));
                recoveredAlleles.addAll(missedCommonAlleles);
            }
        }

        candidateAlleles.addAll(recoveredAlleles);

        List<HlaAllele> missingExpected = mConfig.ActualAlleles.stream().filter(x -> !candidateAlleles.contains(x)).collect(Collectors.toList());

        if (!missingExpected.isEmpty())
        {
            LL_LOGGER.info("  including {} known actual alleles as candidates: {}",
                    missingExpected.size(), HlaAllele.toString(missingExpected));

            candidateAlleles.addAll(missingExpected);
        }

        List<HlaSequenceLoci> candidateSequences = mRefData.AminoAcidSequences.stream()
                .filter(x -> candidateAlleles.contains(x.Allele)).collect(Collectors.toList());

        // calculate allele coverage
        mRefAminoAcidCounts = SequenceCount.aminoAcids(minEvidence, refAminoAcidFrags);
        mRefNucleotideCounts = SequenceCount.nucleotides(minEvidence, refAminoAcidFrags);

        Map<String,List<Integer>> refNucleotideHetLociMap = calcNucleotideHeterogygousLoci(mRefNucleotideCounts.heterozygousLoci());

        List<HlaSequenceLoci> candidateNucSequences = mRefData.NucleotideSequences.stream()
                .filter(x -> candidateAlleles.contains(x.Allele.asFourDigit())).collect(Collectors.toList());

        List<HlaSequenceLoci> recoveredSequences = mRefData.AminoAcidSequences.stream()
                .filter(x -> recoveredAlleles.contains(x.Allele)).collect(Collectors.toList());

        Map<String,Map<Integer,Set<String>>> geneAminoAcidHetLociMap =
                extractHeterozygousLociSequences(mAminoAcidPipeline.getReferenceAminoAcidCounts(), minEvidence, recoveredSequences);

        mFragAlleleMapper = new FragmentAlleleMapper(
                geneAminoAcidHetLociMap, refNucleotideHetLociMap, mAminoAcidPipeline.getReferenceNucleotides());

        mFragAlleleMapper.setKnownStopLossAlleleFragments(knownStopLossFragments);

        mRefFragAlleles.addAll(mFragAlleleMapper.createFragmentAlleles(refAminoAcidFrags, candidateSequences, candidateNucSequences));

        if(mRefFragAlleles.isEmpty())
        {
            LL_LOGGER.fatal("failed to assign fragments to alleles");
            System.exit(1);
        }

        mHlaYCoverage = new HlaYCoverage(mRefData.HlaYNucleotideSequences, geneAminoAcidHetLociMap, mConfig);
        mHlaYCoverage.checkThreshold(mRefFragAlleles, refAminoAcidFrags);

        // build and score complexes
        ComplexBuilder complexBuilder = new ComplexBuilder(mConfig, mRefData);

        complexBuilder.filterCandidates(mRefFragAlleles, candidateAlleles, recoveredAlleles);
        allValid &= validateAlleles(complexBuilder.getUniqueProteinAlleles());

        // reassess fragment-allele assignment with the reduced set of filtered alleles
        List<HlaAllele> confirmedRecoveredAlleles = complexBuilder.getConfirmedRecoveredAlleles();
        recoveredSequences = recoveredSequences.stream()
                .filter(x -> confirmedRecoveredAlleles.contains(x.Allele)).collect(Collectors.toList());

        geneAminoAcidHetLociMap =
                extractHeterozygousLociSequences(mAminoAcidPipeline.getReferenceAminoAcidCounts(), minEvidence, recoveredSequences);

        mFragAlleleMapper.setHetAminoAcidLoci(geneAminoAcidHetLociMap);
        mHlaYCoverage.updateAminoAcidLoci(geneAminoAcidHetLociMap);

        List<HlaAllele> confirmedAlleles = complexBuilder.getUniqueProteinAlleles();

        candidateSequences = candidateSequences.stream()
                .filter(x -> confirmedAlleles.contains(x.Allele)).collect(Collectors.toList());

        candidateNucSequences = candidateNucSequences.stream()
                .filter(x -> confirmedAlleles.contains(x.Allele.asFourDigit())).collect(Collectors.toList());

        mRefFragAlleles.clear();
        mRefFragAlleles.addAll(mFragAlleleMapper.createFragmentAlleles(refAminoAcidFrags, candidateSequences, candidateNucSequences));

        List<HlaComplex> complexes = complexBuilder.buildComplexes(mRefFragAlleles, confirmedRecoveredAlleles);
        // allValid &= validateComplexes(complexes); // too expensive in current form even for validation, address in unit tests instead

        LL_LOGGER.info("calculating coverage of {} complexes", complexes.size());
        ComplexCoverageCalculator complexCalculator = new ComplexCoverageCalculator(mConfig.Threads, mConfig.TopScoreThreshold);
        List<ComplexCoverage> calculatedComplexes = complexCalculator.calculateComplexCoverages(mRefFragAlleles, complexes);

        ComplexCoverageRanking complexRanker = new ComplexCoverageRanking(mConfig.TopScoreThreshold, mRefData);
        mRankedComplexes.addAll(complexRanker.rankCandidates(calculatedComplexes, recoveredAlleles, candidateSequences));

        if(mRankedComplexes.isEmpty())
        {
            LL_LOGGER.fatal("failed to calculate complex coverage");
            System.exit(1);
        }

        ComplexCoverage winningRefCoverage = mRankedComplexes.get(0);

        if(!mConfig.ActualAlleles.isEmpty())
        {
            // search amongst the ranked candidates otherwise manually compute coverage
            ComplexCoverage actualAllelesCoverage = null;

            for(ComplexCoverage rankedCoverage : mRankedComplexes)
            {
                if(rankedCoverage.getAlleles().size() != mConfig.ActualAlleles.size())
                    continue;

                if(rankedCoverage.getAlleles().stream().allMatch(x -> mConfig.ActualAlleles.contains(x)))
                {
                    actualAllelesCoverage = rankedCoverage;
                    break;
                }
            }

            if(actualAllelesCoverage == null)
            {
                actualAllelesCoverage = ComplexBuilder.calcProteinCoverage(mRefFragAlleles, mConfig.ActualAlleles);

            }

            LL_LOGGER.info("forcing winning coverage to actual alleles");
            winningRefCoverage = actualAllelesCoverage;
        }

        winningRefCoverage.expandToSixAlleles();

        List<HlaAllele> winningAlleles = winningRefCoverage.getAlleles();

        mHlaYCoverage.assignReferenceFragments(winningAlleles, mRefFragAlleles, refAminoAcidFrags);

        List<HlaSequenceLoci> winningSequences = candidateSequences.stream()
                .filter(x -> winningAlleles.contains(x.Allele)).collect(Collectors.toList());

        List<HlaSequenceLoci> winningNucSequences = candidateNucSequences.stream()
                .filter(x -> winningAlleles.contains(x.Allele.asFourDigit())).collect(Collectors.toList());

        LL_LOGGER.info("{}", HlaComplexFile.header());

        for (ComplexCoverage rankedComplex : mRankedComplexes)
        {
            LL_LOGGER.info(HlaComplexFile.asString(rankedComplex));
        }

        // log key results for fast post-run analysis
        StringJoiner totalCoverages = new StringJoiner(",");
        winningRefCoverage.getAlleleCoverage().forEach(x -> totalCoverages.add(String.format("%.0f",x.TotalCoverage)));

        double scoreMargin = 0;
        StringJoiner nextSolutionInfo = new StringJoiner(ITEM_DELIM);

        if(mRankedComplexes.size() > 1)
        {
            ComplexCoverage nextSolution = mRankedComplexes.get(1);
            scoreMargin = mRankedComplexes.get(0).getScore() - nextSolution.getScore();
            nextSolution.getAlleles().stream().filter(x -> !winningAlleles.contains(x)).forEach(x -> nextSolutionInfo.add(x.toString()));
        }

        LL_LOGGER.info("WINNERS_{}: {}, {}, {}, {}, {}, {}",
                mConfig.RunId.isEmpty() ? "REF" : mConfig.RunId, mConfig.Sample, mRankedComplexes.size(),
                HlaAllele.toString(winningRefCoverage.getAlleles()), String.format("%.3f", scoreMargin), nextSolutionInfo, totalCoverages);

        // write fragment assignment data
        for(FragmentAlleles fragAllele : mRefFragAlleles)
        {
            if(fragAllele.getFragment().isScopeSet())
                continue;

            if(winningAlleles.stream().anyMatch(x -> fragAllele.contains(x)))
                fragAllele.getFragment().setScope(SOLUTION);
            else
                fragAllele.getFragment().setScope(CANDIDATE);
        }

        mSomaticCodingCounts.addAll(SomaticCodingCount.create(winningAlleles));

        extractTumorResults(winningAlleles, winningRefCoverage, winningSequences, winningNucSequences);

        extractRnaCoverage(winningAlleles, winningSequences, winningNucSequences);

        if(!allValid)
        {
            LL_LOGGER.error("failed validation");
            writeFailedSampleFileOutputs(geneBaseDepth, medianBaseQuality);
            return false;
        }

        // create various QC and other metrics
        SomaticVariantQC somaticVariantQC = SomaticVariantQC.create(mSomaticVariants.size(), mSomaticCodingCounts);

        List<PhasedEvidence> combinedPhasedEvidence = Lists.newArrayList();
        combinedPhasedEvidence.addAll(aPhasedEvidence);
        combinedPhasedEvidence.addAll(bPhasedEvidence);
        combinedPhasedEvidence.addAll(cPhasedEvidence);

        List<Fragment> unmatchedFrags = refAminoAcidFrags.stream().filter(x -> x.scope().isUnmatched()).collect(Collectors.toList());

        HaplotypeQC haplotypeQC = HaplotypeQC.create(
                winningSequences, mRefData.HlaYAminoAcidSequences, combinedPhasedEvidence, mRefAminoAcidCounts, unmatchedFrags);

        AminoAcidQC aminoAcidQC = AminoAcidQC.create(
                winningSequences, mRefData.HlaYAminoAcidSequences, mRefAminoAcidCounts,
                haplotypeQC.UnmatchedHaplotypes, totalFragmentCount);

        BamQC bamQC = BamQC.create(mRefBamReader, geneBaseDepth);
        CoverageQC coverageQC = CoverageQC.create(refAminoAcidFrags, winningRefCoverage);

        mSummaryMetrics = new LilacQC(
                scoreMargin, nextSolutionInfo.toString(), medianBaseQuality, mHlaYCoverage.getSelectedAllele(),
                aminoAcidQC, bamQC, coverageQC, haplotypeQC, somaticVariantQC);

        mSolutionSummary = SolutionSummary.create(winningRefCoverage, mTumorCoverage, mTumorCopyNumber, mSomaticCodingCounts, mRnaCoverage);

        return true;
    }

    public void extractTumorResults(
            final List<HlaAllele> winningAlleles, final ComplexCoverage winningRefCoverage,
            final List<HlaSequenceLoci> winningSequences, final List<HlaSequenceLoci> winningNucSequences)
    {
        if(mConfig.TumorBam.isEmpty())
        {
            mTumorCoverage = ComplexCoverage.create(Lists.newArrayList());
            return;
        }

        List<Fragment> tumorNucleotideFrags = mNucleotideGeneEnrichment.enrich(mTumorBamReader.readFromBam());

        List<Fragment> tumorFragments = mAminoAcidPipeline.calcComparisonCoverageFragments(tumorNucleotideFrags);

        LL_LOGGER.info("calculating tumor coverage from frags({} highQual={})", tumorNucleotideFrags.size(), tumorFragments.size());

        List<FragmentAlleles> tumorFragAlleles = mFragAlleleMapper.createFragmentAlleles(tumorFragments, winningSequences, winningNucSequences);

        if(mHlaYCoverage.exceedsThreshold())
            mHlaYCoverage.assignFragments(winningAlleles, tumorFragAlleles, tumorFragments, "TUMOR");

        mTumorCoverage = ComplexBuilder.calcProteinCoverage(tumorFragAlleles, winningAlleles);

        mTumorCoverage.populateMissingCoverage(winningAlleles);

        if(!mConfig.CopyNumberFile.isEmpty())
        {
            CopyNumberAssignment copyNumberAssignment = new CopyNumberAssignment();
            copyNumberAssignment.loadCopyNumberData(mConfig);

            copyNumberAssignment.assign(mConfig.Sample, winningAlleles, winningRefCoverage, mTumorCoverage, mTumorCopyNumber);
        }

        // SOMATIC VARIANTS
        SomaticVariantAnnotation variantAnnotation = new SomaticVariantAnnotation(
                mConfig, mRefData.HlaTranscriptData, mRefData.LociPositionFinder);

        if(variantAnnotation.getSomaticVariants().isEmpty())
            return;

        mSomaticVariants.addAll(variantAnnotation.getSomaticVariants());

        LL_LOGGER.info("calculating somatic variant allele coverage");

        boolean hasVcfData = mSomaticVariants.stream().anyMatch(x -> x.Context != null);
        LilacVCF lilacVCF = null;

        if(hasVcfData)
        {
            final VersionInfo version = new VersionInfo("lilac.version");
            lilacVCF = new LilacVCF(mConfig.formFileId(LILAC_FILE_SOMATIC_VCF), mConfig.SomaticVariantsFile).writeHeader(version.toString());
        }

        for(SomaticVariant variant : mSomaticVariants)
        {
            List<AlleleCoverage> variantCoverage = variantAnnotation.assignAlleleCoverage(variant, mTumorBamReader, winningSequences);
            List<HlaAllele> variantAlleles = variantCoverage.stream().map(x -> x.Allele).collect(Collectors.toList());
            LL_LOGGER.info("  {} -> {}}", variant, variantCoverage);

            if(lilacVCF != null && variant.Context != null)
                lilacVCF.writeVariant(variant.Context, variantAlleles);

            addVariant(mSomaticCodingCounts, variant, variantAlleles);
        }

        if(lilacVCF != null)
            lilacVCF.close();
    }

    public void extractRnaCoverage(
            final List<HlaAllele> winningAlleles, final List<HlaSequenceLoci> winningSequences, final List<HlaSequenceLoci> winningNucSequences)
    {
        if(mConfig.RnaBam.isEmpty())
        {
            mRnaCoverage = ComplexCoverage.create(Lists.newArrayList());
            return;
        }

        BamRecordReader rnaBamReader = new BamRecordReader(mConfig.RnaBam, mConfig.RefGenome, mRefData.HlaTranscriptData, mNucleotideFragFactory);

        List<Fragment> rnaNucleotideFrags = mNucleotideGeneEnrichment.enrich(rnaBamReader.readFromBam());

        List<Fragment> rnaFragments = mAminoAcidPipeline.calcComparisonCoverageFragments(rnaNucleotideFrags);

        LL_LOGGER.info("calculating RNA coverage from frags({} highQual={})", rnaNucleotideFrags.size(), rnaFragments.size());

        List<FragmentAlleles> rnaFragAlleles = mFragAlleleMapper.createFragmentAlleles(rnaFragments, winningSequences, winningNucSequences);

        if(mHlaYCoverage.exceedsThreshold())
            mHlaYCoverage.assignFragments(winningAlleles, rnaFragAlleles, rnaFragments, "RNA");

        mRnaCoverage = ComplexBuilder.calcProteinCoverage(rnaFragAlleles, winningAlleles);

        mRnaCoverage.populateMissingCoverage(winningAlleles);
    }

    public void writeFileOutputs()
    {
        if(mConfig.OutputDir.isEmpty())
            return;

        mSummaryMetrics.log(mConfig.Sample);

        LL_LOGGER.info("writing output to {}", mConfig.OutputDir);

        mSolutionSummary.write(LilacAllele.generateFilename(mConfig.OutputDir, mConfig.Sample));
        mSummaryMetrics.writefile(LilacQcData.generateFilename(mConfig.OutputDir, mConfig.Sample));

        HlaComplexFile.writeToFile(mConfig.formFileId(LILAC_FILE_CANDIDATE_COVERAGE), mRankedComplexes);

        if(mConfig.WriteAllFiles)
        {
            HlaComplexFile.writeFragmentAssignment(mConfig.formFileId(LILAC_FILE_CANDIDATE_FRAGS), mRankedComplexes, mRefFragAlleles);
            mRefAminoAcidCounts.writeVertically(mConfig.formFileId(LILAC_FILE_CANDIDATE_AA));
            mRefNucleotideCounts.writeVertically(mConfig.formFileId(LILAC_FILE_CANDIDATE_NUC));
            mAminoAcidPipeline.writeCounts(mConfig);
            FragmentUtils.writeFragmentData(mConfig.formFileId(LILAC_FILE_ALL_FRAGMENTS), mRefNucleotideFrags);
            mHlaYCoverage.writeAlleleCounts(mConfig.Sample);
        }
    }

    public void writeFailedSampleFileOutputs(final Map<String,int[]> geneBaseDepth, int medianBaseQuality)
    {
        if(mConfig.OutputDir.isEmpty())
            return;

        LL_LOGGER.info("writing failed-sample output to {}", mConfig.OutputDir);

        HaplotypeQC haplotypeQC = new HaplotypeQC(0, 0, 0, Lists.newArrayList());

        AminoAcidQC aminoAcidQC = new AminoAcidQC(0, 0);

        BamQC bamQC = new BamQC(0, 0, 0, geneBaseDepth);

        CoverageQC coverageQC = new CoverageQC(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        mSummaryMetrics = new LilacQC(
                0, "", 0, null,
                aminoAcidQC, bamQC, coverageQC, haplotypeQC, new SomaticVariantQC(0, 0));

        mSolutionSummary = new SolutionSummary(null, mTumorCoverage, mTumorCopyNumber, mSomaticCodingCounts, mRnaCoverage);

        mSolutionSummary.write(LilacAllele.generateFilename(mConfig.OutputDir, mConfig.Sample));
        mSummaryMetrics.writefile(LilacQcData.generateFilename(mConfig.OutputDir, mConfig.Sample));
    }

    private boolean validateFragments(final List<Fragment> fragments)
    {
        if(!mConfig.RunValidation)
            return true;

        List<Fragment> invalidFragments = fragments.stream().filter(x -> !x.validate()).collect(Collectors.toList());
        if(invalidFragments.isEmpty())
            return true;


        LL_LOGGER.warn("has {} invalid fragments", invalidFragments.size());
        return false;
    }

    private boolean validateAlleles(final List<HlaAllele> alleles)
    {
        // check uniqueness amongst valid candidate alleles
        if(!mConfig.RunValidation)
            return true;

        Set<HlaAllele> duplicateAlleles = HlaAllele.findDuplicates(alleles);
        if(duplicateAlleles.isEmpty())
            return  true;

        LL_LOGGER.warn("has {} duplicate alleles from complex building", duplicateAlleles.size());
        return false;
    }

    private boolean validateComplexes(final List<HlaComplex> complexes)
    {
        if(!mConfig.RunValidation)
            return true;

        Set<HlaComplex> duplicates = findDuplicates(complexes);
        if(duplicates.isEmpty())
            return true;

        LL_LOGGER.warn("has {} duplicate complexes", duplicates.size());
        return false;
    }

    private boolean hasSufficientGeneDepth(final Map<String,int[]> geneBaseDepth)
    {
        int aLowCoverage = (int) Arrays.stream(geneBaseDepth.get(HLA_A)).filter(x -> x < WARN_LOW_COVERAGE_DEPTH).count();
        int bLowCoverage = (int)Arrays.stream(geneBaseDepth.get(HLA_B)).filter(x -> x < WARN_LOW_COVERAGE_DEPTH).count();
        int cLowCoverage = (int)Arrays.stream(geneBaseDepth.get(HLA_C)).filter(x -> x < WARN_LOW_COVERAGE_DEPTH).count();

        if(!mConfig.ReferenceBam.isEmpty() && aLowCoverage + bLowCoverage + cLowCoverage >= FATAL_LOW_COVERAGE_THRESHOLD)
        {
            LL_LOGGER.warn("gene depth coverage(A={} B={} C={}) too low, exiting", aLowCoverage, bLowCoverage, cLowCoverage);
            return false;
        }

        return true;
    }

    public static void main(@NotNull final String[] args) throws ParseException
    {
        final Options options = LilacConfig.createOptions();

        try
        {
            final CommandLine cmd = createCommandLine(args, options);

            setLogLevel(cmd);

            LilacApplication lilac = new LilacApplication(new LilacConfig(cmd));

            long startTime = System.currentTimeMillis();

            if(lilac.run())
            {
                lilac.writeFileOutputs();
            }

            long endTime = System.currentTimeMillis();
            double runTime = (endTime - startTime) / 1000.0;

            LL_LOGGER.info("Lilac complete, run time({}s)", String.format("%.2f", runTime));
        }
        catch(ParseException e)
        {
            LL_LOGGER.warn(e);
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("LilacApplication", options);
            System.exit(1);
        }
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException
    {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }
}
