package com.hartwig.hmftools.patientdb.readers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.ecrf.CpctEcrfModel;
import com.hartwig.hmftools.common.ecrf.datamodel.EcrfField;
import com.hartwig.hmftools.common.ecrf.datamodel.EcrfForm;
import com.hartwig.hmftools.common.ecrf.datamodel.EcrfItemGroup;
import com.hartwig.hmftools.common.ecrf.datamodel.EcrfPatient;
import com.hartwig.hmftools.common.ecrf.datamodel.EcrfStudyEvent;
import com.hartwig.hmftools.patientdb.data.PatientData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CpctPatientReader {
    private static final Logger LOGGER = LogManager.getLogger(CpctPatientReader.class);

    private static final String STUDY_BASELINE = "SE.BASELINE";
    private static final String STUDY_ENDSTUDY = "SE.ENDSTUDY";

    private static final String FORM_DEMOGRAPHY = "FRM.DEMOGRAPHY";
    private static final String FORM_CARCINOMA = "FRM.CARCINOMA";
    private static final String FORM_ELIGIBILITY = "FRM.ELIGIBILITY";
    private static final String FORM_SELCRIT = "FRM.SELCRIT";
    private static final String FORM_DEATH = "FRM.DEATH";

    private static final String ITEMGROUP_DEMOGRAPHY = "GRP.DEMOGRAPHY.DEMOGRAPHY";
    private static final String ITEMGROUP_CARCINOMA = "GRP.CARCINOMA.CARCINOMA";
    private static final String ITEMGROUP_ELIGIBILITY = "GRP.ELIGIBILITY.ELIGIBILITY";
    private static final String ITEMGROUP_SELCRIT = "GRP.SELCRIT.SELCRIT";
    private static final String ITEMGROUP_DEATH = "GRP.DEATH.DEATH";

    public static final String FIELD_SEX = "FLD.DEMOGRAPHY.SEX";
    public static final String FIELD_ETHNICITY = "FLD.DEMOGRAPHY.ETHNIC";

    public static final String FIELD_REGISTRATION_DATE1 = "FLD.ELIGIBILITY.REGDTC";
    public static final String FIELD_REGISTRATION_DATE2 = "FLD.SELCRIT.NREGDTC";
    public static final String FIELD_BIRTH_YEAR1 = "FLD.SELCRIT.NBIRTHYEAR";
    public static final String FIELD_BIRTH_YEAR2 = "FLD.ELIGIBILITY.BIRTHYEAR";
    public static final String FIELD_BIRTH_YEAR3 = "FLD.ELIGIBILITY.BIRTHDTCES";

    public static final String FIELD_PRIMARY_TUMOR_LOCATION = "FLD.CARCINOMA.PTUMLOC";
    public static final String FIELD_PRIMARY_TUMOR_LOCATION_OTHER = "FLD.CARCINOMA.PTUMLOCS";

    public static final String FIELD_DEATH_DATE = "FLD.DEATH.DDEATHDTC";

    private static final String DATAMODEL_HOSPITAL1 = "BASELINE.ELIGIBILITY.ELIGIBILITY.HOSPITAL";
    private static final String DATAMODEL_HOSPITAL2 = "BASELINE.SELCRIT.SELCRIT.NHOSPITAL";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @NotNull
    private final Map<Integer, String> hospitals;

    CpctPatientReader(@NotNull final CpctEcrfModel model) {
        this.hospitals = extractHospitalMap(model);
    }

    @NotNull
    PatientData read(@NotNull final EcrfPatient patient) {
        LOGGER.info("Reading patient " + patient.patientId());
        String gender = null;
        String ethnicity = null;
        String primaryTumorLocation = null;
        LocalDate registrationDate1 = null;
        LocalDate registrationDate2 = null;

        String birthYear1 = null;
        String birthYear2 = null;
        LocalDate birthYear3 = null;
        LocalDate deathDate = null;
        final String impliedHospital = getHospital(patient, hospitals);

        for (final EcrfStudyEvent studyEvent : patient.studyEventsPerOID(STUDY_BASELINE)) {
            for (final EcrfForm demographyForm : studyEvent.nonEmptyFormsPerOID(FORM_DEMOGRAPHY, true)) {
                for (final EcrfItemGroup demographyItemGroup : demographyForm.nonEmptyItemGroupsPerOID(ITEMGROUP_DEMOGRAPHY, true)) {
                    gender = demographyItemGroup.readItemString(FIELD_SEX, 0, true);
                    ethnicity = demographyItemGroup.readItemString(FIELD_ETHNICITY, 0, true);
                }
            }

            for (final EcrfForm carcinomaForm : studyEvent.nonEmptyFormsPerOID(FORM_CARCINOMA, true)) {
                for (final EcrfItemGroup carcinomaItemGroup : carcinomaForm.nonEmptyItemGroupsPerOID(ITEMGROUP_CARCINOMA, true)) {
                    primaryTumorLocation = carcinomaItemGroup.readItemString(FIELD_PRIMARY_TUMOR_LOCATION, 0, true);
                    if (primaryTumorLocation != null && primaryTumorLocation.trim().toLowerCase().startsWith("other")) {
                        primaryTumorLocation = carcinomaItemGroup.readItemString(FIELD_PRIMARY_TUMOR_LOCATION_OTHER, 0, true);
                    }
                }
            }

            for (final EcrfForm eligibilityForm : studyEvent.nonEmptyFormsPerOID(FORM_ELIGIBILITY, true)) {
                for (final EcrfItemGroup eligibilityItemGroup : eligibilityForm.nonEmptyItemGroupsPerOID(ITEMGROUP_ELIGIBILITY, true)) {
                    registrationDate1 = eligibilityItemGroup.readItemDate(FIELD_REGISTRATION_DATE1, 0, DATE_FORMATTER, false);
                    birthYear2 = eligibilityItemGroup.readItemString(FIELD_BIRTH_YEAR2, 0, false);
                    birthYear3 = eligibilityItemGroup.readItemDate(FIELD_BIRTH_YEAR3, 0, DATE_FORMATTER, false);
                }
            }

            for (final EcrfForm selcritForm : studyEvent.nonEmptyFormsPerOID(FORM_SELCRIT, true)) {
                for (final EcrfItemGroup selcritItemGroup : selcritForm.nonEmptyItemGroupsPerOID(ITEMGROUP_SELCRIT, true)) {
                    birthYear1 = selcritItemGroup.readItemString(FIELD_BIRTH_YEAR1, 0, false);
                    if (registrationDate1 == null) {
                        registrationDate2 = selcritItemGroup.readItemDate(FIELD_REGISTRATION_DATE2, 0, DATE_FORMATTER, false);
                    }
                }
            }
        }
        for (final EcrfStudyEvent endStudyEvent : patient.studyEventsPerOID(STUDY_ENDSTUDY)) {
            for (final EcrfForm deathFrom : endStudyEvent.nonEmptyFormsPerOID(FORM_DEATH, false)) {
                for (final EcrfItemGroup deathItemGroup : deathFrom.nonEmptyItemGroupsPerOID(ITEMGROUP_DEATH, false)) {
                    deathDate = deathItemGroup.readItemDate(FIELD_DEATH_DATE, 0, DATE_FORMATTER, true);
                }
            }
        }
        LocalDate registrationDate = registrationDate2 == null ? registrationDate1 : registrationDate2;
        final Integer birthYear = determineBirthYear(birthYear1, birthYear2, birthYear3);
        return new PatientData(patient.patientId(), registrationDate, gender, ethnicity, impliedHospital, birthYear, primaryTumorLocation,
                deathDate);
    }

    @NotNull
    private static Map<Integer, String> extractHospitalMap(@NotNull final CpctEcrfModel datamodel) {
        final Map<Integer, String> hospitals = Maps.newHashMap();
        final Iterable<EcrfField> fields = datamodel.findFieldsById(Lists.newArrayList(DATAMODEL_HOSPITAL1, DATAMODEL_HOSPITAL2));
        fields.forEach(field -> hospitals.putAll(field.codeList()));
        return ImmutableMap.copyOf(hospitals);
    }

    @Nullable
    private static String getHospital(@NotNull final EcrfPatient patient, @NotNull final Map<Integer, String> hospitals) {
        final Integer hospitalCode = Integer.parseInt(patient.patientId().substring(6, 8));
        final String hospital = hospitals.get(hospitalCode);
        if (hospital == null) {
            LOGGER.warn(DATAMODEL_HOSPITAL1 + ", " + DATAMODEL_HOSPITAL2 + " contained no Hospital with code " + hospitalCode);
        }
        return hospital;
    }

    @Nullable
    private static Integer determineBirthYear(@Nullable final String birthYear1, @Nullable final String birthYear2,
            @Nullable final LocalDate birthYear3) {
        if (birthYear1 != null) {
            return Integer.parseInt(birthYear1);
        }
        if (birthYear2 != null) {
            return Integer.parseInt(birthYear2);
        }
        if (birthYear3 != null) {
            return birthYear3.getYear();
        }
        return null;
    }
}
