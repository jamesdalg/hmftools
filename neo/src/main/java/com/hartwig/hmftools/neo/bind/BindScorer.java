package com.hartwig.hmftools.neo.bind;

import static com.hartwig.hmftools.common.utils.ConfigUtils.setLogLevel;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.neo.NeoCommon.NE_LOGGER;
import static com.hartwig.hmftools.neo.bind.BindData.loadBindData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.stats.AucCalc;
import com.hartwig.hmftools.common.stats.AucData;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

public class BindScorer
{
    private final ScoreConfig mConfig;

    private final Map<String,Map<Integer,List<BindData>>> mAllelePeptideData;
    private final Map<String,Map<Integer,BindScoreMatrix>> mAlleleBindMatrices;
    private final FlankScores mFlankScores;

    private final RandomPeptideDistribution mRandomDistribution;
    private final BindingLikelihood mBindingLikelihood;
    private final RecognitionSimilarity mRecognitionSimilarity;
    private final ExpressionLikelihood mExpressionLikelihood;

    private final Map<String,Integer> mBindDataOtherColumns;

    public BindScorer(final ScoreConfig config)
    {
        mConfig = config;

        mAllelePeptideData = Maps.newHashMap();
        mBindDataOtherColumns = Maps.newLinkedHashMap();

        mAlleleBindMatrices = Maps.newHashMap();

        mRandomDistribution = new RandomPeptideDistribution(config.RandomPeptides);
        mBindingLikelihood = new BindingLikelihood();
        mFlankScores = new FlankScores();
        mExpressionLikelihood = new ExpressionLikelihood();

        mRecognitionSimilarity = new RecognitionSimilarity();
        mRecognitionSimilarity.setCheckSelfSimilarity(mConfig.CheckSelfRecognition);
    }

    public BindScorer(
            final Map<String,Map<Integer,List<BindData>>> allelePeptideData,
            final Map<String,Map<Integer,BindScoreMatrix>> alleleBindMatrices, final RandomPeptideDistribution randomDistribution,
            final FlankScores flankScores, final ExpressionLikelihood expressionLikelihood)
    {
        mConfig = null; // not required when used as an internal calculator

        mAllelePeptideData = allelePeptideData;
        mBindDataOtherColumns = Maps.newLinkedHashMap();

        mAlleleBindMatrices = alleleBindMatrices;
        mRandomDistribution = randomDistribution;
        mBindingLikelihood = null;
        mFlankScores = flankScores;
        mExpressionLikelihood = expressionLikelihood;
        mRecognitionSimilarity = null;
    }

    public Set<String> getScoringAlleles() { return mAlleleBindMatrices.keySet(); }

    public void run()
    {
        NE_LOGGER.info("running BindScorer");

        if(!loadScoringData())
        {
            NE_LOGGER.error("invalid reference data");
            return;
        }

        if(!loadBindData(mConfig.ValidationDataFile, Lists.newArrayList(), mAllelePeptideData, mBindDataOtherColumns))
        {
            NE_LOGGER.error("invalid validation data");
            return;
        }

        runScoring();

        writeAlleleSummary();
        writePeptideScores();

        if(mConfig.CheckSelfRecognition)
        {
            String recogSimFilename = BindCommon.formFilename(mConfig.OutputDir, "recog_allele_sim", mConfig.OutputId);
            mRecognitionSimilarity.logCrossAlleleSimilarities(recogSimFilename);
        }

        NE_LOGGER.info("scoring complete");
    }

    public static final double INVALID_CALC = -1;

    public void runScoring()
    {
        NE_LOGGER.info("running scoring");

        for(Map.Entry<String,Map<Integer,List<BindData>>> alleleEntry : mAllelePeptideData.entrySet())
        {
            final String allele = alleleEntry.getKey();
            final Map<Integer,List<BindData>> pepLenBindDataMap = alleleEntry.getValue();

            Map<Integer,BindScoreMatrix> pepLenMatrixMap = mAlleleBindMatrices.get(allele);

            if(pepLenMatrixMap == null)
            {
                NE_LOGGER.warn("allele({}) has no matrix scoring data", allele);
                continue;
            }

            for(Map.Entry<Integer,List<BindData>> pepLenEntry : pepLenBindDataMap.entrySet())
            {
                final List<BindData> bindDataList = pepLenEntry.getValue();

                for(BindData bindData : bindDataList)
                {
                    BindScoreMatrix matrix = pepLenMatrixMap.get(bindData.peptideLength());

                    if(matrix == null)
                        continue;

                    calcScoreData(
                            bindData, matrix, mFlankScores, mRandomDistribution, mBindingLikelihood,
                            mExpressionLikelihood, mRecognitionSimilarity);
                }
            }
        }
    }

    public static double calcScore(
            final BindScoreMatrix matrix, final FlankScores flankScores, final String peptide, final String upFlank, final String downFlank)
    {
        double score = matrix.calcScore(peptide);

        if(flankScores.hasData())
        {
            double flankScore = flankScores.calcScore(upFlank, downFlank);
            score += flankScore;
        }

        return score;
    }

    public void calcScoreData(final BindData bindData)
    {
        if(!mAlleleBindMatrices.containsKey(bindData.Allele))
            return;

        BindScoreMatrix matrix = mAlleleBindMatrices.get(bindData.Allele).get(bindData.peptideLength());

        if(matrix == null)
            return;

        calcScoreData(
                bindData, matrix, mFlankScores, mRandomDistribution,
                mBindingLikelihood, mExpressionLikelihood, mRecognitionSimilarity);
    }

    public static void calcScoreData(
            final BindData bindData, final BindScoreMatrix matrix, final FlankScores flankScores,
            final RandomPeptideDistribution randomDistribution, final BindingLikelihood bindingLikelihood,
            final ExpressionLikelihood expressionLikelihood, final RecognitionSimilarity recognitionSimilarity)
    {
        double score = matrix.calcScore(bindData.Peptide);

        double flankScore = 0;
        if(flankScores.hasData() && bindData.hasFlanks())
        {
            flankScore = flankScores.calcScore(bindData.UpFlank, bindData.DownFlank);
            score += flankScore;
        }

        double rankPercentile = randomDistribution.getScoreRank(bindData.Allele, bindData.peptideLength(), score);

        double likelihood = INVALID_CALC;
        double likelihoodRank = INVALID_CALC;
        double expLikelihood = INVALID_CALC;
        double expLikelihoodRank = INVALID_CALC;
        double recogSimilarity = INVALID_CALC;
        double otherAlleleRecogSimilarity = INVALID_CALC;

        if(bindingLikelihood != null && bindingLikelihood.hasData())
        {
            likelihood = bindingLikelihood.getBindingLikelihood(bindData.Allele, bindData.Peptide, rankPercentile);

            likelihoodRank = randomDistribution.getLikelihoodRank(bindData.Allele, likelihood);

            if(expressionLikelihood != null && expressionLikelihood.hasData() && bindData.hasTPM())
            {
                double tpmLikelihood = expressionLikelihood.calcLikelihood(bindData.tpm());
                expLikelihood = likelihood * tpmLikelihood;
                expLikelihoodRank = randomDistribution.getExpressionLikelihoodRank(bindData.Allele, expLikelihood);
            }
        }

        if(recognitionSimilarity != null && recognitionSimilarity.hasData())
        {
            recogSimilarity = recognitionSimilarity.calcSimilarity(bindData.Allele, bindData.Peptide);
            otherAlleleRecogSimilarity = recognitionSimilarity.calcOtherAlleleSimilarity(bindData.Allele, bindData.Peptide);
        }

        bindData.setScoreData(
                score, flankScore, rankPercentile, likelihood, likelihoodRank, expLikelihood, expLikelihoodRank,
                recogSimilarity, otherAlleleRecogSimilarity);
    }

    private void writePeptideScores()
    {
        if(!mConfig.WritePeptideScores)
            return;

        String outputFile = BindCommon.formFilename(mConfig.OutputDir, "peptide_scores", mConfig.OutputId);
        NE_LOGGER.info("writing peptide scores to {}", outputFile);

        try
        {
            BufferedWriter writer = createBufferedWriter(outputFile, false);
            writer.write("Allele,Peptide,Source,Score,Rank,Likelihood,LikelihoodRank");

            boolean writeFlanks = mFlankScores.hasData() && mAllelePeptideData.values().stream().anyMatch(x -> x.values().stream()
                    .filter(y -> !y.isEmpty()).anyMatch(y -> y.get(0).hasFlanks()));

            boolean writeExpression = mExpressionLikelihood.hasData() && mAllelePeptideData.values().stream().anyMatch(x -> x.values().stream()
                    .filter(y -> !y.isEmpty()).anyMatch(y -> y.get(0).hasTPM()));

            if(writeFlanks)
                writer.write(",FlankScore,UpFlank,DownFlank");

            if(writeExpression)
                writer.write(",TPM,ExpLikelihood,ExpLikelihoodRank");

            boolean calcRecognitionSim = mRecognitionSimilarity != null && mRecognitionSimilarity.hasData();

            if(calcRecognitionSim)
                writer.write(",RecogSim");

            boolean hasOtherData = !mBindDataOtherColumns.isEmpty();

            if(hasOtherData)
            {
                for(String column : mBindDataOtherColumns.keySet())
                {
                    writer.write(String.format(",%s", column));
                }
            }

            writer.newLine();

            for(Map.Entry<String,Map<Integer,List<BindData>>> alleleEntry : mAllelePeptideData.entrySet())
            {
                final String allele = alleleEntry.getKey();
                final Map<Integer,List<BindData>> pepLenBindDataMap = alleleEntry.getValue();

                for(List<BindData> bindDataList : pepLenBindDataMap.values())
                {
                    for(BindData bindData : bindDataList)
                    {
                        writer.write(String.format("%s,%s,%s,%.4f,%.6f,%.6f,%.6f",
                                allele, bindData.Peptide, bindData.Source, bindData.score(),
                                bindData.rankPercentile(), bindData.likelihood(), bindData.likelihoodRank()));

                        if(writeFlanks)
                        {
                            writer.write(String.format(",%.4f,%s,%s", bindData.flankScore(), bindData.UpFlank, bindData.DownFlank));
                        }

                        if(writeExpression)
                        {
                            writer.write(String.format(",%.4f,%.6f,%.6f",
                                    bindData.tpm(), bindData.expressionLikelihood(), bindData.expressionLikelihoodRank()));
                        }

                        if(calcRecognitionSim)
                        {
                            writer.write(String.format(",%.1f", bindData.recognitionSimilarity()));
                        }

                        if(hasOtherData)
                        {
                            for(Integer columnIndex : mBindDataOtherColumns.values())
                            {
                                writer.write(String.format(",%s", bindData.getOtherData().get(columnIndex)));
                            }
                        }

                        writer.newLine();
                    }
                }
            }

            writer.close();
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to write peptide scores file: {}", e.toString());
        }
    }

    private void writeAlleleSummary()
    {
        if(!mConfig.WriteSummaryData)
            return;

        try
        {
            String outputFile = BindCommon.formFilename(mConfig.OutputDir, "allele_summary", mConfig.OutputId);
            BufferedWriter writer = createBufferedWriter(outputFile, false);
            writer.write("Allele,PeptideLength,BindCount,TPR,AUC");
            writer.newLine();

            for(Map.Entry<String,Map<Integer,List<BindData>>> alleleEntry : mAllelePeptideData.entrySet())
            {
                final String allele = alleleEntry.getKey();

                final Map<Integer,List<BindData>> pepLenBindDataMap = alleleEntry.getValue();

                List<AucData> alleleAucData = Lists.newArrayList();
                TprCalc alleleTprCalc = new TprCalc();

                for(Map.Entry<Integer,List<BindData>> pepLenEntry : pepLenBindDataMap.entrySet())
                {
                    int peptideLength = pepLenEntry.getKey();
                    final List<BindData> bindDataList = pepLenEntry.getValue();

                    TprCalc pepLenTprCalc = new TprCalc();

                    for(BindData bindData : bindDataList)
                    {
                        pepLenTprCalc.addRank(bindData.likelihoodRank());
                        alleleTprCalc.addRank(bindData.likelihoodRank());

                        alleleAucData.add(new AucData(true, bindData.likelihoodRank(), true));
                    }

                    writer.write(String.format("%s,%d,%d,%.4f,0",
                            allele, peptideLength, pepLenTprCalc.entryCount(), pepLenTprCalc.calc()));
                    writer.newLine();
                }

                double aucPerc = AucCalc.calcPercentilesAuc(alleleAucData, Level.TRACE);

                writer.write(String.format("%s,ALL,%d,%.4f,%.4f",
                        allele, alleleTprCalc.entryCount(), alleleTprCalc.calc(), aucPerc));
                writer.newLine();

            }

            writer.close();
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to init allele summary writer: {}", e.toString());
        }
    }

    public boolean loadScoringData()
    {
        List<BindScoreMatrix> matrixList = BindScoreMatrix.loadFromCsv(mConfig.PosWeightsFile);

        for(BindScoreMatrix matrix : matrixList)
        {
            Map<Integer,BindScoreMatrix> pepLenMap = mAlleleBindMatrices.get(matrix.Allele);

            if(pepLenMap == null)
            {
                pepLenMap = Maps.newHashMap();
                mAlleleBindMatrices.put(matrix.Allele, pepLenMap);
            }

            pepLenMap.put(matrix.PeptideLength, matrix);
        }

        if(!mRandomDistribution.loadData())
            return false;

        if(mConfig.BindLikelihoodFile != null && !mBindingLikelihood.loadLikelihoods(mConfig.BindLikelihoodFile))
            return false;

        if(mConfig.FlankPosWeightsFile != null && Files.exists(Paths.get(mConfig.FlankPosWeightsFile)))
        {
            if(!mFlankScores.loadPosWeights(mConfig.FlankPosWeightsFile))
                return false;
        }

        if(mConfig.RecognitionDataFile != null && !mRecognitionSimilarity.loadData(mConfig.RecognitionDataFile))
            return false;

        if(mConfig.ExpressionLikelihoodFile != null && !mExpressionLikelihood.loadTpmRates(mConfig.ExpressionLikelihoodFile))
            return false;

        return true;
    }

    public static void main(@NotNull final String[] args) throws ParseException
    {
        final Options options = new Options();

        ScoreConfig.addCmdLineArgs(options);

        final CommandLine cmd = createCommandLine(args, options);

        setLogLevel(cmd);

        BindScorer bindScorer = new BindScorer(new ScoreConfig(cmd));
        bindScorer.run();
    }

    @NotNull
    public static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException
    {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }
}
