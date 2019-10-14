package com.hartwig.hmftools.patientreporter.copynumber;

import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.drivercatalog.DriverType;
import com.hartwig.hmftools.common.drivercatalog.LikelihoodMethod;
import com.hartwig.hmftools.common.numeric.Doubles;

import org.jetbrains.annotations.NotNull;

public enum CopyNumberInterpretation {
    GAIN("gain"),
    FULL_LOSS("full loss"),
    PARTIAL_LOSS("partial loss"),
    HOM_DEL_DISRUPTION("homozygous deletion disruption");

    @NotNull
    private final String text;

    CopyNumberInterpretation(@NotNull final String text) {
        this.text = text;
    }

    @NotNull
    public String text() {
        return text;
    }

    @NotNull
    public static CopyNumberInterpretation fromCNADriver(@NotNull DriverCatalog copyNumber) {
        if (copyNumber.driver() == DriverType.AMP) {
            return GAIN;
        }

        if (copyNumber.driver() == DriverType.DEL) {
            return Doubles.greaterThan(copyNumber.maxCopyNumber(), 0.5) ? PARTIAL_LOSS : FULL_LOSS;
        }

        if (copyNumber.driver() == DriverType.HOM_DISRUPTION && copyNumber.likelihoodMethod() == LikelihoodMethod.DEL) {
            return HOM_DEL_DISRUPTION;
        }

        throw new IllegalStateException("Driver not an AMP or DEL: " + copyNumber);
    }
}