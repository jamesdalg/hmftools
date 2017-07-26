package com.hartwig.hmftools.breakpointinspector;

import static com.hartwig.hmftools.breakpointinspector.Util.prefixList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

class BreakpointStats {
    int PR_Only_Normal = 0;
    int PR_SR_Normal = 0;
    int PR_Only_Support = 0;
    int PR_SR_Support = 0;
    int SR_Only_Support = 0;
    int Depth = 0;

    static List<String> GetHeader() {
        return Arrays.asList("PR_ONLY_NORMAL", "PR_SR_NORMAL", "PR_ONLY_SUPPORT", "PR_SR_SUPPORT", "SR_ONLY_SUPPORT", "DEPTH");
    }

    List<Integer> GetData() {
        return Arrays.asList(PR_Only_Normal, PR_SR_Normal, PR_Only_Support, PR_SR_Support, SR_Only_Support, Depth);
    }
}

class SampleStats {
    BreakpointStats BP1_Stats = new BreakpointStats();
    BreakpointStats BP2_Stats = new BreakpointStats();

    static List<String> GetHeader() {
        final List<String> header = Lists.newArrayList();
        header.addAll(prefixList(BreakpointStats.GetHeader(), "BP1_"));
        header.addAll(prefixList(BreakpointStats.GetHeader(), "BP2_"));
        return header;
    }

    private static List<String> toStrings(final List<Integer> list) {
        return list.stream().map(i -> Integer.toString(i)).collect(Collectors.toList());
    }

    List<String> GetData() {
        final List<String> data = Lists.newArrayList();
        data.addAll(toStrings(BP1_Stats.GetData()));
        data.addAll(toStrings(BP2_Stats.GetData()));
        return data;
    }
}
