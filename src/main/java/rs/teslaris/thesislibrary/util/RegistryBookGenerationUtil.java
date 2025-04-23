package rs.teslaris.thesislibrary.util;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.util.language.SerbianTransliteration;
import rs.teslaris.thesislibrary.model.DissertationInformation;
import rs.teslaris.thesislibrary.model.PreviousTitleInformation;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;
import rs.teslaris.thesislibrary.model.RegistryBookPersonalInformation;

@Component
public class RegistryBookGenerationUtil {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMANY);

    private static MessageSource messageSource;


    public RegistryBookGenerationUtil(MessageSource messageSource) {
        RegistryBookGenerationUtil.messageSource = messageSource;
    }

    public static void constructRowsForChunk(TreeMap<String, List<List<String>>> rows,
                                             List<RegistryBookEntry> chunk, String lang) {
        for (RegistryBookEntry entry : chunk) {
            var rowData = new ArrayList<String>();

            addBookNumbers(rowData, entry);
            addAuthorInfo(rowData, entry.getPersonalInformation(), lang);
            addPreviousInstitutionInfo(rowData, entry.getPreviousTitleInformation());
            addPreviousTitleInfo(rowData, entry.getPreviousTitleInformation());
            addDissertationInstitution(rowData, entry.getDissertationInformation());
            addDissertationTitle(rowData, entry.getDissertationInformation(), lang);
            addCommissionAndMentor(rowData, entry.getDissertationInformation());
            addDefenceInfo(rowData, entry.getDissertationInformation(), lang);
            rowData.add(entry.getDissertationInformation().getAcquiredTitle());
            setDiplomaInformation(rowData, entry.getDissertationInformation(), lang);

            rowData.add(entry.getPromotion().getPromotionDate().format(DATE_FORMATTER));

            rows.computeIfAbsent(entry.getPromotionSchoolYear(), k -> new ArrayList<>())
                .add(rowData);
        }
    }

    private static void addBookNumbers(List<String> rowData, RegistryBookEntry entry) {
        rowData.add(
            entry.getRegistryBookNumber() + "\n---------\n" + entry.getPromotionOrdinalNumber());
    }

    private static void addAuthorInfo(ArrayList<String> rowData,
                                      RegistryBookPersonalInformation info,
                                      String lang) {
        rowData.add(info.getAuthorName().toString());
        setAuthorBirthInformation(rowData, info, lang);
        setAuthorParentInformation(rowData, info, lang);
    }

    private static void addPreviousInstitutionInfo(List<String> rowData,
                                                   PreviousTitleInformation info) {
        rowData.add(info.getInstitutionName() + ", " + info.getInstitutionPlace());
    }

    private static void addPreviousTitleInfo(List<String> rowData, PreviousTitleInformation info) {
        rowData.add(info.getAcademicTitle().getValue() + "\n" + info.getSchoolYear());
    }

    private static void addDissertationInstitution(List<String> rowData,
                                                   DissertationInformation info) {
        rowData.add(getTransliteratedContent(info.getOrganisationUnit().getName()) +
            ", " + info.getInstitutionPlace());
    }

    private static void addDissertationTitle(List<String> rowData, DissertationInformation info,
                                             String lang) {
        rowData.add(getTableLabel("reporting.registry-book.dissertation", lang) + "\n" +
            info.getDissertationTitle());
    }

    private static void addCommissionAndMentor(List<String> rowData, DissertationInformation info) {
        String commission = info.getCommission();
        String mentor = info.getMentor();
        rowData.add(commission + "\n" + (commission.contains(mentor) ? "" : mentor));
    }

    private static void addDefenceInfo(List<String> rowData, DissertationInformation info,
                                       String lang) {
        StringBuilder defenceInfo = new StringBuilder();

        if (!info.getGrade().isBlank()) {
            defenceInfo.append(getTableLabel("reporting.registry-book.grade", lang))
                .append("\n").append(info.getGrade()).append("\n");
        }

        defenceInfo.append(getTableLabel("reporting.registry-book.defended", lang))
            .append("\n").append(info.getDefenceDate().format(DATE_FORMATTER));

        rowData.add(defenceInfo.toString());
    }

    private static void setAuthorBirthInformation(ArrayList<String> rowData,
                                                  RegistryBookPersonalInformation personalInformation,
                                                  String lang) {
        var birthInformation = getTableLabel("reporting.registry-book.date", lang) + "\n" +
            personalInformation.getLocalBirthDate().format(DATE_FORMATTER) + "\n" +
            getTableLabel("reporting.registry-book.place", lang) + "\n" +
            personalInformation.getPlaceOfBrith() + "\n" +
            getTableLabel("reporting.registry-book.municipality", lang) +
            "\n" +
            personalInformation.getMunicipalityOfBrith() +
            "\n" +
            getTableLabel("reporting.registry-book.country", lang) +
            "\n" +
            getTransliteratedContent(
                personalInformation.getCountryOfBirth().getName()) +
            "\n";

        rowData.add(birthInformation);
    }

    private static void setAuthorParentInformation(ArrayList<String> rowData,
                                                   RegistryBookPersonalInformation personalInformation,
                                                   String lang) {
        var parentInformation = new StringBuilder();

        if (!personalInformation.getFatherName().isBlank()) {
            parentInformation.append(getTableLabel("reporting.registry-book.father", lang))
                .append("\n");
            parentInformation.append(personalInformation.getFatherName()).append(" ")
                .append(personalInformation.getFatherSurname()).append("\n");
        }

        if (!personalInformation.getMotherName().isBlank()) {
            parentInformation.append(getTableLabel("reporting.registry-book.mother", lang))
                .append("\n");
            parentInformation.append(personalInformation.getMotherName()).append(" ")
                .append(personalInformation.getMotherSurname()).append("\n");
        }

        if (!personalInformation.getGuardianNameAndSurname().isBlank()) {
            parentInformation.append(getTableLabel("reporting.registry-book.guardian", lang))
                .append("\n");
            parentInformation.append(personalInformation.getGuardianNameAndSurname()).append("\n");
        }

        rowData.add(parentInformation.toString());
    }

    private static void setDiplomaInformation(ArrayList<String> rowData,
                                              DissertationInformation dissertationInformation,
                                              String lang) {
        var diplomaInformation = new StringBuilder();

        if (!dissertationInformation.getDiplomaNumber().isBlank()) {
            diplomaInformation.append(getTableLabel("reporting.registry-book.diploma-number", lang))
                .append("\n");
            diplomaInformation.append(dissertationInformation.getDiplomaNumber()).append("\n")
                .append(getTableLabel("reporting.registry-book.date", lang)).append("\n")
                .append(dissertationInformation.getDiplomaIssueDate().format(DATE_FORMATTER))
                .append("\n");
        }

        if (!dissertationInformation.getDiplomaSupplementsNumber().isBlank()) {
            diplomaInformation.append(
                    getTableLabel("reporting.registry-book.supplements-number", lang))
                .append("\n");
            diplomaInformation.append(dissertationInformation.getDiplomaSupplementsNumber())
                .append("\n")
                .append(getTableLabel("reporting.registry-book.date", lang)).append("\n")
                .append(
                    dissertationInformation.getDiplomaSupplementsIssueDate().format(DATE_FORMATTER))
                .append("\n");
        }

        rowData.add(diplomaInformation.toString());
    }

    public static String getTableLabel(String fieldName, String lang) {
        return messageSource.getMessage(
            fieldName,
            new Object[] {},
            Locale.forLanguageTag(lang)
        );
    }

    public static String getTransliteratedContent(Set<MultiLingualContent> multilingualContent) {
        MultiLingualContent fallback = null;
        for (var content : multilingualContent) {
            if ("SR".equalsIgnoreCase(content.getLanguage().getLanguageTag())) {
                return SerbianTransliteration.toCyrillic(content.getContent());
            }
            fallback = content;
        }
        return fallback != null ? fallback.getContent() : "";
    }
}
