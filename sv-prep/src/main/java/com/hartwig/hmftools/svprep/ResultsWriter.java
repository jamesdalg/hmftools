package com.hartwig.hmftools.svprep;

import static java.lang.Math.min;
import static java.lang.String.format;

import static com.hartwig.hmftools.common.utils.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.svprep.SvCommon.ITEM_DELIM;
import static com.hartwig.hmftools.svprep.SvCommon.SV_LOGGER;
import static com.hartwig.hmftools.svprep.WriteType.JUNCTIONS;
import static com.hartwig.hmftools.svprep.WriteType.READS;
import static com.hartwig.hmftools.svprep.WriteType.SV_BED;

import static htsjdk.samtools.SAMFlag.MATE_UNMAPPED;
import static htsjdk.samtools.SAMFlag.PROPER_PAIR;
import static htsjdk.samtools.SAMFlag.READ_UNMAPPED;
import static htsjdk.samtools.SAMFlag.SECONDARY_ALIGNMENT;
import static htsjdk.samtools.SAMFlag.SUPPLEMENTARY_ALIGNMENT;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import com.hartwig.hmftools.common.samtools.SupplementaryReadData;
import com.hartwig.hmftools.svprep.reads.JunctionData;
import com.hartwig.hmftools.svprep.reads.ReadFilterType;
import com.hartwig.hmftools.svprep.reads.ReadGroup;
import com.hartwig.hmftools.svprep.reads.ReadGroupStatus;
import com.hartwig.hmftools.svprep.reads.ReadRecord;
import com.hartwig.hmftools.svprep.reads.ReadType;
import com.hartwig.hmftools.svprep.reads.RemoteJunction;

import htsjdk.samtools.SAMRecord;

public class ResultsWriter
{
    private final SvConfig mConfig;

    private final BufferedWriter mReadWriter;
    private final BufferedWriter mBedWriter;
    private final BufferedWriter mJunctionWriter;
    private final BamWriter mBamWriter;

    public ResultsWriter(final SvConfig config)
    {
        mConfig = config;

        if(mConfig.OutputDir == null)
        {
            mBedWriter = null;
            mReadWriter = null;
            mJunctionWriter = null;
            mBamWriter = null;
            return;
        }

        mJunctionWriter = initialiseJunctionWriter();
        mBedWriter = initialiseBedWriter();
        mReadWriter = initialiseReadWriter();
        mBamWriter = new BamWriter(config);
    }

    public void close()
    {
        closeBufferedWriter(mReadWriter);
        closeBufferedWriter(mBedWriter);
        closeBufferedWriter(mJunctionWriter);
        mBamWriter.close();
    }

    private BufferedWriter initialiseReadWriter()
    {
        if(!mConfig.WriteTypes.contains(READS))
            return null;

        try
        {
            String filename = mConfig.formFilename(READS);
            BufferedWriter writer = createBufferedWriter(filename, false);

            writer.write("ReadId,GroupCount,ExpectedCount,GroupStatus,HasExternal,ReadType,Chromosome,PosStart,PosEnd,Cigar");
            writer.write(",FragLength,MateChr,MatePosStart,MapQual,SuppData,Flags");
            writer.write(",FirstInPair,ReadReversed,Proper,Unmapped,MateUnmapped,Supplementary,JunctionPositions");

            writer.newLine();

            return writer;
        }
        catch(IOException e)
        {
            SV_LOGGER.error(" failed to create read writer: {}", e.toString());
        }

        return null;
    }

    public synchronized void writeReadGroup(final List<ReadGroup> readGroups)
    {
        for(ReadGroup readGroup : readGroups)
        {
            String junctionPosStr = "";

            if(readGroup.junctionPositions() != null)
            {
                StringJoiner sjPos = new StringJoiner(ITEM_DELIM);
                readGroup.junctionPositions().forEach(x -> sjPos.add(String.valueOf(x)));
                junctionPosStr = sjPos.toString();
            }

            for(ReadRecord read : readGroup.reads())
            {
                if(read.written())
                    continue;

                writeReadData(
                        read, readGroup.size(), readGroup.expectedReadCount(), readGroup.groupStatus(), readGroup.spansPartitions(),
                        junctionPosStr);
            }
        }
    }

    private void writeReadData(
            final ReadRecord read, int readCount, int expectedReadCount, final ReadGroupStatus status, boolean spansPartitions,
            final String junctionPositions)
    {
        if(mReadWriter == null || read.written())
            return;

        try
        {
            mReadWriter.write(format("%s,%d,%d,%s,%s", read.id(), readCount, expectedReadCount, status, spansPartitions));

            mReadWriter.write(format(",%s,%s,%d,%d,%s",
                    read.readType(), read.Chromosome, read.start(), read.end(), read.cigar().toString()));

            SupplementaryReadData suppData = read.supplementaryAlignment();

            mReadWriter.write(format(",%d,%s,%d,%d,%s,%d",
                    read.fragmentInsertSize(), read.MateChromosome, read.MatePosStart, read.mapQuality(),
                    suppData != null ? suppData.asCsv() : "N/A", read.flags()));

            mReadWriter.write(format(",%s,%s,%s,%s,%s,%s",
                    read.isFirstOfPair(), read.isReadReversed(), read.hasFlag(PROPER_PAIR), read.hasFlag(READ_UNMAPPED),
                    read.isMateUnmapped(), read.hasFlag(SUPPLEMENTARY_ALIGNMENT)));

            mReadWriter.write(format(",%s", junctionPositions));

            mReadWriter.newLine();
        }
        catch(IOException e)
        {
            SV_LOGGER.error(" failed to write read data: {}", e.toString());
        }
    }

    private BufferedWriter initialiseJunctionWriter()
    {
        if(!mConfig.WriteTypes.contains(JUNCTIONS))
            return null;

        try
        {
            String filename = mConfig.formFilename(JUNCTIONS);
            BufferedWriter writer = createBufferedWriter(filename, false);

            writer.write("Chromosome,Position,Orientation,JunctionFrags,SupportFrags,DiscordantFrags,LowMapQualFrags,Hotspot,InitialReadId");
            writer.write(",RemoteJunctionCount,RemoteJunctions");
            writer.newLine();

            return writer;
        }
        catch(IOException e)
        {
            SV_LOGGER.error(" failed to create junction writer: {}", e.toString());
        }

        return null;
    }

    public synchronized void writeJunctionData(final String chromosome, final List<JunctionData> junctions)
    {
        if(mJunctionWriter == null)
            return;

        try
        {
            for(JunctionData junctionData : junctions)
            {
                int lowMapQualFrags = (int)junctionData.JunctionGroups.stream()
                        .filter(x -> x.reads().stream().anyMatch(y -> y.filters() == ReadFilterType.MIN_MAP_QUAL.flag())).count();

                int exactSupportFrags = (int)junctionData.SupportingGroups.stream()
                        .filter(x -> x.reads().stream().anyMatch(y -> y.readType() == ReadType.EXACT_SUPPORT)).count();

                int discordantFrags = junctionData.SupportingGroups.size() - exactSupportFrags;

                mJunctionWriter.write(format("%s,%d,%d,%d,%d,%d,%d,%s,%s",
                        chromosome, junctionData.Position, junctionData.Orientation, junctionData.junctionFragmentCount(),
                        exactSupportFrags, discordantFrags, lowMapQualFrags, junctionData.hotspot(), junctionData.InitialRead.id()));

                // RemoteChromosome:RemotePosition:RemoteOrientation;Fragments then separated by ';'
                String remoteJunctionsStr = "";

                if(!junctionData.RemoteJunctions.isEmpty())
                {
                    Collections.sort(junctionData.RemoteJunctions, new RemoteJunction.RemoteJunctionSorter());

                    StringJoiner sj = new StringJoiner(ITEM_DELIM);

                    for(int i = 0; i < min(junctionData.RemoteJunctions.size(), 10); ++i)
                    {
                        RemoteJunction remoteJunction = junctionData.RemoteJunctions.get(i);
                        sj.add(format("%s:%d:%d;%d",
                                remoteJunction.Chromosome, remoteJunction.Position, remoteJunction.Orientation, remoteJunction.Fragments));
                        // junctionData.RemoteJunctions.forEach(x -> sj.add(format("%s:%d:%d", x.Chromosome, x.Position, x.Orientation)));
                    }
                    remoteJunctionsStr = sj.toString();
                }

                mJunctionWriter.write(format(",%d,%s", junctionData.RemoteJunctions.size(), remoteJunctionsStr));
                mJunctionWriter.newLine();
            }
        }
        catch(IOException e)
        {
            SV_LOGGER.error(" failed to write junction data: {}", e.toString());
        }
    }

    public synchronized void writeBamRecords(final List<ReadGroup> readGroups)
    {
        if(mBamWriter == null)
            return;

        for(ReadGroup readGroup : readGroups)
        {
            readGroup.reads().stream().filter(x -> !x.written()).forEach(x -> mBamWriter.writeRecord(x.record()));
        }
    }

    private BufferedWriter initialiseBedWriter()
    {
        if(!mConfig.WriteTypes.contains(SV_BED))
            return null;

        return null;
    }
}
