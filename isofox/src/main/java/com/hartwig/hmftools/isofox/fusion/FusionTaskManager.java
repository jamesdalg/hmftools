package com.hartwig.hmftools.isofox.fusion;

import static com.hartwig.hmftools.isofox.IsofoxConfig.ISF_LOGGER;
import static com.hartwig.hmftools.isofox.fusion.FusionReadGroup.mergeChimericReadMaps;
import static com.hartwig.hmftools.isofox.fusion.HardFilteredCache.removePartialGroupsWithHardFilteredMatch;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache;
import com.hartwig.hmftools.isofox.IsofoxConfig;

public class FusionTaskManager
{
    private final IsofoxConfig mConfig;
    private final EnsemblDataCache mGeneTransCache;

    private final FusionWriter mFusionWriter;
    private final PassingFusions mPassingFusions;

    private final RacFragmentCache mRacFragmentCache;
    private final HardFilteredCache mHardFilteredCache;
    private int mHardFilteredFusionCount;
    private final Map<String,Map<String,FusionReadGroup>> mIncompleteReadGroups; // keyed by chromosome then readId

    public FusionTaskManager(final IsofoxConfig config, final EnsemblDataCache geneTransCache)
    {
        mConfig = config;
        mGeneTransCache = geneTransCache;

        mPassingFusions = new PassingFusions(config.Fusions.KnownFusions, config.Fusions.CohortFile);

        mRacFragmentCache = new RacFragmentCache();
        mIncompleteReadGroups = Maps.newHashMap();
        mHardFilteredCache = new HardFilteredCache();

        mGeneTransCache.createTranscriptIdMap();
        mHardFilteredCache.registerKnownSpliteSites(mGeneTransCache);
        mHardFilteredFusionCount = 0;

        mFusionWriter = new FusionWriter(mConfig);
    }

    public FusionFinder createFusionFinder(final String id)
    {
        return new FusionFinder(id, mConfig, mGeneTransCache, mRacFragmentCache, mPassingFusions, mFusionWriter);
    }

    public final RacFragmentCache racFragmentCache() { return mRacFragmentCache; }
    public final HardFilteredCache hardFilteredCache() { return mHardFilteredCache; }
    public final Map<String,Map<String,FusionReadGroup>> incompleteReadGroups() { return mIncompleteReadGroups; }

    public synchronized List<FusionReadGroup> addIncompleteReadGroup(
            final String chromosome, final Map<String,Map<String,FusionReadGroup>> chrIncompleteGroups,
            final Map<String,Set<String>> chrHardFilteredReadIds)
    {
        // receive new chromosome's incomplete groups for a particular chromosome, with these grouped by the chromosome they link to
        // additionally the new chromosome's hard-filtered groups, which are used to clear out the cache of previous partial groups
        // likewise use the existing hard-filtered cache to clean out any of the new partial groups
        int initTotalIncomplete = mIncompleteReadGroups.values().stream().mapToInt(x -> x.size()).sum();
        int initTotalHardFiltered = mHardFilteredCache.cacheCount();
        int initChrIncomplete = chrIncompleteGroups.values().stream().mapToInt(x -> x.size()).sum();
        int initChrHardFiltered = chrHardFilteredReadIds.values().stream().mapToInt(x -> x.size()).sum();

        List<FusionReadGroup> completeGroups = Lists.newArrayList();

        for(Map.Entry<String,Map<String, FusionReadGroup>> entry : chrIncompleteGroups.entrySet())
        {
            String otherChromosome = entry.getKey();
            Map<String, FusionReadGroup> newIncompleteGroups = entry.getValue();

            Map<String,FusionReadGroup> existingGroups = mIncompleteReadGroups.get(otherChromosome);

            if(existingGroups == null)
            {
                Map<String,FusionReadGroup> chromosomeGroups = mIncompleteReadGroups.get(chromosome);
                if(chromosomeGroups == null)
                {
                    chromosomeGroups = Maps.newHashMap();
                    mIncompleteReadGroups.put(chromosome, chromosomeGroups);
                }

                chromosomeGroups.putAll(newIncompleteGroups);

                ISF_LOGGER.debug("added chromosomes({} & {}) newGroups({}) new hardFiltered({})",
                        chromosome, otherChromosome, newIncompleteGroups.size(),
                        chrHardFilteredReadIds.values().stream().mapToInt(x -> x.size()).sum());
            }
            else
            {
                removePartialGroupsWithHardFilteredMatch(existingGroups, chrHardFilteredReadIds);

                mHardFilteredCache.removeHardFilteredReads(chromosome, chrIncompleteGroups, chrHardFilteredReadIds);

                mergeChimericReadMaps(existingGroups, completeGroups, newIncompleteGroups);

                // check and remove previously hard-filtered supplementary read groups
                // purge any hard-filtered groups involving these 2 chromosomes since they won't be handled again
                mHardFilteredCache.purgeChromosomeEntries(chromosome, otherChromosome);

                ISF_LOGGER.debug("combined chromosomes({} & {}) existing({}) new({}) complete({})",
                        chromosome, otherChromosome, existingGroups.size(), newIncompleteGroups.size(), completeGroups.size());
            }
        }

        mHardFilteredCache.addHardFilteredReads(chrHardFilteredReadIds);

        int newTotalIncomplete = mIncompleteReadGroups.values().stream().mapToInt(x -> x.size()).sum();
        int newTotalHardFiltered = mHardFilteredCache.cacheCount();
        int newChrHardFiltered = chrHardFilteredReadIds.values().stream().mapToInt(x -> x.size()).sum();;

        ISF_LOGGER.info("chr({}) complete({}) partials chr({}) total({} -> {}), filtered chr({} -> {}) total({} -> {})",
                chromosome, completeGroups.size(), initChrIncomplete, initTotalIncomplete, newTotalIncomplete,
                initChrHardFiltered, newChrHardFiltered, initTotalHardFiltered, newTotalHardFiltered);

        return completeGroups;
    }

    public synchronized void addRacFragments(final String chromosome, int geneCollectionId, final JunctionRacFragments racFragments)
    {
        mRacFragmentCache.addRacFragments(chromosome, geneCollectionId, racFragments);
    }

    public synchronized void addHardFilteredFusionCount(int count) { mHardFilteredFusionCount += count; }

    public void close()
    {
        int incompleteGroupCount = mIncompleteReadGroups.values().stream().mapToInt(x -> x.size()).sum();
        int totalHardFiltered = mHardFilteredFusionCount + mHardFilteredCache.hardFilteredCount();

        ISF_LOGGER.info("all fusion tasks complete: incompleteGroups({}) RAC frags({} assigned={} groups={}) hardFiltered({} supps={})",
                incompleteGroupCount, mRacFragmentCache.totalFragmentCount(), mRacFragmentCache.assignedFragmentCount(),
                mRacFragmentCache.totalGroupCount(), totalHardFiltered, mHardFilteredCache.hardFilteredCount());

        // write any unassigned RAC fragments
        if(mConfig.Fusions.WriteChimericReads || mConfig.Fusions.WriteChimericFragments)
            mFusionWriter.writeUnfusedFragments(mRacFragmentCache.getUnassignedFragments());

        if(mConfig.RunPerfChecks)
        {
            List<FusionReadGroup> incompleteGroups = Lists.newArrayList();

            for(Map.Entry<String,Map<String, FusionReadGroup>> chrEntry : mIncompleteReadGroups.entrySet())
            {
                String chromosome = chrEntry.getKey();

                if(mConfig.Filters.excludeChromosome(chromosome))
                    continue;

                Map<String, FusionReadGroup> rgMap = chrEntry.getValue();
                for(FusionReadGroup readGroup : rgMap.values())
                {
                    if(!mConfig.Filters.SpecificChromosomes.isEmpty())
                    {
                        if(readGroup.Reads.stream().anyMatch(x -> !mConfig.Filters.SpecificChromosomes.contains(x.MateChromosome)))
                            continue;
                    }

                    if(!skipMissingReads(readGroup.Reads))
                    {
                        incompleteGroups.add(readGroup);
                    }
                }
            }

            mFusionWriter.writeIncompleteGroupReads(incompleteGroups);
        }

        mFusionWriter.close();
    }

    private boolean skipMissingReads(final List<FusionRead> reads)
    {
        for(final FusionRead read : reads)
        {
            if(read.HasSuppAlignment)
            {
                if(mConfig.Filters.skipRead(read.SuppData.Chromosome, read.SuppData.Position))
                    return true;

                ISF_LOGGER.debug("read({}) missing supp({})", read, read.SuppData);
            }
        }

        return false;
    }
}
