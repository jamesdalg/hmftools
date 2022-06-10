package com.hartwig.hmftools.sage.evidence;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.sage.candidate.Candidate;
import com.hartwig.hmftools.sage.SageConfig;
import com.hartwig.hmftools.sage.common.VariantTier;

import org.jetbrains.annotations.NotNull;

public class ReadContextCounterFactory
{
    private static final Set<VariantTier> HIGH_COVERAGE = EnumSet.of(VariantTier.HOTSPOT, VariantTier.PANEL);

    private final SageConfig mConfig;

    public ReadContextCounterFactory(final SageConfig config)
    {
        mConfig = config;
    }

    public List<ReadContextCounter> create(final List<Candidate> candidates)
    {
        List<ReadContextCounter> readCounters = Lists.newArrayListWithExpectedSize(candidates.size());

        int readId = 0;

        for(Candidate candidate : candidates)
        {
            readCounters.add(new ReadContextCounter(
                    readId++,
                    candidate.variant(),
                    candidate.readContext(),
                    candidate.tier(),
                    maxCoverage(candidate),
                    candidate.minNumberOfEvents()));
        }

        return readCounters;
    }

    private int maxCoverage(@NotNull final Candidate candidate)
    {
        return HIGH_COVERAGE.contains(candidate.tier()) ? mConfig.MaxReadDepthPanel : mConfig.MaxReadDepth;
    }
}
