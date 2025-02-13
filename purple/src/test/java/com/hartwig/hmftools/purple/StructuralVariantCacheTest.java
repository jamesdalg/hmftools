package com.hartwig.hmftools.purple;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import com.google.common.collect.Lists;

import org.junit.Test;

import htsjdk.variant.vcf.VCFHeader;

public class StructuralVariantCacheTest
{
    @Test
    public void testDummySupplierInstantiatesSuccessfully()
    {
        new StructuralVariantCache();
    }

    @Test
    public void testHeaderSamplesAreNotSorted()
    {
        final VCFHeader outOfOrderHeader = new VCFHeader(Collections.emptySet(), Lists.newArrayList("BBBBB", "AAAAA"));
        final VCFHeader victim = StructuralVariantCache.generateOutputHeader("2.23", outOfOrderHeader);
        assertEquals(2, victim.getGenotypeSamples().size());
        assertEquals("BBBBB", victim.getGenotypeSamples().get(0));
        assertEquals("AAAAA", victim.getGenotypeSamples().get(1));
    }
}
