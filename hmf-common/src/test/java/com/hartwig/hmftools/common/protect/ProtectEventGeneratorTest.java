package com.hartwig.hmftools.common.protect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.hartwig.hmftools.common.linx.LinxTestFactory;
import com.hartwig.hmftools.common.protect.variant.OtherEffectsTestFactory;
import com.hartwig.hmftools.common.purple.interpretation.GainLoss;
import com.hartwig.hmftools.common.purple.interpretation.GainLossTestFactory;
import com.hartwig.hmftools.common.sv.linx.LinxFusion;
import com.hartwig.hmftools.common.test.SomaticVariantTestFactory;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.ImmutableSomaticVariantImpl;
import com.hartwig.hmftools.common.variant.ReportableVariant;
import com.hartwig.hmftools.common.variant.ReportableVariantTestFactory;
import com.hartwig.hmftools.common.variant.Variant;

import org.junit.Test;

public class ProtectEventGeneratorTest {

    @Test
    public void canTestToVariantEvent() {
        assertEquals("p.Gly12Cys", EventGenerator.toVariantEvent("p.Gly12Cys", "c.123A>C", "missense_variant", CodingEffect.MISSENSE));
        assertEquals("c.123A>C splice", EventGenerator.toVariantEvent("p.?", "c.123A>C", "missense_variant", CodingEffect.SPLICE));
        assertEquals("c.123A>C", EventGenerator.toVariantEvent("", "c.123A>C", "missense_variant", CodingEffect.MISSENSE));
        assertEquals("upstream", EventGenerator.toVariantEvent("", "", "upstream_gene_variant", CodingEffect.SPLICE));
        assertEquals("missense_variant", EventGenerator.toVariantEvent("", "", "missense_variant", CodingEffect.MISSENSE));
    }

    @Test
    public void canGenerateEventForReportableVariant() {
        ReportableVariant base = ReportableVariantTestFactory.builder().isCanonical(true).canonicalHgvsCodingImpact("coding").build();
        assertEquals("coding", EventGenerator.variantEvent(base));

        ReportableVariant nonCanonical = ReportableVariantTestFactory.builder()
                .from(base)
                .isCanonical(false)
                .otherReportedEffects(OtherEffectsTestFactory.create())
                .build();
        assertNotNull(EventGenerator.variantEvent(nonCanonical));
    }

    @Test
    public void canGenerateEventForVariant() {
        Variant base = SomaticVariantTestFactory.builder().canonicalEffect("some effect").build();
        assertEquals("some effect", EventGenerator.variantEvent(base));

        String upstreamEffect = EventGenerator.UPSTREAM_GENE_VARIANT;
        Variant upstream = ImmutableSomaticVariantImpl.builder().from(base).canonicalEffect(upstreamEffect).build();
        assertEquals("upstream", EventGenerator.variantEvent(upstream));

        Variant coding = ImmutableSomaticVariantImpl.builder().from(upstream).canonicalHgvsCodingImpact("coding impact").build();
        assertEquals("coding impact", EventGenerator.variantEvent(coding));

        Variant protein = ImmutableSomaticVariantImpl.builder().from(coding).canonicalHgvsProteinImpact("protein impact").build();
        assertEquals("protein impact", EventGenerator.variantEvent(protein));
    }

    @Test
    public void canGenerateEventForCopyNumber() {
        GainLoss gainLoss = GainLossTestFactory.createTestGainLoss();
        assertEquals(gainLoss.interpretation().display(), EventGenerator.copyNumberEvent(gainLoss));
    }

    @Test
    public void canGenerateEventForFusion() {
        LinxFusion fusion = LinxTestFactory.fusionBuilder().geneStart("start").geneEnd("end").build();
        assertEquals("start - end fusion", EventGenerator.fusionEvent(fusion));
    }
}