package rs.teslaris.assessment.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.language.SerbianTransliteration;

@Component
public class AssessmentReportGenerator {

    private static MessageSource messageSource;

    private static OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private static UserRepository userRepository;

    private static OrganisationUnitService organisationUnitService;

    private static CommissionService commissionService;


    @Autowired
    public AssessmentReportGenerator(MessageSource messageSource,
                                     OrganisationUnitIndexRepository organisationUnitIndexRepository,
                                     UserRepository userRepository,
                                     CommissionService commissionService,
                                     OrganisationUnitService organisationUnitService) {
        AssessmentReportGenerator.messageSource = messageSource;
        AssessmentReportGenerator.organisationUnitIndexRepository = organisationUnitIndexRepository;
        AssessmentReportGenerator.userRepository = userRepository;
        AssessmentReportGenerator.commissionService = commissionService;
        AssessmentReportGenerator.organisationUnitService = organisationUnitService;
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable63(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        var year = String.valueOf(assessmentResponses.getFirst().getToYear());
        Map<String, String> replacements = getTable63Headers(locale, year);

        Map<String, Integer> publicationsPerGroup = new TreeMap<>();
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                var groupCode = ClassificationPriorityMapping.getGroupCode(code);
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c)) {
                        publicationsPerGroup.put(groupCode,
                            publicationsPerGroup.getOrDefault(groupCode, 0) + 1);
                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        List<List<String>> tableData = new ArrayList<>();
        publicationsPerGroup.forEach((groupCode, publicationCount) -> {
            tableData.add(List.of(String.valueOf(tableData.size() + 1),
                ClassificationPriorityMapping.getGroupNameBasedOnCode(groupCode, locale), groupCode,
                String.valueOf(publicationCount)));
        });

        return new Pair<>(replacements, tableData);
    }

    private static Map<String, String> getTable63Headers(String locale, String year) {
        return Map.of(
            "{header}",
            LocalizationUtil.getMessage("reporting.table63.header", new Object[] {year}, locale),
            "{col1}",
            LocalizationUtil.getMessage("reporting.table.rowNumber", new Object[] {}, locale),
            "{col2}",
            LocalizationUtil.getMessage("reporting.table63.groupName", new Object[] {}, locale),
            "{col3}",
            LocalizationUtil.getMessage("reporting.table63.groupCode", new Object[] {}, locale),
            "{col4}",
            LocalizationUtil.getMessage("reporting.table63.NumberOfResults", new Object[] {},
                locale)
        );
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable64(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        var fromYear = String.valueOf(assessmentResponses.getFirst().getFromYear());
        var toYear = String.valueOf(assessmentResponses.getFirst().getToYear());
        Map<String, String> replacements = getTable64Headers(locale, fromYear, toYear);

        List<List<String>> tableData = new ArrayList<>();
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c) &&
                        ClassificationPriorityMapping.isOnSciList(code)) {
                        tableData.add(
                            List.of(String.valueOf(tableData.size() + 1), publication.a, code));
                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        return new Pair<>(replacements,
            tableData.stream().sorted((r1, r2) -> classificationCodeSorter(r1.get(2), r2.get(2)))
                .toList());
    }

    private static Map<String, String> getTable64Headers(String locale, String fromYear,
                                                         String toYear) {
        return Map.of(
            "{header}",
            LocalizationUtil.getMessage("reporting.table64.header", new Object[] {fromYear, toYear},
                locale),
            "{col1}",
            LocalizationUtil.getMessage("reporting.table.rowNumber", new Object[] {}, locale),
            "{col2}",
            LocalizationUtil.getMessage("reporting.table64.publications", new Object[] {}, locale)
        );
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable67(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        var fromYear = String.valueOf(assessmentResponses.getFirst().getFromYear());
        var toYear = String.valueOf(assessmentResponses.getFirst().getToYear());
        Map<String, String> replacements = getTable67Headers(locale, fromYear, toYear);

        List<List<String>> tableData = new ArrayList<>();
        assessmentResponses.forEach(assessmentResponse -> {
            AtomicInteger numberOfSciPublications = new AtomicInteger();
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                if (ClassificationPriorityMapping.isOnSciList(code)) {
                    numberOfSciPublications.addAndGet(publications.size());
                }
            });
            tableData.add(List.of(String.valueOf(tableData.size() + 1), "",
                (locale.equalsIgnoreCase(LanguageAbbreviations.SERBIAN) ||
                    locale.equalsIgnoreCase(LanguageAbbreviations.SERBIAN_CYRILLIC)) ?
                    SerbianTransliteration.toCyrillic(assessmentResponse.getPersonName()) :
                    assessmentResponse.getPersonName(),
                getContent(assessmentResponse.getInstitutionName(), locale.toUpperCase()),
                String.valueOf(numberOfSciPublications.get())));
        });

        return new Pair<>(replacements, tableData);
    }

    private static Map<String, String> getTable67Headers(String locale, String fromYear,
                                                         String toYear) {
        return Map.of(
            "{header}",
            LocalizationUtil.getMessage("reporting.table67.header", new Object[] {fromYear, toYear},
                locale),
            "{col1}",
            LocalizationUtil.getMessage("reporting.table.rowNumber", new Object[] {}, locale),
            "{col2}",
            LocalizationUtil.getMessage("reporting.table67.personalIdentifier", new Object[] {},
                locale),
            "{col3}",
            LocalizationUtil.getMessage("reporting.table67.nameAndSurname", new Object[] {},
                locale),
            "{col4}", LocalizationUtil.getMessage("reporting.table67.employmentInstitutionName",
                new Object[] {}, locale),
            "{col5}", LocalizationUtil.getMessage("reporting.table67.numberOfSCIPublications",
                new Object[] {}, locale)
        );
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable67WithPosition(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        var fromYear = String.valueOf(assessmentResponses.getFirst().getFromYear());
        var toYear = String.valueOf(assessmentResponses.getFirst().getToYear());
        Map<String, String> replacements = getTable67PositionsHeaders(locale, fromYear, toYear);

        List<List<String>> tableData = new ArrayList<>();
        assessmentResponses.forEach(assessmentResponse -> {
            AtomicInteger numberOfSciPublications = new AtomicInteger();
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                if (ClassificationPriorityMapping.isOnSciList(code)) {
                    numberOfSciPublications.addAndGet(publications.size());
                }
            });
            tableData.add(List.of(String.valueOf(tableData.size() + 1), "",
                (locale.equalsIgnoreCase(LanguageAbbreviations.SERBIAN) || locale.equalsIgnoreCase(LanguageAbbreviations.SERBIAN_CYRILLIC)) ?
                    SerbianTransliteration.toCyrillic(assessmentResponse.getPersonName()) :
                    assessmentResponse.getPersonName(),
                Objects.nonNull(assessmentResponse.getPersonPosition()) ?
                    messageSource.getMessage(assessmentResponse.getPersonPosition().getValue(),
                        new Object[] {}, Locale.forLanguageTag(locale)) :
                    messageSource.getMessage("reporting.shouldCheck", new Object[] {},
                        Locale.forLanguageTag(locale)),
                String.valueOf(numberOfSciPublications.get())));
        });

        return new Pair<>(replacements, tableData);
    }

    private static Map<String, String> getTable67PositionsHeaders(String locale, String fromYear,
                                                                  String toYear) {
        return Map.of(
            "{header}", LocalizationUtil.getMessage("reporting.table67Positions.header",
                new Object[] {fromYear, toYear}, locale),
            "{col1}",
            LocalizationUtil.getMessage("reporting.table.rowNumber", new Object[] {}, locale),
            "{col2}",
            LocalizationUtil.getMessage("reporting.table67.personalIdentifier", new Object[] {},
                locale),
            "{col3}",
            LocalizationUtil.getMessage("reporting.table67.nameAndSurname", new Object[] {},
                locale),
            "{col4}",
            LocalizationUtil.getMessage("reporting.table67.employmentPosition", new Object[] {},
                locale),
            "{col5}", LocalizationUtil.getMessage("reporting.table67.numberOfSCIPublications",
                new Object[] {}, locale)
        );
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableTopLevelInstitution(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        var year = String.valueOf(assessmentResponses.getFirst().getToYear());
        Map<String, String> replacements = getTopLevelInstitutionTableHeaders(locale, year);

        List<List<String>> tableData = new ArrayList<>();
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c) &&
                        ClassificationPriorityMapping.isOnSciList(code)) {
                        var institutionIds =
                            assessmentResponse.getPublicationToInstitution().get(publication.c);
                        StringBuilder institutionName = new StringBuilder();
                        if (institutionIds.isEmpty()) {
                            institutionName = new StringBuilder(
                                messageSource.getMessage("reporting.shouldCheck", new Object[] {},
                                    Locale.forLanguageTag(locale)));
                        } else {
                            for (var institutionId : institutionIds) {
                                var index =
                                    organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                                        institutionId);
                                if (index.isEmpty()) {
                                    institutionName = new StringBuilder(
                                        messageSource.getMessage("reporting.shouldCheck",
                                            new Object[] {},
                                            Locale.forLanguageTag(locale)));
                                    break;
                                }

                                if (locale.equalsIgnoreCase(LanguageAbbreviations.SERBIAN) || locale.equalsIgnoreCase(LanguageAbbreviations.SERBIAN_CYRILLIC)) {
                                    institutionName.append(
                                            SerbianTransliteration.toCyrillic(index.get().getNameSr()))
                                        .append(",");
                                } else {
                                    institutionName.append(index.get().getNameOther()).append("\n");
                                }
                            }
                        }

                        tableData.add(
                            List.of(String.valueOf(tableData.size() + 1),
                                institutionName.toString(),
                                publication.a, code));
                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        return new Pair<>(replacements, tableData.stream().sorted(Comparator.comparingInt(
            rowA -> ClassificationPriorityMapping.getSciListPriority((rowA.get(3))))).toList());
    }

    private static Map<String, String> getTopLevelInstitutionTableHeaders(String locale,
                                                                          String year) {
        return Map.of(
            "{heading}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.header1",
                new Object[] {year}, locale),
            "{heading2}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.header2",
                new Object[] {year}, locale),
            "{sci}", LocalizationUtil.getMessage("reporting.table.sci", new Object[] {}, locale),
            "{col1}",
            LocalizationUtil.getMessage("reporting.table.rowNumber", new Object[] {}, locale),
            "{col2}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.institution",
                new Object[] {}, locale),
            "{col3}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.bibliography",
                new Object[] {}, locale),
            "{col4}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.category",
                new Object[] {}, locale),
            "{col5}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.categories",
                new Object[] {}, locale),
            "{col6}",
            LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.numberOfPublications",
                new Object[] {}, locale),
            "{col7}",
            LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.bibliographies",
                new Object[] {}, locale)
        );
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableTopLevelInstitutionSummary(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses,
        List<Integer> commissionIds, String locale) {

        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        String year = String.valueOf(assessmentResponses.getFirst().getToYear());
        Map<String, String> replacements = getTopLevelInstitutionSummaryTableHeaders(locale, year);

        Map<String, int[]> categoryToSums = computeCategorySums(assessmentResponses, commissionIds);

        List<List<String>> tableData = buildSummaryTable(categoryToSums);

        return new Pair<>(replacements, tableData);
    }

    private static Map<String, String> getTopLevelInstitutionSummaryTableHeaders(String locale,
                                                                                 String year) {
        return Map.of(
            "{heading}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.header1",
                new Object[] {year}, locale),
            "{heading2}",
            LocalizationUtil.getMessage("reporting.tableTopLevelInstitutionSummary.header2",
                new Object[] {year}, locale),
            "{col1}",
            LocalizationUtil.getMessage("reporting.table.rowNumber", new Object[] {}, locale),
            "{col2}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.category",
                new Object[] {}, locale),
            "{col3}",
            LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.numberOfPublications",
                new Object[] {}, locale),
            "{col4}",
            LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.bibliographies",
                new Object[] {}, locale)
        );
    }

    private static Map<String, int[]> computeCategorySums(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses,
        List<Integer> commissionIds) {

        Map<String, int[]> categoryToSums =
            new TreeMap<>(AssessmentReportGenerator::classificationCodeSorter);

        for (int i = 0; i < commissionIds.size(); i++) {
            int commissionId = commissionIds.get(i);
            int finalI = i;
            assessmentResponses.stream()
                .filter(response -> response.getCommissionId().equals(commissionId))
                .forEach(response -> response.getPublicationsPerCategory()
                    .forEach((category, publications) -> {
                        categoryToSums.computeIfAbsent(category,
                            k -> new int[commissionIds.size()]);
                        categoryToSums.get(category)[finalI] += publications.size();
                    })
                );
        }

        return categoryToSums;
    }

    private static List<List<String>> buildSummaryTable(Map<String, int[]> categoryToSums) {
        List<List<String>> tableData = new ArrayList<>();

        categoryToSums.forEach((category, sums) -> {
            List<String> rowData = new ArrayList<>();
            rowData.add(String.valueOf(tableData.size() + 1));
            rowData.add(category);
            Arrays.stream(sums).mapToObj(String::valueOf).forEach(rowData::add);
            tableData.add(rowData);
        });

        return tableData;
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableTopLevelInstitutionColored(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {

        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        String year = String.valueOf(assessmentResponses.getFirst().getToYear());
        Map<String, String> replacements = getTopLevelInstitutionColoredTableHeaders(locale, year);

        int commissionId = assessmentResponses.getFirst().getCommissionId();
        int organisationUnitId = userRepository.findOUIdForCommission(commissionId);
        Set<Integer> subOUs = new HashSet<>(
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId));

        List<List<String>> tableData =
            processPublicationsForColoredReport(assessmentResponses, subOUs);

        return new Pair<>(replacements, tableData);
    }

    private static Map<String, String> getTopLevelInstitutionColoredTableHeaders(String locale,
                                                                                 String year) {
        return Map.of(
            "{heading}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.header1",
                new Object[] {year}, locale),
            "{heading2}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.header2",
                new Object[] {year}, locale),
            "{sci}", LocalizationUtil.getMessage("reporting.table.sci", new Object[] {}, locale),
            "{col1}",
            LocalizationUtil.getMessage("reporting.table.rowNumber", new Object[] {}, locale),
            "{col2}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.bibliography",
                new Object[] {}, locale),
            "{col3}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.category",
                new Object[] {}, locale),
            "{col4}", LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.categories",
                new Object[] {}, locale),
            "{col5}",
            LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.numberOfPublications",
                new Object[] {}, locale),
            "{col6}",
            LocalizationUtil.getMessage("reporting.tableTopLevelInstitution.bibliographies",
                new Object[] {}, locale)
        );
    }

    private static List<List<String>> processPublicationsForColoredReport(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses,
        Set<Integer> subOUs) {

        List<List<String>> tableData = new ArrayList<>();
        Set<Integer> handledPublicationIds = new HashSet<>();

        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c) &&
                        ClassificationPriorityMapping.isOnSciList(code)) {
                        var institutionIds =
                            assessmentResponse.getPublicationToInstitution().get(publication.c);
                        var color = determineRowColor(new HashSet<>(institutionIds), subOUs);

                        tableData.add(List.of(
                            String.valueOf(tableData.size() + 1),
                            publication.a + "§" + color,
                            code
                        ));

                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        return tableData.stream()
            .sorted(Comparator.comparingInt(
                row -> ClassificationPriorityMapping.getSciListPriority(row.get(2))))
            .toList();
    }

    private static String determineRowColor(Set<Integer> institutionIds, Set<Integer> subOUs) {
        if (institutionIds.isEmpty()) {
            return TableRowColors.YELLOW;
        } else if (subOUs.containsAll(institutionIds)) {
            return TableRowColors.WHITE;
        }
        return TableRowColors.RED;
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableForAllPublications(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, Boolean colored) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        var commissionId = assessmentResponses.getFirst().getCommissionId();
        var organisationUnitId = userRepository.findOUIdForCommission(commissionId);
        var subOUs =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        Map<String, Pair<String, Integer>> publicationsPerGroup =
            new TreeMap<>(AssessmentReportGenerator::classificationCodeSorter);
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c)) {
                        var institutionIds =
                            assessmentResponse.getPublicationToInstitution().get(publication.c);
                        var color = TableRowColors.RED;
                        if (institutionIds.isEmpty()) {
                            color = TableRowColors.YELLOW;
                        } else if (new HashSet<>(subOUs).containsAll(institutionIds)) {
                            color = TableRowColors.WHITE;
                        }

                        var existingPair =
                            publicationsPerGroup.getOrDefault(code, new Pair<>("", 0));
                        existingPair.b += 1;
                        existingPair.a += "\n" + existingPair.b + " " + publication.a + (colored ?
                            ("§" + color) : "");
                        publicationsPerGroup.put(code, existingPair);
                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        List<List<String>> tableData = new ArrayList<>();
        publicationsPerGroup.forEach((code, titlesAndCount) -> {
            tableData.add(List.of(String.valueOf(tableData.size() + 1), code,
                String.valueOf(titlesAndCount.b),
                titlesAndCount.a));
        });

        return new Pair<>(new HashMap<>(), tableData);
    }

    public static List<String> constructDataForCommissionColumns(List<Integer> commissionIds,
                                                                 String locale) {
        var commissionNames = new ArrayList<String>();
        commissionIds.forEach(commissionId -> {
            var commission = commissionService.findOne(commissionId);
            commissionNames.add(getContent(
                MultilingualContentConverter.getMultilingualContentDTO(commission.getDescription()),
                locale.toUpperCase()));
        });

        return commissionNames;
    }

    private static String getContent(List<MultilingualContentDTO> contentList,
                                     String languageCode) {
        var localisedContent = contentList.stream()
            .filter(mc -> mc.getLanguageTag().equals(languageCode)).findFirst();
        if (localisedContent.isPresent()) {
            return (languageCode.equalsIgnoreCase(LanguageAbbreviations.SERBIAN) || languageCode.equalsIgnoreCase(LanguageAbbreviations.SERBIAN_CYRILLIC)) ?
                SerbianTransliteration.toCyrillic(localisedContent.get().getContent()) :
                localisedContent.get().getContent();
        }

        return contentList.stream()
            .findFirst()
            .map(mc -> (languageCode.equals(LanguageAbbreviations.SERBIAN)  || languageCode.equalsIgnoreCase(LanguageAbbreviations.SERBIAN_CYRILLIC)) ?
                SerbianTransliteration.toCyrillic(mc.getContent()) : mc.getContent())
            .orElseThrow(() -> new NotFoundException("Missing container title"));
    }

    private static int classificationCodeSorter(String s1, String s2) {
        if (ClassificationPriorityMapping.isOnSciList(s1) &&
            ClassificationPriorityMapping.isOnSciList(s2)) {
            return Integer.compare(ClassificationPriorityMapping.getSciListPriority(s1),
                ClassificationPriorityMapping.getSciListPriority(s2));
        }

        return s1.compareTo(s2);
    }
}
