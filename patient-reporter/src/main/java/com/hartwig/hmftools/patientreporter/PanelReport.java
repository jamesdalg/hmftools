package com.hartwig.hmftools.patientreporter;

import java.util.Optional;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public interface PanelReport {

    @NotNull
    SampleReport sampleReport();

    @NotNull
    default String user() {
        String systemUser = System.getProperty("user.name");
        String userName = Strings.EMPTY;
        String trainedEmployee = " (trained IT employee)";
        String combinedUserName = Strings.EMPTY;
        if (systemUser.equals("lieke") || systemUser.equals("liekeschoenmaker") || systemUser.equals("lschoenmaker")) {
            userName = "Lieke Schoenmaker";
            combinedUserName = userName + trainedEmployee;
        } else if (systemUser.equals("korneel") || systemUser.equals("korneelduyvesteyn") || systemUser.equals("kduyvesteyn")) {
            userName = "Korneel Duyvesteyn";
            combinedUserName = userName + trainedEmployee;
        } else if (systemUser.equals("sandra") || systemUser.equals("sandravandenbroek") || systemUser.equals("sandravdbroek")
                || systemUser.equals("s_vandenbroek") || systemUser.equals("svandenbroek")) {
            userName = "Sandra van den Broek";
            combinedUserName = userName + trainedEmployee;
        } else if (systemUser.equals("daphne") || systemUser.equals("d_vanbeek") || systemUser.equals("daphnevanbeek")
                || systemUser.equals("dvanbeek")) {
            userName = "Daphne van Beek";
            combinedUserName = userName + trainedEmployee;
        } else if (systemUser.equals("root")) {
            combinedUserName = "automatically";
        } else {
            userName = systemUser;
            combinedUserName = userName + trainedEmployee;
        }

        if (combinedUserName.endsWith(trainedEmployee)) {
            combinedUserName = "by " + combinedUserName;
        }

        return combinedUserName;

    }

    @NotNull
    String qsFormNumber();

    @NotNull
    Optional<String> comments();

    boolean isCorrectedReport();

    boolean isCorrectedReportExtern();

    @NotNull
    String signaturePath();

    @NotNull
    String logoCompanyPath();

    @NotNull
    String reportDate();

    boolean isWGSreport();
}
