package rs.teslaris.core.assessment.util;

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
import rs.teslaris.core.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.util.FunctionalUtil;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.SerbianTransliteration;

@Component
public class ReportGenerationUtil {

    private static MessageSource messageSource;

    private static OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private static UserRepository userRepository;

    private static OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private static CommissionService commissionService;


    @Autowired
    public ReportGenerationUtil(MessageSource messageSource,
                                OrganisationUnitIndexRepository organisationUnitIndexRepository,
                                UserRepository userRepository, CommissionService commissionService,
                                OrganisationUnitsRelationRepository organisationUnitsRelationRepository) {
        ReportGenerationUtil.messageSource = messageSource;
        ReportGenerationUtil.organisationUnitIndexRepository = organisationUnitIndexRepository;
        ReportGenerationUtil.userRepository = userRepository;
        ReportGenerationUtil.organisationUnitsRelationRepository =
            organisationUnitsRelationRepository;
        ReportGenerationUtil.commissionService = commissionService;
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable63(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{header}", messageSource.getMessage("reporting.table63.header",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.table63.groupName",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}", messageSource.getMessage("reporting.table63.groupCode",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.table63.NumberOfResults",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

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

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable64(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{header}", messageSource.getMessage("reporting.table64.header",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getFromYear()),
                    String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.table64.publications",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

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

        return new Pair<>(replacements, tableData);
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable67(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{header}", messageSource.getMessage("reporting.table67.header",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getFromYear()),
                    String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.table67.personalIdentifier",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}", messageSource.getMessage("reporting.table67.nameAndSurname",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.table67.employmentInstitutionName",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col5}", messageSource.getMessage("reporting.table67.numberOfSCIPublications",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

        List<List<String>> tableData = new ArrayList<>();
        assessmentResponses.forEach(assessmentResponse -> {
            AtomicInteger numberOfSciPublications = new AtomicInteger();
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                if (ClassificationPriorityMapping.isOnSciList(code)) {
                    numberOfSciPublications.addAndGet(publications.size());
                }
            });
            tableData.add(List.of(String.valueOf(tableData.size() + 1), "",
                locale.equals("sr") ?
                    SerbianTransliteration.toCyrillic(assessmentResponse.getPersonName()) :
                    assessmentResponse.getPersonName(),
                getContent(assessmentResponse.getInstitutionName(), locale.toUpperCase()),
                String.valueOf(numberOfSciPublications.get())));
        });

        return new Pair<>(replacements, tableData);
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable67WithPosition(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{header}", messageSource.getMessage("reporting.table67Positions.header",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getFromYear()),
                    String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.table67.personalIdentifier",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}", messageSource.getMessage("reporting.table67.nameAndSurname",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.table67.employmentPosition",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col5}", messageSource.getMessage("reporting.table67.numberOfSCIPublications",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

        List<List<String>> tableData = new ArrayList<>();
        assessmentResponses.forEach(assessmentResponse -> {
            AtomicInteger numberOfSciPublications = new AtomicInteger();
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                if (ClassificationPriorityMapping.isOnSciList(code)) {
                    numberOfSciPublications.addAndGet(publications.size());
                }
            });
            tableData.add(List.of(String.valueOf(tableData.size() + 1), "",
                locale.equals("sr") ?
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

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableTopLevelInstitution(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{heading}", messageSource.getMessage("reporting.tableTopLevelInstitution.header1",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{heading2}", messageSource.getMessage("reporting.tableTopLevelInstitution.header2",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{sci}", messageSource.getMessage("reporting.table.sci",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.tableTopLevelInstitution.institution",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}", messageSource.getMessage("reporting.tableTopLevelInstitution.bibliography",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.tableTopLevelInstitution.category",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col5}", messageSource.getMessage("reporting.tableTopLevelInstitution.categories",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col6}",
            messageSource.getMessage("reporting.tableTopLevelInstitution.numberOfPublications",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col7}", messageSource.getMessage("reporting.tableTopLevelInstitution.bibliographies",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

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

                                if (locale.equals("sr")) {
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

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableTopLevelInstitutionSummary(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses,
        List<Integer> commissionIds, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{heading}", messageSource.getMessage("reporting.tableTopLevelInstitution.header1",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{heading2}",
            messageSource.getMessage("reporting.tableTopLevelInstitutionSummary.header2",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.tableTopLevelInstitution.category",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}",
            messageSource.getMessage("reporting.tableTopLevelInstitution.numberOfPublications",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.tableTopLevelInstitution.bibliographies",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

        var categoryToSums = new TreeMap<String, int[]>();
        FunctionalUtil.forEachWithCounter(commissionIds, (index, commissionId) -> {
            assessmentResponses.stream()
                .filter(response -> response.getCommissionId().equals(commissionId))
                .forEach(commissionResponse -> {
                    commissionResponse.getPublicationsPerCategory()
                        .forEach((category, publications) -> {
                            categoryToSums.computeIfAbsent(category,
                                k -> new int[commissionIds.size()]);
                            categoryToSums.get(category)[index] += publications.size();
                        });
                });
        });

        List<List<String>> tableData = new ArrayList<>();
        categoryToSums.forEach((category, sums) -> {
            var rowData = new ArrayList<String>();
            rowData.add(String.valueOf(tableData.size() + 1));
            rowData.add(category);
            Arrays.stream(sums).forEach(sum -> {
                rowData.add(String.valueOf(sum));
            });
            tableData.add(rowData);
        });

        return new Pair<>(replacements, tableData);
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableTopLevelInstitutionColored(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{heading}", messageSource.getMessage("reporting.tableTopLevelInstitution.header1",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{heading2}", messageSource.getMessage("reporting.tableTopLevelInstitution.header2",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{sci}", messageSource.getMessage("reporting.table.sci",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.tableTopLevelInstitution.bibliography",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}", messageSource.getMessage("reporting.tableTopLevelInstitution.category",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.tableTopLevelInstitution.categories",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col5}",
            messageSource.getMessage("reporting.tableTopLevelInstitution.numberOfPublications",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col6}", messageSource.getMessage("reporting.tableTopLevelInstitution.bibliographies",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

        var commissionId = assessmentResponses.getFirst().getCommissionId();
        var organisationUnitId = userRepository.findOUIdForCommission(commissionId);
        var subOUs =
            organisationUnitsRelationRepository.getSubOUsRecursive(organisationUnitId);
        subOUs.add(organisationUnitId);

        List<List<String>> tableData = new ArrayList<>();
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c) &&
                        ClassificationPriorityMapping.isOnSciList(code)) {
                        var institutionIds =
                            assessmentResponse.getPublicationToInstitution().get(publication.c);
                        var color = TableRowColors.RED;
                        if (institutionIds.isEmpty()) {
                            color = TableRowColors.YELLOW;
                        } else if (new HashSet<>(subOUs).containsAll(institutionIds)) {
                            color = TableRowColors.WHITE;
                        }

                        tableData.add(
                            List.of(String.valueOf(tableData.size() + 1),
                                publication.a + "§" + color, code));
                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        return new Pair<>(replacements, tableData.stream().sorted(Comparator.comparingInt(
            rowA -> ClassificationPriorityMapping.getSciListPriority((rowA.get(2))))).toList());
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableForAllPublications(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, Boolean colored) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        var commissionId = assessmentResponses.getFirst().getCommissionId();
        var organisationUnitId = userRepository.findOUIdForCommission(commissionId);
        var subOUs =
            organisationUnitsRelationRepository.getSubOUsRecursive(organisationUnitId);
        subOUs.add(organisationUnitId);

        Map<String, Pair<String, Integer>> publicationsPerGroup = new TreeMap<>(
            Comparator.comparingInt(ClassificationPriorityMapping::getSciListPriority).reversed());
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
            return languageCode.equals("SR") ?
                SerbianTransliteration.toCyrillic(localisedContent.get().getContent()) :
                localisedContent.get().getContent();
        }

        return contentList.stream()
            .findFirst()
            .map(mc -> languageCode.equals("SR") ?
                SerbianTransliteration.toCyrillic(mc.getContent()) : mc.getContent())
            .orElseThrow(() -> new NotFoundException("Missing container title"));
    }
}
