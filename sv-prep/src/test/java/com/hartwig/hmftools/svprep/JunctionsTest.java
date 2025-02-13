package com.hartwig.hmftools.svprep;

import static com.hartwig.hmftools.common.test.MockRefGenome.generateRandomBases;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.NEG_ORIENT;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.POS_ORIENT;
import static com.hartwig.hmftools.svprep.SvPrepTestUtils.BLACKLIST_LOCATIONS;
import static com.hartwig.hmftools.svprep.SvPrepTestUtils.CHR_1;
import static com.hartwig.hmftools.svprep.SvPrepTestUtils.HOTSPOT_CACHE;
import static com.hartwig.hmftools.svprep.SvPrepTestUtils.buildFlags;
import static com.hartwig.hmftools.svprep.SvPrepTestUtils.createSamRecord;
import static com.hartwig.hmftools.svprep.SvPrepTestUtils.readIdStr;
import static com.hartwig.hmftools.svprep.reads.ReadFilters.isRepetitiveSectionBreak;
import static com.hartwig.hmftools.svprep.reads.ReadRecord.hasPolyATSoftClip;
import static com.hartwig.hmftools.svprep.reads.ReadType.CANDIDATE_SUPPORT;
import static com.hartwig.hmftools.svprep.reads.ReadType.JUNCTION;
import static com.hartwig.hmftools.svprep.reads.ReadType.NO_SUPPORT;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import com.hartwig.hmftools.common.utils.sv.BaseRegion;
import com.hartwig.hmftools.common.utils.sv.ChrBaseRegion;
import com.hartwig.hmftools.svprep.reads.JunctionData;
import com.hartwig.hmftools.svprep.reads.JunctionTracker;
import com.hartwig.hmftools.svprep.reads.ReadRecord;
import com.hartwig.hmftools.svprep.reads.ReadType;

import org.junit.Test;

public class JunctionsTest
{
    private static final String REF_BASES = generateRandomBases(500);

    private final ChrBaseRegion mPartitionRegion;
    private final JunctionTracker mJunctionTracker;

    public JunctionsTest()
    {
        mPartitionRegion = new ChrBaseRegion(CHR_1, 1, 5000);
        mJunctionTracker = new JunctionTracker(mPartitionRegion, new SvConfig(1000), HOTSPOT_CACHE, BLACKLIST_LOCATIONS);
    }

    private void addRead(final ReadRecord read, final ReadType readType)
    {
        read.setReadType(readType);
        mJunctionTracker.processRead(read);
    }

    @Test
    public void testBasicJunctions()
    {
        int readId = 0;

        ReadRecord read1 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 800, REF_BASES.substring(0, 100), "30S70M"));

        ReadRecord read2 = ReadRecord.from(createSamRecord(
                readIdStr(readId), CHR_1, 820, REF_BASES.substring(20, 120), "100M",
                buildFlags(false, true, false)));

        addRead(read1, JUNCTION);
        addRead(read2, NO_SUPPORT);

        ReadRecord suppRead1 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 800, REF_BASES.substring(0, 73), "3S70M"));

        addRead(suppRead1, CANDIDATE_SUPPORT);

        ReadRecord read3 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 950, REF_BASES.substring(0, 100), "30S70M"));

        ReadRecord read4 = ReadRecord.from(createSamRecord(
                readIdStr(readId), CHR_1, 980, REF_BASES.substring(20, 120), "100M",
                buildFlags(false, true, false)));

        addRead(read3, JUNCTION);
        addRead(read4, NO_SUPPORT);

        ReadRecord suppRead2 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 950, REF_BASES.substring(0, 73), "3S70M"));

        addRead(suppRead2, CANDIDATE_SUPPORT);

        ReadRecord read5 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 950, REF_BASES.substring(20, 120), "100M"));

        ReadRecord read6 = ReadRecord.from(createSamRecord(
                readIdStr(readId), CHR_1, 980, REF_BASES.substring(0, 100), "70M30S"));

        addRead(read5, NO_SUPPORT);
        addRead(read6, JUNCTION);

        ReadRecord suppRead3 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 990, REF_BASES.substring(0, 63), "60M3S"));

        addRead(suppRead3, CANDIDATE_SUPPORT);

        ReadRecord read7 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 1010, REF_BASES.substring(10, 90), "50M30S"));

        ReadRecord read8 = ReadRecord.from(createSamRecord(
                readIdStr(readId), CHR_1, 1010, REF_BASES.substring(0, 50), "50M"));

        addRead(read7, JUNCTION);
        addRead(read8, NO_SUPPORT);

        ReadRecord suppRead4 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 990, REF_BASES.substring(0, 73), "70M3S"));

        addRead(suppRead4, CANDIDATE_SUPPORT);

        mJunctionTracker.assignFragments();

        assertEquals(4, mJunctionTracker.junctions().size());

        JunctionData junctionData = mJunctionTracker.junctions().stream().filter(x -> x.Position == 800).findFirst().orElse(null);
        assertNotNull(junctionData);
        assertEquals(NEG_ORIENT, junctionData.Orientation);
        assertEquals(1, junctionData.junctionFragmentCount());
        assertEquals(1, junctionData.exactSupportFragmentCount());

        junctionData = mJunctionTracker.junctions().stream().filter(x -> x.Position == 950).findFirst().orElse(null);
        assertNotNull(junctionData);
        assertEquals(NEG_ORIENT, junctionData.Orientation);
        assertEquals(1, junctionData.junctionFragmentCount());
        assertEquals(1, junctionData.exactSupportFragmentCount());

        junctionData = mJunctionTracker.junctions().stream().filter(x -> x.Position == 1049).findFirst().orElse(null);
        assertNotNull(junctionData);
        assertEquals(POS_ORIENT, junctionData.Orientation);
        assertEquals(1, junctionData.junctionFragmentCount());
        assertEquals(1, junctionData.exactSupportFragmentCount());
        assertEquals(4, junctionData.supportingFragmentCount());

        junctionData = mJunctionTracker.junctions().stream().filter(x -> x.Position == 1059).findFirst().orElse(null);
        assertNotNull(junctionData);
        assertEquals(POS_ORIENT, junctionData.Orientation);
        assertEquals(1, junctionData.junctionFragmentCount());
        assertEquals(2, junctionData.exactSupportFragmentCount());
        assertEquals(5, junctionData.supportingFragmentCount());
    }

    @Test
    public void testInternalDeletes()
    {
        // initial delete is too short
        int readId = 0;

        ReadRecord read1 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 10, REF_BASES.substring(0, 80), "20M10D50M"));

        addRead(read1, JUNCTION);

        // then a simple one
        ReadRecord read2 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 100, REF_BASES.substring(0, 80), "20M40D20M"));

        addRead(read2, JUNCTION);

        // with supporting reads - first is too short as an indel
        ReadRecord suppRead = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 100, REF_BASES.substring(0, 80), "20M20D20M"));

        addRead(suppRead, CANDIDATE_SUPPORT);

        suppRead = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 120, REF_BASES.substring(0, 80), "20M20D20M"));

        addRead(suppRead, CANDIDATE_SUPPORT);

        // and a more complicated one
        // 5S10M2D10M3I10M35D10M2S from base 210: 10-19 match, 20-21 del, 22-31 match, ignore insert, 32-41 match, 42-76 del, 77-86 match

        ReadRecord read3 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 210, REF_BASES.substring(0, 1), "5S10M2D10M3I10M35D10M2S"));

        addRead(read3, JUNCTION);

        mJunctionTracker.assignFragments();

        assertEquals(4, mJunctionTracker.junctions().size());

        JunctionData junctionData = mJunctionTracker.junctions().stream().filter(x -> x.Position == 119).findFirst().orElse(null);
        assertNotNull(junctionData);
        assertEquals(POS_ORIENT, junctionData.Orientation);
        assertEquals(1, junctionData.junctionFragmentCount());
        assertEquals(1, junctionData.exactSupportFragmentCount());

        junctionData = mJunctionTracker.junctions().stream().filter(x -> x.Position == 160).findFirst().orElse(null);
        assertNotNull(junctionData);
        assertEquals(NEG_ORIENT, junctionData.Orientation);
        assertEquals(1, junctionData.junctionFragmentCount());
        assertEquals(1, junctionData.exactSupportFragmentCount());

        junctionData = mJunctionTracker.junctions().stream().filter(x -> x.Position == 241).findFirst().orElse(null);
        assertNotNull(junctionData);
        assertEquals(POS_ORIENT, junctionData.Orientation);
        assertEquals(1, junctionData.junctionFragmentCount());
        assertEquals(0, junctionData.exactSupportFragmentCount());

        junctionData = mJunctionTracker.junctions().stream().filter(x -> x.Position == 277).findFirst().orElse(null);
        assertNotNull(junctionData);
        assertEquals(NEG_ORIENT, junctionData.Orientation);
        assertEquals(1, junctionData.junctionFragmentCount());
        assertEquals(0, junctionData.exactSupportFragmentCount());
    }

    @Test
    public void testInternalInserts()
    {
        int readId = 0;

        // first is too short
        ReadRecord read1 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 10, REF_BASES.substring(0, 70), "20M10I50M"));

        addRead(read1, JUNCTION);

        // then a simple one
        ReadRecord read2 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 100, REF_BASES.substring(0, 70), "20M40I50M"));

        addRead(read2, JUNCTION);

        // and a more complicated one

        ReadRecord read3 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 210, REF_BASES.substring(0, 100), "5S10M2D10M3I10M35I10M2S"));

        addRead(read3, JUNCTION);

        mJunctionTracker.assignFragments();

        assertEquals(4, mJunctionTracker.junctions().size());
        assertEquals(119, mJunctionTracker.junctions().get(0).Position);
        assertEquals(120, mJunctionTracker.junctions().get(1).Position);

        assertEquals(241, mJunctionTracker.junctions().get(2).Position);
        assertEquals(242, mJunctionTracker.junctions().get(3).Position);
    }

    @Test
    public void testBlacklistRegions()
    {
        BLACKLIST_LOCATIONS.addRegion(CHR_1, new BaseRegion(500, 1500));

        JunctionTracker junctionTracker = new JunctionTracker(mPartitionRegion, new SvConfig(1000), HOTSPOT_CACHE, BLACKLIST_LOCATIONS);

        int readId = 0;

        ReadRecord read1 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 800, REF_BASES.substring(0, 100), "30S70M"));

        ReadRecord read2 = ReadRecord.from(createSamRecord(
                readIdStr(readId), CHR_1, 820, REF_BASES.substring(20, 120), "100M"));

        read1.setReadType(JUNCTION);
        read2.setReadType(JUNCTION);
        junctionTracker.processRead(read1);
        junctionTracker.processRead(read2);

        ReadRecord suppRead1 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 800, REF_BASES.substring(0, 73), "3S70M"));

        suppRead1.setReadType(CANDIDATE_SUPPORT);
        junctionTracker.processRead(suppRead1);

        junctionTracker.assignFragments();

        assertTrue(junctionTracker.junctions().isEmpty());
    }

    @Test
    public void testProximateJunctions()
    {
        int readId = 0;

        ReadRecord read1 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 800, REF_BASES.substring(0, 100), "30S70M"));

        read1.setReadType(JUNCTION);

        ReadRecord read2 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 820, REF_BASES.substring(20, 120), "100M"));

        /*
        read2.setReadType(JUNCTION);
        addRead();
        junctionTracker.processRead(read1);
        junctionTracker.processRead(read2);

        ReadRecord suppRead1 = ReadRecord.from(createSamRecord(
                readIdStr(++readId), CHR_1, 800, REF_BASES.substring(0, 73), "3S70M"));

        suppRead1.setReadType(CANDIDATE_SUPPORT);
        junctionTracker.processRead(suppRead1);

        junctionTracker.assignFragments();

        assertTrue(junctionTracker.junctions().isEmpty());
         */
    }


}
