package com.hartwig.hmftools.common.cobalt;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;

import org.junit.Test;

public class CobaltRatioFileTest {

    private static final String V37_PATH = Resources.getResource("cobalt/cobalt.37.tsv").getPath();
    private static final String V38_PATH = Resources.getResource("cobalt/cobalt.38.tsv").getPath();
    private static final String CHM13_PATH = Resources.getResource("cobalt/cobalt.CHM13.tsv").getPath();
    @Test
    public void testV38() throws IOException {
        final List<CobaltRatio> v38 = Lists.newArrayList(CobaltRatioFile.read(V38_PATH).get(HumanChromosome._1));
        assertEquals(5, v38.size());
    }
    @Test
    public void testCHM13() throws IOException {
        final List<CobaltRatio> vCHM13 = Lists.newArrayList(CobaltRatioFile.read(CHM13_PATH).get(HumanChromosome._1));
        assertEquals(5, vCHM13.size());
    }

    @Test
    public void testV37() throws IOException {
        final List<CobaltRatio> v37 = Lists.newArrayList(CobaltRatioFile.read(V37_PATH).get(HumanChromosome._1));
        assertEquals(4, v37.size());
    }
}
