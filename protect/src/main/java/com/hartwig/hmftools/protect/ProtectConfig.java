package com.hartwig.hmftools.protect;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface ProtectConfig {

    // General params needed for every report
    String TUMOR_SAMPLE_ID = "tumor_sample_id";
    String OUTPUT_DIRECTORY = "output_dir";

    String SERVE_ACTIONABILITY_DIRECTORY = "serve_actionability_dir";

    String TUMOR_LOCATION_TSV = "tumor_location_tsv";
    String GERMLINE_GENES_CSV = "germline_genes_csv";

    // Files containing the actual genomic results for this sample.
    String PURPLE_PURITY_TSV = "purple_purity_tsv";
    String PURPLE_QC_FILE = "purple_qc_file";
    String PURPLE_GENE_CNV_TSV = "purple_gene_cnv_tsv";
    String PURPLE_DRIVER_CATALOG_TSV = "purple_driver_catalog_tsv";
    String PURPLE_SOMATIC_VARIANT_VCF = "purple_somatic_variant_vcf";
    String BACHELOR_TSV = "bachelor_tsv";
    String LINX_FUSION_TSV = "linx_fusion_tsv";
    String LINX_DISRUPTION_TSV = "linx_disruption_tsv";
    String LINX_VIRAL_INSERTION_TSV = "linx_viral_insertion_tsv";
    String LINX_DRIVERS_TSV = "linx_drivers_tsv";
    String CHORD_PREDICTION_TXT = "chord_prediction_txt";

    // Some additional optional params and flags
    String LOG_DEBUG = "log_debug";

    @NotNull
    static Options createOptions() {
        Options options = new Options();

        options.addOption(TUMOR_SAMPLE_ID, true, "The sample ID for which PROTECT will run.");
        options.addOption(OUTPUT_DIRECTORY, true, "Path to where the PROTECT output data will be written to.");

        options.addOption(SERVE_ACTIONABILITY_DIRECTORY, true, "Path towards the SERVE actionability directory.");

        options.addOption(TUMOR_LOCATION_TSV, true, "Path towards the (curated) tumor location TSV.");
        options.addOption(GERMLINE_GENES_CSV, true, "Path towards a CSV containing germline genes which we want to report.");

        options.addOption(PURPLE_PURITY_TSV, true, "Path towards the purple purity TSV.");
        options.addOption(PURPLE_QC_FILE, true, "Path towards the purple qc file.");
        options.addOption(PURPLE_GENE_CNV_TSV, true, "Path towards the purple gene copy number TSV.");
        options.addOption(PURPLE_DRIVER_CATALOG_TSV, true, "Path towards the purple driver catalog TSV.");
        options.addOption(PURPLE_SOMATIC_VARIANT_VCF, true, "Path towards the purple somatic variant VCF.");
        options.addOption(BACHELOR_TSV, true, "Path towards the germline TSV.");
        options.addOption(LINX_FUSION_TSV, true, "Path towards the linx fusion TSV.");
        options.addOption(LINX_DISRUPTION_TSV, true, "Path towards the linx disruption TSV.");
        options.addOption(LINX_VIRAL_INSERTION_TSV, true, "Path towards the LINX viral insertion TSV.");
        options.addOption(LINX_DRIVERS_TSV, true, "Path towards the LINX driver catalog TSV.");
        options.addOption(CHORD_PREDICTION_TXT, true, "Path towards the CHORD prediction TXT.");

        options.addOption(LOG_DEBUG, false, "If provided, set the log level to debug rather than default.");

        return options;
    }

    @NotNull
    String tumorSampleId();

    @NotNull
    String outputDir();

    @NotNull
    String serveActionabilityDir();

    @NotNull
    String tumorLocationTsv();

    @NotNull
    String germlineGenesCsv();

    @NotNull
    String purplePurityTsv();

    @NotNull
    String purpleQcFile();

    @NotNull
    String purpleGeneCnvTsv();

    @NotNull
    String purpleDriverCatalogTsv();

    @NotNull
    String purpleSomaticVariantVcf();

    @NotNull
    String bachelorTsv();

    @NotNull
    String linxFusionTsv();

    @NotNull
    String linxDisruptionTsv();

    @NotNull
    String linxViralInsertionTsv();

    @NotNull
    String linxDriversTsv();

    @NotNull
    String chordPredictionTxt();

    @NotNull
    static ProtectConfig createConfig(@NotNull CommandLine cmd) throws ParseException {
        if (cmd.hasOption(LOG_DEBUG)) {
            Configurator.setRootLevel(Level.DEBUG);
        }

        return ImmutableProtectConfig.builder()
                .tumorSampleId(nonOptionalValue(cmd, TUMOR_SAMPLE_ID))
                .outputDir(nonOptionalDir(cmd, OUTPUT_DIRECTORY))
                .serveActionabilityDir(nonOptionalDir(cmd, SERVE_ACTIONABILITY_DIRECTORY))
                .tumorLocationTsv(nonOptionalFile(cmd, TUMOR_LOCATION_TSV))
                .germlineGenesCsv(nonOptionalFile(cmd, GERMLINE_GENES_CSV))
                .purplePurityTsv(nonOptionalFile(cmd, PURPLE_PURITY_TSV))
                .purpleQcFile(nonOptionalFile(cmd, PURPLE_QC_FILE))
                .purpleGeneCnvTsv(nonOptionalFile(cmd, PURPLE_GENE_CNV_TSV))
                .purpleDriverCatalogTsv(nonOptionalFile(cmd, PURPLE_DRIVER_CATALOG_TSV))
                .purpleSomaticVariantVcf(nonOptionalFile(cmd, PURPLE_SOMATIC_VARIANT_VCF))
                .bachelorTsv(nonOptionalFile(cmd, BACHELOR_TSV))
                .linxFusionTsv(nonOptionalFile(cmd, LINX_FUSION_TSV))
                .linxDisruptionTsv(nonOptionalFile(cmd, LINX_DISRUPTION_TSV))
                .linxViralInsertionTsv(nonOptionalFile(cmd, LINX_VIRAL_INSERTION_TSV))
                .linxDriversTsv(nonOptionalFile(cmd, LINX_DRIVERS_TSV))
                .chordPredictionTxt(nonOptionalFile(cmd, CHORD_PREDICTION_TXT))
                .build();
    }

    @NotNull
    static String nonOptionalValue(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = cmd.getOptionValue(param);
        if (value == null) {
            throw new ParseException("Parameter must be provided: " + param);
        }

        return value;
    }

    @NotNull
    static String nonOptionalDir(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = nonOptionalValue(cmd, param);

        if (!pathExists(value) || !pathIsDirectory(value)) {
            throw new ParseException("Parameter '" + param + "' must be an existing directory: " + value);
        }

        return value;
    }

    @NotNull
    static String nonOptionalFile(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = nonOptionalValue(cmd, param);

        if (!pathExists(value)) {
            throw new ParseException("Parameter '" + param + "' must be an existing file: " + value);
        }

        return value;
    }

    static boolean pathExists(@NotNull String path) {
        return Files.exists(new File(path).toPath());
    }

    static boolean pathIsDirectory(@NotNull String path) {
        return Files.isDirectory(new File(path).toPath());
    }
}
