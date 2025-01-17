package rs.teslaris.core.assessment.service.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.assessment.model.EntityClassificationSource;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.assessment.ruleengine.JournalClassificationRuleEngine;
import rs.teslaris.core.assessment.service.impl.cruddelegate.PublicationSeriesAssessmentClassificationJPAServiceImpl;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.PublicationSeriesAssessmentClassificationService;
import rs.teslaris.core.assessment.util.ClassificationMappingConfigurationLoader;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.seeding.CsvDataLoader;

@Service
@Transactional
@Slf4j
public class PublicationSeriesAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl
    implements PublicationSeriesAssessmentClassificationService {

    private final PublicationSeriesAssessmentClassificationJPAServiceImpl
        publicationSeriesAssessmentClassificationJPAService;

    private final PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;

    private final PublicationSeriesService publicationSeriesService;

    private final JournalService journalService;

    private final PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    private final JournalRepository journalRepository;

    private final JournalIndexRepository journalIndexRepository;

    private final CommissionService commissionService;

    private final TaskManagerService taskManagerService;

    private final AssessmentClassificationService assessmentClassificationService;

    private final String RULE_ENGINE_BASE_PACKAGE =
        "rs.teslaris.core.assessment.ruleengine.";

    private final CsvDataLoader csvDataLoader;

    private final String MNO_DIRECTORY =
        "src/main/resources/publicationSeriesClassifications/mno";


    @Autowired
    public PublicationSeriesAssessmentClassificationServiceImpl(
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        CommissionService commissionService,
        AssessmentClassificationService assessmentClassificationService,
        PublicationSeriesAssessmentClassificationJPAServiceImpl publicationSeriesAssessmentClassificationJPAService,
        PublicationSeriesAssessmentClassificationRepository publicationSeriesAssessmentClassificationRepository,
        PublicationSeriesService publicationSeriesService, JournalService journalService,
        PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository,
        JournalRepository journalRepository, JournalIndexRepository journalIndexRepository,
        CommissionService commissionService1, TaskManagerService taskManagerService,
        AssessmentClassificationService assessmentClassificationService1,
        CsvDataLoader csvDataLoader) {
        super(entityAssessmentClassificationRepository, commissionService,
            assessmentClassificationService);
        this.publicationSeriesAssessmentClassificationJPAService =
            publicationSeriesAssessmentClassificationJPAService;
        this.publicationSeriesAssessmentClassificationRepository =
            publicationSeriesAssessmentClassificationRepository;
        this.publicationSeriesService = publicationSeriesService;
        this.journalService = journalService;
        this.publicationSeriesIndicatorRepository = publicationSeriesIndicatorRepository;
        this.journalRepository = journalRepository;
        this.journalIndexRepository = journalIndexRepository;
        this.commissionService = commissionService1;
        this.taskManagerService = taskManagerService;
        this.assessmentClassificationService = assessmentClassificationService1;
        this.csvDataLoader = csvDataLoader;
    }

    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPublicationSeries(
        Integer publicationSeriesId) {
        return publicationSeriesAssessmentClassificationRepository.findAssessmentClassificationsForPublicationSeries(
                publicationSeriesId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .sorted((a, b) -> b.year().compareTo(a.year()))
            .collect(Collectors.toList());
    }

    @Override
    public PublicationSeriesAssessmentClassification createPublicationSeriesAssessmentClassification(
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO) {
        var newAssessmentClassification = new PublicationSeriesAssessmentClassification();

        setCommonFields(newAssessmentClassification, publicationSeriesAssessmentClassificationDTO);
        newAssessmentClassification.setPublicationSeries(
            publicationSeriesService.findOne(
                publicationSeriesAssessmentClassificationDTO.getPublicationSeriesId()));

        return publicationSeriesAssessmentClassificationJPAService.save(
            newAssessmentClassification);
    }

    @Override
    public void updatePublicationSeriesAssessmentClassification(
        Integer publicationSeriesAssessmentClassificationId,
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO) {
        var publicationSeriesAssessmentClassificationToUpdate =
            publicationSeriesAssessmentClassificationJPAService.findOne(
                publicationSeriesAssessmentClassificationId);

        setCommonFields(publicationSeriesAssessmentClassificationToUpdate,
            publicationSeriesAssessmentClassificationDTO);
        publicationSeriesAssessmentClassificationToUpdate.setPublicationSeries(
            publicationSeriesService.findOne(
                publicationSeriesAssessmentClassificationDTO.getPublicationSeriesId()));

        publicationSeriesAssessmentClassificationJPAService.save(
            publicationSeriesAssessmentClassificationToUpdate);
    }

    @Override
    public void performJournalClassification(Integer commissionId,
                                             List<Integer> classificationYears) {
        var commission = commissionService.findOne(commissionId);
        var className = commission.getFormalDescriptionOfRule();
        JournalClassificationRuleEngine ruleEngine;
        try {
            Class<?> clazz = Class.forName(RULE_ENGINE_BASE_PACKAGE + className);

            ruleEngine =
                (JournalClassificationRuleEngine) clazz.getDeclaredConstructor().newInstance();
            ruleEngine.initialize(publicationSeriesIndicatorRepository, journalRepository,
                journalIndexRepository, publicationSeriesAssessmentClassificationRepository,
                assessmentClassificationService);

            classificationYears.forEach((classificationYear) -> {
                ruleEngine.startClassification(classificationYear, commission);
            });
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + className);
        } catch (NoSuchMethodException e) {
            System.err.println("No default constructor found for: " + className);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error instantiating class: " + className);
        }
    }

    @Override
    public void scheduleClassification(LocalDateTime timeToRun, Integer commissionId,
                                       Integer userId, List<Integer> classificationYears) {
        var commission = commissionService.findOne(commissionId);
        taskManagerService.scheduleTask(
            "Publication_Series_Classification-" + commission.getFormalDescriptionOfRule() +
                "-" + StringUtils.join(classificationYears, "_") +
                "-" + UUID.randomUUID(), timeToRun,
            () -> performJournalClassification(commissionId, classificationYears), userId);
    }

    @Override
    public void scheduleClassificationLoading(LocalDateTime timeToRun,
                                              EntityClassificationSource source,
                                              Integer userId) {
        Runnable handlerFunction = switch (source) {
            case MNO -> this::loadPublicationSeriesClassificationsFromMNOFiles;
            default -> null;
        };

        taskManagerService.scheduleTask(
            "Publication_Series_Classification_Load-" + source.name() + "-" + UUID.randomUUID(),
            timeToRun,
            handlerFunction, userId);
    }

    private void loadPublicationSeriesClassificationsFromMNOFiles() {
        loadPublicationSeriesClassifications(
            MNO_DIRECTORY,
            "mno",
            ',',
            path -> true
        );
    }

    private void loadPublicationSeriesClassifications(String directory, String configKey,
                                                      char separator,
                                                      Predicate<Path> additionalFilter) {
        var dirPath = Paths.get(directory);

        var mapping =
            ClassificationMappingConfigurationLoader.fetchClassificationMappingConfiguration(
                configKey);
        if (Objects.isNull(mapping)) {
            log.error("Configuration {} does not exist", configKey);
            return;
        }

        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            try (var paths = Files.walk(dirPath)) {
                var csvFileCount = paths.filter(
                        path -> Files.isRegularFile(path) && path.toString().endsWith(".csv"))
                    .filter(additionalFilter)
                    .count();

                log.info("Loading {} csv files from {}", csvFileCount, directory);
                var counter = new AtomicInteger(1);

                try (var csvPaths = Files.walk(dirPath)) {
                    csvPaths.filter(
                            path -> Files.isRegularFile(path) && path.toString().endsWith(".csv"))
                        .filter(additionalFilter)
                        .forEach(csvFile -> {
                            csvDataLoader.loadAssessmentData(
                                csvFile.normalize().toAbsolutePath().toString(),
                                mapping, this::processClassificationsLine, mapping.yearParseRegex(),
                                separator,
                                mapping.parallelize());
                            log.info("Loaded {} of {}", counter.getAndIncrement(), csvFileCount);
                        });
                }
            } catch (IOException e) {
                log.error("An error occurred while reading {} files. Aborting. Reason: {}",
                    configKey, e.getMessage());
            }
        } else {
            log.error("Directory {} does not exist or is not a directory", directory);
        }
    }

    private void processClassificationsLine(String[] line,
                                            ClassificationMappingConfigurationLoader.ClassificationMapping mapping,
                                            Integer year) {
        if (line.length == 1) {
            log.info("Invalid line format. Skipping...");
            return;
        }

        var issnDetails = parseIssnDetails(line[mapping.issnColumn()], mapping.issnDelimiter());
        var publicationSeries = findOrCreatePublicationSeries(line, mapping, issnDetails);

        var classification = assessmentClassificationService.readAssessmentClassificationByCode(
            line[mapping.classificationColumn()]);
        var commission = commissionService.findOne(mapping.commissionId());
        var category = line[mapping.categoryColumn()];

        saveJournalClassification(publicationSeries, commission, classification, category, year);
    }

    private Pair<String, String> parseIssnDetails(String issnField, String delimiter) {
        var eIssn = "";
        var printIssn = "";

        if (issnField.contains(delimiter)) {
            var issnTokens = issnField.split(delimiter);
            printIssn = formatIssn(issnTokens[0]);
            if (issnTokens.length > 1) {
                eIssn = formatIssn(issnTokens[1]);
            }
        } else {
            printIssn = formatIssn(issnField);
        }

        if (eIssn.isEmpty()) {
            eIssn = printIssn;
        }

        return new Pair<>(printIssn, eIssn);
    }

    private PublicationSeries findOrCreatePublicationSeries(String[] line,
                                                            ClassificationMappingConfigurationLoader.ClassificationMapping mapping,
                                                            Pair<String, String> issnDetails) {
        var issnSpecified = !issnDetails.b.isEmpty() || !issnDetails.a.isEmpty();
        return journalService.findOrCreatePublicationSeries(
            line,
            mapping.defaultLanguage(),
            line[mapping.nameColumn()].trim(),
            issnDetails.b,
            issnDetails.a,
            issnSpecified
        );
    }

    private void saveJournalClassification(PublicationSeries publicationSeries,
                                           Commission commission,
                                           AssessmentClassification classification,
                                           String category,
                                           Integer year) {
        var journalClassification = new PublicationSeriesAssessmentClassification();
        journalClassification.setPublicationSeries(publicationSeries);
        journalClassification.setTimestamp(LocalDateTime.now());
        journalClassification.setCommission(commission);
        journalClassification.setClassificationYear(year);
        journalClassification.setCategoryIdentifier(category);
        journalClassification.setAssessmentClassification(classification);

        var existingClassification =
            publicationSeriesAssessmentClassificationRepository.findClassificationForPublicationSeriesAndCategoryAndYear(
                publicationSeries.getId(), category, year);
        existingClassification.ifPresent(
            publicationSeriesAssessmentClassificationRepository::delete
        );

        publicationSeriesAssessmentClassificationRepository.save(journalClassification);
    }

    private String formatIssn(String issn) {
        if (issn.isEmpty()) {
            return "";
        }

        issn = issn.replace("e", "")
            .replace("е", "") // cyrillic "е"
            .replace(":", "")
            .trim()
            .replace(" ", "");
        if (!issn.contains("-")) {
            issn = issn.substring(0, 4) + "-" + issn.substring(4, 8);
        }

        return issn.toUpperCase();
    }
}
