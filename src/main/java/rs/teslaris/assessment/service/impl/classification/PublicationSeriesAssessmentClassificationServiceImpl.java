package rs.teslaris.assessment.service.impl.classification;

import jakarta.validation.ValidationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.classification.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.classification.EntityClassificationSource;
import rs.teslaris.assessment.model.classification.PublicationSeriesAssessmentClassification;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.ruleengine.JournalClassificationRuleEngine;
import rs.teslaris.assessment.service.impl.cruddelegate.PublicationSeriesAssessmentClassificationJPAServiceImpl;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.PublicationSeriesAssessmentClassificationService;
import rs.teslaris.assessment.util.AssessmentRulesConfigurationLoader;
import rs.teslaris.assessment.util.ClassificationBatchWriter;
import rs.teslaris.assessment.util.ClassificationMappingConfigurationLoader;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.seeding.CsvDataLoader;

@Service
@Transactional
@Slf4j
@Traceable
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

    private final TaskManagerService taskManagerService;

    private final String RULE_ENGINE_BASE_PACKAGE =
        "rs.teslaris.assessment.ruleengine.";

    private final CsvDataLoader csvDataLoader;

    private final ClassificationBatchWriter classificationBatchWriter;

    @Value("${assessment.classifications.publication-series.mno}")
    private String MNO_DIRECTORY;


    @Autowired
    public PublicationSeriesAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService,
        DocumentPublicationService documentPublicationService,
        ConferenceService conferenceService,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        PublicationSeriesAssessmentClassificationJPAServiceImpl publicationSeriesAssessmentClassificationJPAService,
        PublicationSeriesAssessmentClassificationRepository publicationSeriesAssessmentClassificationRepository,
        PublicationSeriesService publicationSeriesService, JournalService journalService,
        PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository,
        JournalRepository journalRepository, JournalIndexRepository journalIndexRepository,
        TaskManagerService taskManagerService, CsvDataLoader csvDataLoader,
        ClassificationBatchWriter classificationBatchWriter) {
        super(assessmentClassificationService, commissionService, documentPublicationService,
            conferenceService, entityAssessmentClassificationRepository);
        this.publicationSeriesAssessmentClassificationJPAService =
            publicationSeriesAssessmentClassificationJPAService;
        this.publicationSeriesAssessmentClassificationRepository =
            publicationSeriesAssessmentClassificationRepository;
        this.publicationSeriesService = publicationSeriesService;
        this.journalService = journalService;
        this.publicationSeriesIndicatorRepository = publicationSeriesIndicatorRepository;
        this.journalRepository = journalRepository;
        this.journalIndexRepository = journalIndexRepository;
        this.taskManagerService = taskManagerService;
        this.csvDataLoader = csvDataLoader;
        this.classificationBatchWriter = classificationBatchWriter;
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
        if (Objects.isNull(publicationSeriesAssessmentClassificationDTO.getClassificationYear())) {
            throw new ValidationException("You have to provide classification year.");
        }

        var newAssessmentClassification = new PublicationSeriesAssessmentClassification();
        newAssessmentClassification.setClassificationReason(
            AssessmentRulesConfigurationLoader.getRuleDescription("journalClassificationRules",
                "manual"));

        newAssessmentClassification.setCommission(
            commissionService.findOne(
                publicationSeriesAssessmentClassificationDTO.getCommissionId()));
        setCommonFields(newAssessmentClassification, publicationSeriesAssessmentClassificationDTO);
        newAssessmentClassification.setPublicationSeries(
            publicationSeriesService.findOne(
                publicationSeriesAssessmentClassificationDTO.getPublicationSeriesId()));

        var savedClassification = publicationSeriesAssessmentClassificationJPAService.save(
            newAssessmentClassification);
        journalService.reindexJournalVolatileInformation(
            newAssessmentClassification.getPublicationSeries().getId());

        return savedClassification;
    }

    @Override
    public void updatePublicationSeriesAssessmentClassification(
        Integer publicationSeriesAssessmentClassificationId,
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO) {
        if (Objects.isNull(publicationSeriesAssessmentClassificationDTO.getClassificationYear())) {
            throw new ValidationException("You have to provide classification year.");
        }

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
        journalService.reindexJournalVolatileInformation(
            publicationSeriesAssessmentClassificationToUpdate.getPublicationSeries().getId());
    }

    private void performJournalClassification(Integer commissionId,
                                              List<Integer> classificationYears,
                                              List<Integer> journalIds) {
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
                if (journalIds.isEmpty()) {
                    ruleEngine.startClassification(classificationYear, commission);
                } else {
                    ruleEngine.startClassification(classificationYear, commission, journalIds);
                }
            });
        } catch (ClassNotFoundException e) {
            log.error("Class not found: {}", className);
        } catch (NoSuchMethodException e) {
            log.error("No default constructor found for: {}", className);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Error instantiating class: {}", className);
        }
    }

    @Override
    public void scheduleClassification(LocalDateTime timeToRun, Integer commissionId,
                                       Integer userId, List<Integer> classificationYears,
                                       List<Integer> journalIds) {
        var commission = commissionService.findOne(commissionId);
        var taskId = taskManagerService.scheduleTask(
            "Publication_Series_Classification-" + commission.getFormalDescriptionOfRule() +
                "-" + StringUtils.join(classificationYears, "_") +
                "-" + UUID.randomUUID(), timeToRun,
            () -> performJournalClassification(commissionId, classificationYears, journalIds),
            userId, RecurrenceType.ONCE);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.JOURNAL_CLASSIFICATION, new HashMap<>() {{
                put("commissionId", commissionId);
                put("classificationYears", classificationYears);
                put("journalIds", journalIds);
                put("userId", userId);
            }}, RecurrenceType.ONCE));
    }

    @Override
    public void scheduleClassificationLoading(LocalDateTime timeToRun,
                                              EntityClassificationSource source,
                                              Integer userId, Integer commissionId) {
        Runnable handlerFunction = switch (source) {
            case MNO -> (() -> loadPublicationSeriesClassificationsFromMNOFiles(commissionId));
        };

        var taskId = taskManagerService.scheduleTask(
            "Publication_Series_Classification_Load-" + source.name() + "-" + UUID.randomUUID(),
            timeToRun, handlerFunction, userId, RecurrenceType.ONCE);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.JOURNAL_CLASSIFICATION_LOADING, new HashMap<>() {{
                put("source", source);
                put("commissionId", commissionId);
                put("userId", userId);
            }}, RecurrenceType.ONCE));
    }

    private void loadPublicationSeriesClassificationsFromMNOFiles(Integer commissionId) {
        var commission = commissionService.findOne(commissionId);
        loadPublicationSeriesClassifications(
            MNO_DIRECTORY,
            commission.getFormalDescriptionOfRule().replace("load-", ""),
            ',',
            commission,
            path -> true
        );
    }

    private void loadPublicationSeriesClassifications(String directory, String configKey,
                                                      char separator, Commission commission,
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
                                mapping,
                                ((line, mappingConf, year) -> processClassificationsLine(line,
                                    mappingConf, year, commission)), mapping.yearParseRegex(),
                                separator,
                                mapping.parallelize(), classificationBatchWriter);
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
                                            Integer year, Commission commission) {
        if (line.length == 1) {
            log.info("Invalid line format. Skipping...");
            return;
        }

        var classificationCode = line[mapping.classificationColumn()].trim();
        if (classificationCode.isEmpty() || classificationCode.equals("-")) {
            log.info("Classification not specified for column, skipping.");
            return;
        }

        var issnDetails = parseIssnDetails(line[mapping.issnColumn()], mapping.issnDelimiter());
        var publicationSeries = findOrCreatePublicationSeries(line, mapping, issnDetails);

        var classification =
            assessmentClassificationService.readAssessmentClassificationByCode(
                "journal" + classificationCode);
        var category = line[mapping.categoryColumn()].trim();

        for (var discriminator : mapping.discriminators()) {
            var discriminatorValue = line[discriminator.columnId()].trim();
            if (!discriminator.acceptedValues().contains(discriminatorValue)) {
                log.info("Discriminator check failed for value: {}", discriminatorValue);
                return;
            }
        }

        saveJournalClassification(publicationSeries, commission, classification, category, year);
    }

    private Pair<String, String> parseIssnDetails(String issnField, String delimiter) {
        issnField = issnField.replace("- ", "-").replace("е: ", "е:");
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

        // TODO: Is this one necessary, it is not adherent to the generic logic
        //  but was also too much of a hassle to put into configuration file. What should we do?
        journalClassification.setClassificationReason(
            AssessmentRulesConfigurationLoader.getRuleDescription("journalClassificationRules",
                classification.getCode().equals("journalM24") ? "M24MNO" : "MNO", year, category));


        publicationSeriesAssessmentClassificationRepository.deleteClassificationReasonsForPublicationSeriesAndCategoryAndYearAndCommission(
            publicationSeries.getId(), category, year, commission.getId());
        publicationSeriesAssessmentClassificationRepository.deleteClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
            publicationSeries.getId(), category, year, commission.getId());

        publicationSeriesAssessmentClassificationRepository.save(journalClassification);
    }

    private String formatIssn(String issn) {
        issn = issn.replace("e", "")
            .replace("е", "") // cyrillic "е"
            .replace("Х", "X") // cyrillic "Х"
            .replace(":", "")
            .replace("–", "-")
            .replace(".", "")
            .replace(",", "")
            .trim()
            .replace(" ", "");
        if (issn.isEmpty()) {
            return "";
        }

        if (!issn.contains("-")) {
            issn = issn.substring(0, 4) + "-" + issn.substring(4, 8);
        }

        return issn.toUpperCase();
    }
}
