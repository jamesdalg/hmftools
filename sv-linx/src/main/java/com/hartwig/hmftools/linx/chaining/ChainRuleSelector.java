package com.hartwig.hmftools.linx.chaining;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import static com.hartwig.hmftools.linx.analysis.SvUtilities.copyNumbersEqual;
import static com.hartwig.hmftools.linx.analysis.SvUtilities.formatPloidy;
import static com.hartwig.hmftools.linx.chaining.ChainDiagnostics.LOG_TYPE_VERBOSE;
import static com.hartwig.hmftools.linx.chaining.ChainPloidyLimits.ploidyOverlap;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.ADJACENT;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.CHAIN_SPLIT;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.FOLDBACK;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.NEAREST;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.PLOIDY_MATCH;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.PLOIDY_MAX;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.PLOIDY_OVERLAP;
import static com.hartwig.hmftools.linx.chaining.ChainingRule.SINGLE_OPTION;
import static com.hartwig.hmftools.linx.chaining.ProposedLinks.PL_TYPE_COMPLEX_DUP;
import static com.hartwig.hmftools.linx.chaining.ProposedLinks.PL_TYPE_FOLDBACK;
import static com.hartwig.hmftools.linx.chaining.ProposedLinks.PM_MATCHED;
import static com.hartwig.hmftools.linx.chaining.ProposedLinks.PM_NONE;
import static com.hartwig.hmftools.linx.chaining.ProposedLinks.PM_OVERLAP;
import static com.hartwig.hmftools.linx.types.SvVarData.SE_END;
import static com.hartwig.hmftools.linx.types.SvVarData.SE_START;
import static com.hartwig.hmftools.linx.types.SvVarData.isStart;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.linx.types.SvBreakend;
import com.hartwig.hmftools.linx.types.SvLinkedPair;
import com.hartwig.hmftools.linx.types.SvVarData;

public class ChainRuleSelector
{
    private ChainFinder mChainFinder;

    // references from chain-finder
    private Map<SvBreakend, List<SvLinkedPair>> mSvBreakendPossibleLinks;
    private final Map<SvVarData, SvChainState> mSvConnectionsMap;
    private final List<SvLinkedPair> mSkippedPairs;
    private final List<SvVarData> mComplexDupCandidates;
    private final List<SvVarData> mFoldbacks;
    private final List<SvLinkedPair> mAdjacentMatchingPairs;
    private final List<SvLinkedPair> mAdjacentPairs;
    private final List<SvChain> mChains;

    public ChainRuleSelector(
            final ChainFinder chainFinder,
            final Map<SvBreakend, List<SvLinkedPair>> svBreakendPossibleLinks,
            final Map<SvVarData, SvChainState> svConnectionsMap,
            final List<SvLinkedPair> skippedPairs,
            final List<SvVarData> foldbacks,
            final List<SvVarData> complexDupCandidates,
            final List<SvLinkedPair> adjacentMatchingPairs,
            final List<SvLinkedPair> adjacentPairs,
            final List<SvChain> chains)
    {
        mChainFinder = chainFinder;
        mSvBreakendPossibleLinks = svBreakendPossibleLinks;
        mSvConnectionsMap = svConnectionsMap;
        mSkippedPairs = skippedPairs;
        mFoldbacks = foldbacks;
        mComplexDupCandidates = complexDupCandidates;
        mAdjacentMatchingPairs = adjacentMatchingPairs;
        mAdjacentPairs = adjacentPairs;
        mChains = chains;
    }

    public List<ProposedLinks> findSingleOptionPairs()
    {
        // find all breakends with only one other link options and order by highest ploidy
        List<ProposedLinks> proposedLinks = Lists.newArrayList();

        for(Map.Entry<SvBreakend, List<SvLinkedPair>> entry : mSvBreakendPossibleLinks.entrySet())
        {
            if(entry.getValue().size() != 1)
                continue;

            SvBreakend limitingBreakend = entry.getKey();

            // special case for DM DUPs - because they can link with themselves at the end, don't restrict their connectivity earlier on
            if(mChainFinder.isDoubleMinuteDup(limitingBreakend.getSV()))
                continue;

            SvLinkedPair newPair = entry.getValue().get(0);

            if(mSkippedPairs.contains(newPair))
                continue;

            // avoid the second breakend also being a limiting factor and so adding this link twice
            if(proposedLinks.stream().map(x -> x.Links.get(0)).anyMatch(y -> y == newPair))
                continue;

            double ploidyFirst = mChainFinder.getUnlinkedBreakendCount(newPair.getFirstBreakend());
            double ploidySecond = mChainFinder.getUnlinkedBreakendCount(newPair.getSecondBreakend());

            if(ploidyFirst == 0 || ploidySecond == 0)
                continue;

            ProposedLinks proposedLink = new ProposedLinks(newPair, min(ploidyFirst, ploidySecond), SINGLE_OPTION);

            if(copyNumbersEqual(ploidyFirst, ploidySecond))
            {
                proposedLink.setPloidyMatch(PM_MATCHED);
            }
            else if(ploidyOverlap(ploidyFirst, newPair.getFirstBreakend().getSV().ploidyUncertainty(),
                    ploidySecond, newPair.getSecondBreakend().getSV().ploidyUncertainty()))
            {
                proposedLink.setPloidyMatch(PM_OVERLAP);
            }

            proposedLinks.add(proposedLink);
        }

        return proposedLinks;
    }

    public List<ProposedLinks> findFoldbackPairs(final List<ProposedLinks> proposedLinks)
    {
        // both ends of a foldback connect to one end of another SV with ploidy >= 2x

        if(mFoldbacks.isEmpty())
            return proposedLinks;

        List<ProposedLinks> newProposedLinks = Lists.newArrayList();
        List<SvVarData> processedChainedFoldbacks = Lists.newArrayList(); // to avoid double-processing

        for(SvVarData foldback : mFoldbacks)
        {
            boolean isChainedFoldback = foldback.isChainedFoldback();

            if(isChainedFoldback)
            {
                if(processedChainedFoldbacks.contains(foldback))
                    continue;

                processedChainedFoldbacks.add(foldback);
                processedChainedFoldbacks.add(foldback.getChainedFoldbackSv());
            }

            SvBreakend foldbackStart = null;
            SvBreakend foldbackEnd = null;

            if(!isChainedFoldback)
            {
                foldbackStart = foldback.getBreakend(true);
                foldbackEnd = foldback.getBreakend(false);
            }
            else
            {
                if(foldback.getFoldbackBreakend(true) != null)
                {
                    foldbackStart = foldback.getBreakend(true);
                    foldbackEnd = foldback.getFoldbackBreakend(true);
                }
                else
                {
                    foldbackStart = foldback.getBreakend(false);
                    foldbackEnd = foldback.getFoldbackBreakend(false);
                }
            }

            int origFoldbackPloidy = foldback.getImpliedPloidy();

            double foldbackPloidy = min(
                    mChainFinder.getUnlinkedBreakendCount(foldbackStart),
                    mChainFinder.getUnlinkedBreakendCount(foldbackEnd));

            if(foldbackPloidy == 0)
                continue;

            List<SvLinkedPair> pairsOnFbStart = mSvBreakendPossibleLinks.get(foldbackStart);
            List<SvLinkedPair> pairsOnFbEnd = mSvBreakendPossibleLinks.get(foldbackEnd);

            if(pairsOnFbStart == null || pairsOnFbEnd == null)
                continue;

            // get the set of possible pairs for the start and end breakends of the foldback and look for:
            // a) the same non-foldback breakend linked to different breakends of the same foldback
            // b) at least a 2:1 ploidy ratio between the non-foldback breakend and the foldback breakend

            for(SvLinkedPair pairStart : pairsOnFbStart)
            {
                SvVarData nonFbVar = pairStart.getOtherSV(foldback);
                SvBreakend otherBreakend = pairStart.getOtherBreakend(foldbackStart);

                if(nonFbVar.getImpliedPloidy() < origFoldbackPloidy)
                    continue;

                double nonFoldbackPloidy = mChainFinder.getUnlinkedBreakendCount(otherBreakend);

                // foldback ploidy must be half or less to match for a potential chain split
                if(!copyNumbersEqual(foldbackPloidy * 2, nonFoldbackPloidy) && nonFoldbackPloidy <= foldbackPloidy)
                    continue;

                // does this exist in the other foldback breakend's set of possible pairs
                for(SvLinkedPair pairEnd : pairsOnFbEnd)
                {
                    SvBreakend otherBreakend2 = pairEnd.getOtherBreakend(foldbackEnd);

                    // check that available breakends support this SV being connected twice
                    if(otherBreakend != otherBreakend2)
                        continue;

                    SvChain targetChain = null;
                    for(SvChain chain : mChains)
                    {
                        if(!copyNumbersEqual(foldbackPloidy * 2, chain.ploidy()) && chain.ploidy() <= foldbackPloidy)
                            continue;

                        for(int se = SE_START; se <= SE_END; ++se)
                        {
                            boolean isStart = isStart(se);
                            SvBreakend chainBe = chain.getOpenBreakend(isStart);

                            if (chainBe == null)
                                continue;

                            if(chainBe == otherBreakend || chainBe == otherBreakend)
                            {
                                targetChain = chain;
                                break;
                            }
                        }
                    }

                    ProposedLinks proposedLink = new ProposedLinks(
                            Lists.newArrayList(pairStart, pairEnd), foldbackPloidy, CHAIN_SPLIT, targetChain, PL_TYPE_FOLDBACK);

                    if(copyNumbersEqual(foldbackPloidy * 2, nonFoldbackPloidy))
                    {
                        proposedLink.setPloidyMatch(PM_MATCHED);
                    }
                    else if(ploidyOverlap(foldbackPloidy * 2, foldback.ploidyUncertainty(),
                            nonFoldbackPloidy, otherBreakend.getSV().ploidyUncertainty()))
                    {
                        proposedLink.setPloidyMatch(PM_OVERLAP);
                    }

                    newProposedLinks.add(proposedLink);

                    mChainFinder.log(LOG_TYPE_VERBOSE, String.format("foldback(%s) ploidy(%d) matched with breakend(%s) ploidy(%.1f -> %.1f)",
                            foldback.id(), foldbackPloidy, otherBreakend, nonFbVar.ploidyMin(), nonFbVar.ploidyMax()));

                    break;
                }
            }
        }

        // now check for a match between any previously identified proposed links and this set
        return restrictProposedLinks(proposedLinks, newProposedLinks, CHAIN_SPLIT);
    }

    public List<ProposedLinks> findComplexDupPairs(List<ProposedLinks> proposedLinks)
    {
        // both ends of a foldback or complex DUP connect to one end of another SV with ploidy >= 2x
        if(mComplexDupCandidates.isEmpty())
            return proposedLinks;

        List<ProposedLinks> newProposedLinks = Lists.newArrayList();

        // the complex DUP SVs need to connect to both ends of either a single SV or a set of chained SVs with twice the ploidy
        // for a complex DUP of the form D - A - D, where A has double ploidy of D, and both ends of D connect to both ends of A
        for(SvVarData compDup : mComplexDupCandidates)
        {
            double compDupPloidy = mChainFinder.getUnlinkedCount(compDup);

            SvBreakend compDupBeStart = compDup.getBreakend(true);
            SvBreakend compDupBeEnd = compDup.getBreakend(false);

            List<SvLinkedPair> pairsOnCdStart = mSvBreakendPossibleLinks.get(compDupBeStart);
            List<SvLinkedPair> pairsOnCdEnd = mSvBreakendPossibleLinks.get(compDupBeEnd);

            if (pairsOnCdStart == null || pairsOnCdEnd == null)
                continue;

            // search existing chains for open chain ends match the set of possibles for the complex DUP and with twice the ploidy
            for(SvChain chain : mChains)
            {
                if(!copyNumbersEqual(compDupPloidy * 2, chain.ploidy()) && chain.ploidy() <= compDupPloidy)
                    continue;

                SvBreakend chainBeStart = chain.getOpenBreakend(true);
                SvBreakend chainBeEnd = chain.getOpenBreakend(false);

                if(chainBeStart == null || chainBeEnd == null)
                    continue;

                SvLinkedPair[] matchingPair = {null, null};

                for(int se = SE_START; se <= SE_END; ++se)
                {
                    boolean isStart = isStart(se);
                    SvBreakend chainBe = chain.getOpenBreakend(isStart);

                    if(chainBe == null)
                        continue;

                    // shouldn't need to check breakend's unlinked ploidy again?
                    if(mChainFinder.getUnlinkedBreakendCount(chainBe) == 0)
                        continue;

                    // look for this link amongst the possible pairs
                    for(int se2 = SE_START; se2 <= SE_END; ++se2)
                    {
                        boolean isStart2 = isStart(se2);
                        List<SvLinkedPair> comDupPairs = isStart2 ? pairsOnCdStart : pairsOnCdEnd;
                        for (SvLinkedPair pair : comDupPairs)
                        {
                            if(pair.hasBreakend(chainBe))
                            {
                                matchingPair[se2] = pair;
                                break;
                            }
                        }
                    }
                }

                if(matchingPair[SE_START] == null || matchingPair[SE_END] == null)
                    continue;

                ProposedLinks proposedLink = new ProposedLinks(Lists.newArrayList(matchingPair[SE_START], matchingPair[SE_END]),
                        compDupPloidy, CHAIN_SPLIT, chain, PL_TYPE_COMPLEX_DUP);

                if(copyNumbersEqual(compDupPloidy * 2, chain.ploidy()))
                {
                    proposedLink.setPloidyMatch(PM_MATCHED);
                }
                else if(ploidyOverlap(compDupPloidy * 2, compDup.ploidyUncertainty(),
                        chain.ploidy(), chain.ploidyUncertainty()))
                {
                    proposedLink.setPloidyMatch(PM_OVERLAP);
                }

                newProposedLinks.add(proposedLink);

                mChainFinder.log(LOG_TYPE_VERBOSE, String.format("comDup(%s) ploidy(%d) matched with chain breakends(%s & %s) ploidy(%.1f -> %.1f)",
                        compDup.id(), compDupPloidy, chainBeStart.toString(), chainBeEnd.toString(),
                        chainBeStart.getSV().ploidyMin(), chainBeStart.getSV().ploidyMax()));

                /*
                mDiagnostics.logCsv("COMP_DUP", compDup,
                        String.format("ploidy(%.1f-%.1f-%.1f) beStart(%s ploidy=%.1f-%.1f) beEnd(%s ploidy=%.1f-%.1f)",
                                compDup.ploidyMin(), compDup.ploidy(), compDup.ploidyMax(),
                                chainBeStart.toString(), chainBeStart.getSV().ploidyMin(), chainBeStart.getSV().ploidyMax(),
                                chainBeEnd.toString(), chainBeEnd.getSV().ploidyMin(), chainBeEnd.getSV().ploidyMax()));
                */
            }

            // alternatively check if the same SV can be linked to the complex DUP's breakends and satisifies the ploidy constraints
            for(SvLinkedPair pairStart : pairsOnCdStart)
            {
                SvVarData otherVar = pairStart.getOtherSV(compDup);
                SvBreakend otherBreakend = pairStart.getOtherBreakend(compDupBeStart);

                double otherBreakendPloidy = mChainFinder.getUnlinkedBreakendCount(otherBreakend);

                if(!copyNumbersEqual(compDupPloidy * 2, otherBreakendPloidy) && otherBreakendPloidy <= compDupPloidy)
                    continue;

                // does this exist in the other foldback breakend's set of possible pairs
                for(SvLinkedPair pairEnd : pairsOnCdEnd)
                {
                    SvBreakend otherBreakend2 = pairEnd.getOtherBreakend(compDupBeEnd);

                    // check that available breakends support this SV being connected twice
                    if(otherBreakend.getSV() != otherBreakend2.getSV())
                        continue;

                    double otherBreakendPloidy2 = mChainFinder.getUnlinkedBreakendCount(otherBreakend2);

                    if(copyNumbersEqual(compDupPloidy * 2, otherBreakendPloidy2) || otherBreakendPloidy2 > compDupPloidy)
                    {
                        ProposedLinks proposedLink = new ProposedLinks(Lists.newArrayList(pairStart, pairEnd),
                                compDupPloidy, CHAIN_SPLIT, null, PL_TYPE_COMPLEX_DUP);

                        if(copyNumbersEqual(compDupPloidy * 2, otherBreakendPloidy))
                        {
                            proposedLink.setPloidyMatch(PM_MATCHED);
                        }
                        else if(ploidyOverlap(compDupPloidy * 2, compDup.ploidyUncertainty(),
                                otherBreakendPloidy, otherVar.ploidyUncertainty()))
                        {
                            proposedLink.setPloidyMatch(PM_OVERLAP);
                        }

                        newProposedLinks.add(proposedLink);

                        mChainFinder.log(LOG_TYPE_VERBOSE, String.format("comDup(%s) ploidy(%d) matched with breakends(%s & %s) ploidy(%.1f -> %.1f)",
                                compDup.id(), compDupPloidy, otherBreakend, otherBreakend2, otherVar.ploidyMin(), otherVar.ploidyMax()));
                    }

                    break;
                }
            }
        }

        return restrictProposedLinks(proposedLinks, newProposedLinks, CHAIN_SPLIT);
    }

    public List<ProposedLinks> findFoldbackToFoldbackPairs(final List<ProposedLinks> proposedLinks)
    {
        // 2 foldbacks face each other

        if(mFoldbacks.isEmpty())
            return proposedLinks;

        List<ProposedLinks> newProposedLinks = Lists.newArrayList();

        // first gather up the foldback breakends as pairs
        List<SvBreakend> foldbackBreakends = Lists.newArrayList();

        for(SvVarData foldback : mFoldbacks)
        {
            if(foldback.getFoldbackBreakend(true) != null)
                foldbackBreakends.add(foldback.getBreakend(true));

            if(foldback.getFoldbackBreakend(false) != null)
                foldbackBreakends.add(foldback.getBreakend(false));
        }

        for(SvBreakend breakend : foldbackBreakends)
        {
            double foldbackPloidy = mChainFinder.getUnlinkedBreakendCount(breakend);

            if(foldbackPloidy == 0)
                continue;

            final List<SvLinkedPair> svLinks = mSvBreakendPossibleLinks.get(breakend);

            if(svLinks == null)
                continue;

            for(final SvLinkedPair pair : svLinks)
            {
                SvBreakend otherBreakend = pair.getOtherBreakend(breakend);

                if(!foldbackBreakends.contains(otherBreakend))
                    continue;

                double otherBreakendPloidy = mChainFinder.getUnlinkedBreakendCount(otherBreakend);

                if(mSkippedPairs.contains(pair))
                    continue;

                if(proposedLinks.stream().map(x -> x.Links.get(0)).anyMatch(y -> y == pair))
                    continue;

                mChainFinder.log(LOG_TYPE_VERBOSE, String.format("pair(%s) of foldbacks with ploidy(%s & %s)",
                        pair.toString(), formatPloidy(foldbackPloidy), formatPloidy(otherBreakendPloidy)));

                double avgPloidy = (foldbackPloidy + otherBreakendPloidy) * 0.5;

                ProposedLinks proposedLink = new ProposedLinks(pair, avgPloidy, PLOIDY_MATCH);

                if(copyNumbersEqual(foldbackPloidy, otherBreakendPloidy))
                {
                    proposedLink.setPloidyMatch(PM_MATCHED);
                }
                else if(ploidyOverlap(foldbackPloidy, breakend.getSV().ploidyUncertainty(),
                        otherBreakendPloidy, otherBreakend.getSV().ploidyUncertainty()))
                {
                    proposedLink.setPloidyMatch(PM_OVERLAP);
                }

                newProposedLinks.add(proposedLink);
            }
        }

        // now check for a match between any previously identified proposed links and this set
        return restrictProposedLinks(proposedLinks, newProposedLinks, FOLDBACK);
    }

    public List<ProposedLinks> findPloidyMatchPairs(List<ProposedLinks> proposedLinks)
    {
        List<ProposedLinks> newProposedLinks = Lists.newArrayList();

        if(!proposedLinks.isEmpty())
        {
            proposedLinks.stream().filter(x -> x.ploidyMatchType() == PM_MATCHED).forEach(x -> x.addRule(PLOIDY_MATCH));
            proposedLinks.stream().filter(x -> x.ploidyMatchType() == PM_OVERLAP).forEach(x -> x.addRule(PLOIDY_OVERLAP));
            return proposedLinks;
        }

        double currentMaxPloidy = 0;
        List<SvLinkedPair> addedLinks = Lists.newArrayList();

        for(SvChainState svConn : mSvConnectionsMap.values())
        {
            SvVarData var = svConn.SV;

            // check whether this SV has any possible links with SVs of the same (remaining) rep count
            for(int be = SE_START; be <= SE_END; ++be)
            {
                if(var.isNullBreakend() && be == SE_END)
                    continue;

                boolean isStart = isStart(be);
                double breakendPloidy = svConn.unlinked(be);

                if(breakendPloidy == 0)
                    continue;

                if(!copyNumbersEqual(breakendPloidy, currentMaxPloidy) && breakendPloidy < currentMaxPloidy)
                    continue;

                final SvBreakend breakend = var.getBreakend(isStart);
                final List<SvLinkedPair> svLinks = mSvBreakendPossibleLinks.get(breakend);

                if(svLinks == null)
                    continue;

                for(final SvLinkedPair pair : svLinks)
                {
                    if(addedLinks.contains(pair))
                        continue;

                    if(mSkippedPairs.contains(pair))
                        continue;

                    SvBreakend otherBreakend = pair.getOtherBreakend(breakend);

                    double otherBreakendPloidy = mChainFinder.getUnlinkedBreakendCount(otherBreakend);

                    String ploidyMatch = PM_NONE;
                    if(copyNumbersEqual(otherBreakendPloidy, breakendPloidy))
                    {
                        ploidyMatch = PM_MATCHED;
                    }
                    else if(ploidyOverlap(var.ploidyUncertainty(), breakendPloidy, otherBreakendPloidy, otherBreakend.getSV().ploidyUncertainty()))
                    {
                        ploidyMatch = PM_OVERLAP;
                    }
                    else
                    {
                        continue;
                    }

                    mChainFinder.log(LOG_TYPE_VERBOSE, String.format("pair(%s) with {} ploidy(%s & %s)",
                            pair.toString(), ploidyMatch, formatPloidy(breakendPloidy), formatPloidy(otherBreakendPloidy)));

                    double avgPloidy = (breakendPloidy + otherBreakendPloidy) * 0.5;

                    if(!copyNumbersEqual(avgPloidy, currentMaxPloidy))
                        currentMaxPloidy = avgPloidy;

                    if(ploidyMatch == PM_MATCHED)
                        newProposedLinks.add(new ProposedLinks(pair, avgPloidy, PLOIDY_MATCH));
                    else
                        newProposedLinks.add(new ProposedLinks(pair, avgPloidy, PLOIDY_OVERLAP));
                }
            }
        }

        return newProposedLinks;
    }

    public List<ProposedLinks> findAdjacentMatchingPairs(List<ProposedLinks> proposedLinks)
    {
        if(mAdjacentMatchingPairs.isEmpty())
            return proposedLinks;

        if(!proposedLinks.isEmpty())
        {
            for(ProposedLinks proposedLink : proposedLinks)
            {
                // check for any links which are in the adjacent set
                if(proposedLink.Links.stream().allMatch(x -> mAdjacentMatchingPairs.contains(x)))
                {
                    proposedLink.addRule(ADJACENT);
                }
            }

            return proposedLinks;
        }

        List<ProposedLinks> newProposedLinks = Lists.newArrayList();

        while(!mAdjacentMatchingPairs.isEmpty())
        {
            SvLinkedPair nextPair = mAdjacentMatchingPairs.get(0);

            mAdjacentMatchingPairs.remove(0);

            if(mChainFinder.matchesExistingPair(nextPair))
                continue;

            double firstPloidy = mChainFinder.getUnlinkedBreakendCount(nextPair.getFirstBreakend());
            double secondPloidy = mChainFinder.getUnlinkedBreakendCount(nextPair.getSecondBreakend());

            if(firstPloidy == 0 || secondPloidy == 0)
                continue;

            if(!copyNumbersEqual(firstPloidy, secondPloidy))
                continue;

            // take the average ploidy or calculate a weighted ploidy already?
            // if these links have already been partially used, then incorrect to calculate a weighted ploidy
            double avgPloidy = (firstPloidy + secondPloidy) * 0.5;

            ProposedLinks proposedLink = new ProposedLinks(nextPair, avgPloidy, ADJACENT);
            proposedLink.addRule(PLOIDY_MATCH);
            newProposedLinks.add(proposedLink);
        }

        return restrictProposedLinks(proposedLinks, newProposedLinks, ADJACENT);
    }

    public List<ProposedLinks> findAdjacentPairs(List<ProposedLinks> proposedLinks)
    {
        if(mAdjacentPairs.isEmpty())
            return proposedLinks;

        if(!proposedLinks.isEmpty())
        {
            for(ProposedLinks proposedLink : proposedLinks)
            {
                // check for any links which are in the adjacent set
                if(proposedLink.Links.stream().allMatch(x -> mAdjacentPairs.contains(x)))
                {
                    proposedLink.addRule(ADJACENT);
                }
            }

            return proposedLinks;
        }

        List<ProposedLinks> newProposedLinks = Lists.newArrayList();

        while(!mAdjacentPairs.isEmpty())
        {
            SvLinkedPair nextPair = mAdjacentPairs.get(0);

            mAdjacentPairs.remove(0);

            if(mChainFinder.matchesExistingPair(nextPair))
                continue;

            double firstPloidy = mChainFinder.getUnlinkedBreakendCount(nextPair.getFirstBreakend());
            double secondPloidy = mChainFinder.getUnlinkedBreakendCount(nextPair.getSecondBreakend());

            if(firstPloidy == 0 || secondPloidy == 0)
                continue;

            if(!copyNumbersEqual(firstPloidy, secondPloidy))
                continue;

            // take the average ploidy or calculate a weighted ploidy already?
            // if these links have already been partially used, then incorrect to calculate a weighted ploidy
            double avgPloidy = (firstPloidy + secondPloidy) * 0.5;

            ProposedLinks proposedLink = new ProposedLinks(nextPair, avgPloidy, ADJACENT);
            proposedLink.addRule(PLOIDY_MATCH);
            newProposedLinks.add(proposedLink);
        }

        return restrictProposedLinks(proposedLinks, newProposedLinks, ADJACENT);
    }

    public List<ProposedLinks> findHighestPloidy(List<ProposedLinks> proposedLinks)
    {
        List<ProposedLinks> newProposedLinks = Lists.newArrayList();

        if(!proposedLinks.isEmpty())
        {
            // take the highest from amongt the proposed links
            double maxPloidy = proposedLinks.stream().mapToDouble(x -> x.Ploidy).max().getAsDouble();

            proposedLinks.stream().filter(x -> copyNumbersEqual(maxPloidy, x.Ploidy)).forEach(x -> x.addRule(PLOIDY_MAX));
            return proposedLinks;
        }

        double currentMaxPloidy = 0;
        List<SvLinkedPair> addedLinks = Lists.newArrayList();

        for(SvChainState svConn : mSvConnectionsMap.values())
        {
            SvVarData var = svConn.SV;

            // check whether this SV has any possible links with SVs of the same (remaining) rep count
            for(int be = SE_START; be <= SE_END; ++be)
            {
                if(var.isNullBreakend() && be == SE_END)
                    continue;

                boolean isStart = isStart(be);
                double breakendPloidy = svConn.unlinked(be);

                if(breakendPloidy == 0)
                    continue;

                if(!copyNumbersEqual(breakendPloidy, currentMaxPloidy) && breakendPloidy < currentMaxPloidy)
                    continue;

                final SvBreakend breakend = var.getBreakend(isStart);
                final List<SvLinkedPair> svLinks = mSvBreakendPossibleLinks.get(breakend);

                if(svLinks == null)
                    continue;

                for(final SvLinkedPair pair : svLinks)
                {
                    if(addedLinks.contains(pair))
                        continue;

                    if(mSkippedPairs.contains(pair))
                        continue;

                    SvBreakend otherBreakend = pair.getOtherBreakend(breakend);

                    double otherBreakendPloidy = mChainFinder.getUnlinkedBreakendCount(otherBreakend);

                    if(otherBreakendPloidy == 0)
                        continue;

                    double minPairPloidy = min(otherBreakendPloidy, breakendPloidy);

                    if(!copyNumbersEqual(currentMaxPloidy, minPairPloidy) && minPairPloidy < currentMaxPloidy)
                        continue;

                    currentMaxPloidy = max(minPairPloidy, currentMaxPloidy);

                    mChainFinder.log(LOG_TYPE_VERBOSE, String.format("pair(%s) with max ploidy(%s & %s)",
                            pair.toString(), formatPloidy(breakendPloidy), formatPloidy(otherBreakendPloidy)));

                    double avgPloidy = (breakendPloidy + otherBreakendPloidy) * 0.5;

                    newProposedLinks.add(new ProposedLinks(pair, avgPloidy, PLOIDY_MAX));
                }
            }
        }

        return newProposedLinks;
    }

    public List<ProposedLinks> findNearest(List<ProposedLinks> proposedLinks)
    {
        if(!proposedLinks.isEmpty())
        {
            long shortestDistance = 0;
            ProposedLinks shortestLink = null;

            // take the highest from amongt the proposed links
            for(ProposedLinks proposedLink : proposedLinks)
            {
                if(shortestLink == null || proposedLink.shortestLinkDistance() < shortestDistance)
                {
                    shortestDistance = proposedLink.shortestLinkDistance();
                    shortestLink = proposedLink;
                }
            }

            return Lists.newArrayList(shortestLink);
        }

        long shortestDistance = 0;
        SvLinkedPair shortestLink = null;
        double linkPloidy = 0;

        for(SvChainState svConn : mSvConnectionsMap.values())
        {
            SvVarData var = svConn.SV;

            // check whether this SV has any possible links with SVs of the same (remaining) rep count
            for(int be = SE_START; be <= SE_END; ++be)
            {
                if(var.isNullBreakend() && be == SE_END)
                    continue;

                boolean isStart = isStart(be);
                double breakendPloidy = svConn.unlinked(be);

                if(breakendPloidy == 0)
                    continue;

                final SvBreakend breakend = var.getBreakend(isStart);
                final List<SvLinkedPair> svLinks = mSvBreakendPossibleLinks.get(breakend);

                if(svLinks == null)
                    continue;

                for(final SvLinkedPair pair : svLinks)
                {
                    if(mSkippedPairs.contains(pair))
                        continue;

                    SvBreakend otherBreakend = pair.getOtherBreakend(breakend);

                    double otherBreakendPloidy = mChainFinder.getUnlinkedBreakendCount(otherBreakend);

                    if(otherBreakendPloidy == 0)
                        continue;

                    if(shortestLink == null || pair.length() < shortestDistance)
                    {
                        shortestDistance = pair.length();
                        shortestLink = pair;
                        linkPloidy = min(breakendPloidy, otherBreakendPloidy);
                    }

                    mChainFinder.log(LOG_TYPE_VERBOSE, String.format("pair(%s) shortest ploidy(%s & %s)",
                            pair.toString(), formatPloidy(breakendPloidy), formatPloidy(otherBreakendPloidy)));
                }
            }
        }

        if(shortestLink == null)
            return Lists.newArrayList();

        return Lists.newArrayList(new ProposedLinks(shortestLink, linkPloidy, NEAREST));
    }

    private static List<ProposedLinks> restrictProposedLinks(
            List<ProposedLinks> proposedLinks, List<ProposedLinks> newProposedLinks, ChainingRule newRule)
    {
        // now check for a match between any previously identified proposed links and this set
        if(proposedLinks.isEmpty())
            return newProposedLinks;

        if(newProposedLinks.isEmpty())
            return proposedLinks;

        for(ProposedLinks proposedLink : proposedLinks)
        {
            for (ProposedLinks newProposedLink : newProposedLinks)
            {
                if(anyLinkMatch(proposedLink.Links, newProposedLink.Links))
                {
                    proposedLink.addRule(newRule);
                    break;
                }
            }
        }

        return proposedLinks;
    }

    private static boolean anyLinkMatch(final List<SvLinkedPair> links1, final List<SvLinkedPair> links2)
    {
        for(final SvLinkedPair pair : links1)
        {
            if(links2.contains(pair))
                return true;
        }

        return false;
    }

    public static void cullByPriority(List<ProposedLinks> proposedLinks)
    {
        if(proposedLinks.size() <= 1)
            return;

        // find the highest priority and remove any entries less than this
        int maxPriority = proposedLinks.stream().mapToInt(x -> x.priority()).max().getAsInt();

        int index = 0;
        while(index < proposedLinks.size())
        {
            if(proposedLinks.get(index).priority() < maxPriority)
                proposedLinks.remove(index);
            else
                ++index;
        }
    }

}
