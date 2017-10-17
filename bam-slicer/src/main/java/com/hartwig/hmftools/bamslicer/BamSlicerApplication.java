package com.hartwig.hmftools.bamslicer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.exception.EmptyFileException;
import com.hartwig.hmftools.common.region.GenomeRegion;
import com.hartwig.hmftools.common.slicing.Slicer;
import com.hartwig.hmftools.common.slicing.SlicerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.variant.variantcontext.StructuralVariantType;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

public class BamSlicerApplication {
    private static final Logger LOGGER = LogManager.getLogger(BamSlicerApplication.class);

    private static final String INPUT_MODE_S3 = "s3";
    private static final String INPUT_MODE_FILE = "file";
    private static final String INPUT = "input";
    private static final String BUCKET = "bucket";
    private static final String INDEX = "index";
    private static final String OUTPUT = "output";
    private static final String PROXIMITY = "proximity";
    private static final String VCF = "vcf";
    private static final String BED = "bed";

    public static void main(final String... args) throws ParseException, IOException, EmptyFileException {
        final CommandLine cmd = createCommandLine(args);
        assert cmd != null;

        if (cmd.hasOption(INPUT_MODE_FILE)) {
            sliceFromVCF(cmd);
        }
        if (cmd.hasOption(INPUT_MODE_S3)) {
            sliceFromS3(cmd);
        }
        LOGGER.info("Done.");
    }

    private static void sliceFromVCF(@NotNull final CommandLine cmd) throws IOException {
        final String inputPath = cmd.getOptionValue(INPUT);
        final String outputPath = cmd.getOptionValue(OUTPUT);
        final String vcfPath = cmd.getOptionValue(VCF);
        final int proximity = Integer.parseInt(cmd.getOptionValue(PROXIMITY, "500"));
        final SamReader reader = SamReaderFactory.makeDefault().open(new File(inputPath));
        final QueryInterval[] intervals = getIntervalsFromVCF(vcfPath, reader.getFileHeader(), proximity);
        writeToSlice(outputPath, reader, intervals);
        reader.close();
    }

    private static QueryInterval[] getIntervalsFromVCF(@NotNull final String vcfPath, @NotNull final SAMFileHeader header,
            final int proximity) {
        final File vcfFile = new File(vcfPath);
        final VCFFileReader vcfReader = new VCFFileReader(vcfFile, false);
        final List<QueryInterval> queryIntervals = Lists.newArrayList();
        for (VariantContext variant : vcfReader) {

            queryIntervals.add(new QueryInterval(header.getSequenceIndex(variant.getContig()), Math.max(0, variant.getStart() - proximity),
                    variant.getStart() + proximity));

            if (variant.getStructuralVariantType() == StructuralVariantType.BND) {

                final String call = variant.getAlternateAllele(0).getDisplayString();
                final String[] leftSplit = call.split("\\]");
                final String[] rightSplit = call.split("\\[");

                final String contig;
                final int position;
                if (leftSplit.length >= 2) {
                    final String[] location = leftSplit[1].split(":");
                    contig = location[0];
                    position = Integer.parseInt(location[1]);
                } else if (rightSplit.length >= 2) {
                    final String[] location = rightSplit[1].split(":");
                    contig = location[0];
                    position = Integer.parseInt(location[1]);
                } else {
                    LOGGER.error(variant.getID() + " : could not parse breakpoint");
                    continue;
                }
                queryIntervals.add(
                        new QueryInterval(header.getSequenceIndex(contig), Math.max(0, position - proximity), position + proximity));
            } else {
                queryIntervals.add(
                        new QueryInterval(header.getSequenceIndex(variant.getContig()), Math.max(0, variant.getEnd() - proximity),
                                variant.getEnd() + proximity));
            }
        }

        return QueryInterval.optimizeIntervals(queryIntervals.toArray(new QueryInterval[queryIntervals.size()]));
    }

    private static void sliceFromS3(@NotNull final CommandLine cmd) throws IOException, EmptyFileException {
        final URL bamUrl = SbpS3UrlGenerator.generateUrl(cmd.getOptionValue(BUCKET), cmd.getOptionValue(INPUT));
        final URL indexUrl = SbpS3UrlGenerator.generateUrl(cmd.getOptionValue(BUCKET), cmd.getOptionValue(INDEX));
        final String outputPath = cmd.getOptionValue(OUTPUT);
        final String bedPath = cmd.getOptionValue(BED);
        final SamReader reader = SamReaderFactory.makeDefault().open(SamInputResource.of(bamUrl).index(indexUrl));
        LOGGER.info("Generating query intervals from BED file: {}", bedPath);
        final QueryInterval[] intervals = getIntervalsFromBED(bedPath, reader.getFileHeader());
        LOGGER.info("Generated {} query intervals.", intervals.length);
        LOGGER.info("Slicing bam...");
        writeToSlice(outputPath, reader, intervals);
        reader.close();
    }

    private static QueryInterval[] getIntervalsFromBED(@NotNull final String bedPath, @NotNull final SAMFileHeader header)
            throws IOException, EmptyFileException {
        final Slicer bedSlicer = SlicerFactory.fromBedFile(bedPath);
        final List<QueryInterval> queryIntervals = Lists.newArrayList();
        for (final GenomeRegion region : bedSlicer.regions()) {
            queryIntervals.add(new QueryInterval(header.getSequenceIndex(region.chromosome()), (int) region.start(), (int) region.end()));
        }
        return QueryInterval.optimizeIntervals(queryIntervals.toArray(new QueryInterval[queryIntervals.size()]));
    }

    private static void writeToSlice(final String path, final SamReader reader, final QueryInterval[] intervals) {
        final File outputBAM = new File(path);
        final SAMFileWriter writer = new SAMFileWriterFactory().setCreateIndex(true).makeBAMWriter(reader.getFileHeader(), true, outputBAM);
        final SAMRecordIterator iterator = reader.queryOverlapping(intervals);
        String contig = "";
        while (iterator.hasNext()) {
            final SAMRecord record = iterator.next();
            if (record.getContig() != null && !contig.equals(record.getContig())) {
                contig = record.getContig();
                LOGGER.info("Reading contig: {}", contig);
            }
            writer.addAlignment(record);
        }
        iterator.close();
        writer.close();
    }

    private static Options createOptions() {
        final Options options = new Options();
        final OptionGroup inputModeOptionGroup = new OptionGroup();
        inputModeOptionGroup.addOption(Option.builder(INPUT_MODE_S3).required().desc("read input BAM from s3").build());
        inputModeOptionGroup.addOption(Option.builder(INPUT_MODE_FILE).required().desc("read input BAM from file").build());
        options.addOptionGroup(inputModeOptionGroup);
        return options;
    }

    private static Options createS3Options() {
        final Options options = new Options();
        options.addOption(Option.builder(INPUT_MODE_S3).required().desc("read input BAM from s3").build());
        options.addOption(Option.builder(BUCKET).required().hasArg().desc("s3 bucket for BAM and index files (required)").build());
        options.addOption(Option.builder(INPUT).required().hasArg().desc("BAM file location (required)").build());
        options.addOption(Option.builder(INDEX).required().hasArg().desc("BAM index location (required)").build());
        options.addOption(Option.builder(OUTPUT).required().hasArg().desc("the output BAM (required)").build());
        options.addOption(Option.builder(BED).required().hasArg().desc("BED to slice BAM with (required)").build());
        return options;
    }

    private static Options createVcfOptions() {
        final Options options = new Options();
        options.addOption(Option.builder(INPUT_MODE_FILE).required().desc("read input BAM from the filesystem").build());
        options.addOption(Option.builder(INPUT).required().hasArg().desc("the input BAM to slice (required)").build());
        options.addOption(Option.builder(OUTPUT).required().hasArg().desc("the output BAM (required)").build());
        options.addOption(Option.builder(PROXIMITY).hasArg().desc("distance to slice around breakpoint (optional, default=500)").build());
        options.addOption(Option.builder(VCF).required().hasArg().desc("VCF to slice BAM with (required)").build());
        return options;
    }

    private static CommandLine createCommandLine(@NotNull final String... args) throws ParseException {
        final Options options = createOptions();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args, true);
        if (cmd.hasOption(INPUT_MODE_S3)) {
            final Options s3Options = createS3Options();
            try {
                return parser.parse(s3Options, args);

            } catch (ParseException e) {
                LOGGER.error(e.getMessage());
                printHelpAndExit("Slice an s3 BAM file based on BED", s3Options);
            }
        }
        if (cmd.hasOption(INPUT_MODE_FILE)) {
            final Options vcfOptions = createVcfOptions();
            try {
                return parser.parse(vcfOptions, args);
            } catch (final ParseException e) {
                LOGGER.error(e.getMessage());
                printHelpAndExit("Slice a local BAM file based on VCF", vcfOptions);
            }
        } else {
            printHelpAndExit("Slice a BAM", options);
        }
        return null;
    }

    private static void printHelpAndExit(@NotNull final String header, @NotNull final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Bam-Slicer", header, options, "", true);
        System.exit(1);
    }
}
