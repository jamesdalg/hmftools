package com.hartwig.hmftools.compar.purple;

import static com.hartwig.hmftools.compar.Category.PURITY;
import static com.hartwig.hmftools.compar.DiffFunctions.checkDiff;
import static com.hartwig.hmftools.compar.MatchLevel.REPORTABLE;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.genome.chromosome.GermlineAberration;
import com.hartwig.hmftools.common.purple.PurpleQCStatus;
import com.hartwig.hmftools.common.purple.purity.PurityContext;
import com.hartwig.hmftools.compar.Category;
import com.hartwig.hmftools.compar.ComparableItem;
import com.hartwig.hmftools.compar.DiffThresholds;
import com.hartwig.hmftools.compar.MatchLevel;
import com.hartwig.hmftools.compar.Mismatch;

public class PurityData implements ComparableItem
{
    public final PurityContext Purity;

    protected static final String FLD_PURITY = "purity";
    protected static final String FLD_PLOIDY = "ploidy";
    protected static final String FLD_CONTAMINATION = "contamination";
    protected static final String FLD_TMB = "tmbPerMb";
    protected static final String FLD_MS_INDELS = "msIndelsPerMb";
    protected static final String FLD_TML = "tml";
    protected static final String FLD_CN_SEGS = "copyNumberSegments";
    protected static final String FLD_UNS_CN_SEGS = "unsupportedCopyNumberSegments";
    protected static final String FLD_SV_TMB = "svTmb";

    public PurityData(final PurityContext purityContext)
    {
        Purity = purityContext;
    }

    public Category category() {
        return PURITY;
    }

    @Override
    public String key()
    {
        return "";
    }

    @Override
    public List<String> displayValues()
    {
        List<String> values = Lists.newArrayList();
        values.add(String.format("Pass(%s)", Purity.qc().pass()));
        values.add(String.format("Purity(%.2f)", Purity.bestFit().purity()));
        values.add(String.format("Ploidy(%.2f)", Purity.bestFit().ploidy()));
        return values;
    }

    @Override
    public boolean reportable() {
        return true;
    }

    @Override
    public boolean matches(final ComparableItem other)
    {
        // a single record for each sample
        return true;
    }

    @Override
    public Mismatch findMismatch(final ComparableItem other, final MatchLevel matchLevel, final DiffThresholds thresholds)
    {
        final PurityData otherPurity = (PurityData) other;

        final List<String> diffs = Lists.newArrayList();

        checkDiff(diffs, FLD_PURITY, Purity.bestFit().purity(), otherPurity.Purity.bestFit().purity(), thresholds);
        checkDiff(diffs, FLD_PLOIDY, Purity.bestFit().ploidy(), otherPurity.Purity.bestFit().ploidy(), thresholds);

        checkDiff(diffs, FLD_CONTAMINATION, Purity.qc().contamination(), otherPurity.Purity.qc().contamination(), thresholds);

        checkDiff(diffs, FLD_TMB, Purity.tumorMutationalBurdenPerMb(), otherPurity.Purity.tumorMutationalBurdenPerMb(), thresholds);
        checkDiff(diffs, FLD_MS_INDELS, Purity.microsatelliteIndelsPerMb(), otherPurity.Purity.microsatelliteIndelsPerMb(), thresholds);
        checkDiff(diffs, FLD_TML, Purity.tumorMutationalLoad(), otherPurity.Purity.tumorMutationalLoad(), thresholds);

        checkDiff(diffs, FLD_CN_SEGS, Purity.qc().copyNumberSegments(), otherPurity.Purity.qc().copyNumberSegments(), thresholds);

        checkDiff(
                diffs, FLD_UNS_CN_SEGS,
                Purity.qc().unsupportedCopyNumberSegments(), otherPurity.Purity.qc().unsupportedCopyNumberSegments(),thresholds);

        checkDiff(diffs, FLD_SV_TMB, Purity.bestFit().purity(), otherPurity.Purity.bestFit().purity(), thresholds);

        checkDiff(diffs, "qcStatus", qcStatus(Purity.qc().status()), qcStatus(otherPurity.Purity.qc().status()));

        checkDiff(diffs, "gender", Purity.gender().toString(), otherPurity.Purity.gender().toString());

        checkDiff(
                diffs, "germlineAberrations",
                germlineAberrations(Purity.qc().germlineAberrations()), germlineAberrations(otherPurity.Purity.qc().germlineAberrations()));

        checkDiff(diffs, "fitMethod", Purity.method().toString(), otherPurity.Purity.method().toString());

        checkDiff(diffs, "msStatus", Purity.microsatelliteStatus().toString(), otherPurity.Purity.microsatelliteStatus().toString());

        checkDiff(
                diffs, "tmbStatus",
                Purity.tumorMutationalBurdenStatus().toString(), otherPurity.Purity.tumorMutationalBurdenStatus().toString());

        checkDiff(
                diffs, "tmlStatus",
                Purity.tumorMutationalLoadStatus().toString(), otherPurity.Purity.tumorMutationalLoadStatus().toString());

        return null;
    }

    private static String germlineAberrations(final Set<GermlineAberration> aberrations)
    {
        StringJoiner sj = new StringJoiner(";");
        aberrations.forEach(x -> sj.add(x.toString()));
        return sj.toString();
    }

    public static String qcStatus(final Set<PurpleQCStatus> status)
    {
        StringJoiner sj = new StringJoiner(";");
        status.forEach(x -> sj.add(x.toString()));
        return sj.toString();
    }
}
