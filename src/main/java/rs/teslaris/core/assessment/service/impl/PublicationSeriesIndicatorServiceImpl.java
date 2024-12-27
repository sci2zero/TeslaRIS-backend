package rs.teslaris.core.assessment.service.impl;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.core.assessment.dto.PublicationSeriesIndicatorResponseDTO;
import rs.teslaris.core.assessment.model.EntityIndicatorSource;
import rs.teslaris.core.assessment.model.Indicator;
import rs.teslaris.core.assessment.model.PublicationSeriesIndicator;
import rs.teslaris.core.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.assessment.service.interfaces.PublicationSeriesIndicatorService;
import rs.teslaris.core.assessment.util.EntityIndicatorType;
import rs.teslaris.core.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.seeding.CsvDataLoader;

@Service
@Slf4j
public class PublicationSeriesIndicatorServiceImpl extends EntityIndicatorServiceImpl
    implements PublicationSeriesIndicatorService {

    private final PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    private final CsvDataLoader csvDataLoader;

    private final PublicationSeriesService publicationSeriesService;

    private final JournalService journalService;

    private final LanguageTagService languageTagService;

    private final TaskManagerService taskManagerService;

    private final String WOS_DIRECTORY = "src/main/resources/publicationSeriesIndicators/wos";

    private final String SCIMAGO_DIRECTORY =
        "src/main/resources/publicationSeriesIndicators/scimago";


    @Autowired
    public PublicationSeriesIndicatorServiceImpl(
        EntityIndicatorRepository entityIndicatorRepository,
        DocumentFileService documentFileService,
        IndicatorService indicatorService,
        PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository,
        CsvDataLoader csvDataLoader, PublicationSeriesService publicationSeriesService,
        JournalService journalService, LanguageTagService languageTagService,
        TaskManagerService taskManagerService) {
        super(indicatorService, entityIndicatorRepository, documentFileService);
        this.publicationSeriesIndicatorRepository = publicationSeriesIndicatorRepository;
        this.csvDataLoader = csvDataLoader;
        this.publicationSeriesService = publicationSeriesService;
        this.journalService = journalService;
        this.languageTagService = languageTagService;
        this.taskManagerService = taskManagerService;
    }

    @Override
    public List<PublicationSeriesIndicatorResponseDTO> getIndicatorsForPublicationSeries(
        Integer publicationSeriesId,
        AccessLevel accessLevel) {
        return publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeriesAndIndicatorAccessLevel(
            publicationSeriesId,
            accessLevel).stream().map(
            EntityIndicatorConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    public void scheduleIndicatorLoading(LocalDateTime timeToRun, EntityIndicatorSource source,
                                         Integer userId) {
        Runnable handlerFunction = switch (source) {
            case WEB_OF_SCIENCE -> this::loadPublicationSeriesIndicatorsFromWOSCSVFiles;
            case SCIMAGO -> this::loadPublicationSeriesIndicatorsFromSCImagoCSVFiles;
            default -> null;
        };

        taskManagerService.scheduleTask(
            "Publication_Series_task-" + source.name() + "-" + UUID.randomUUID(), timeToRun,
            handlerFunction, userId);
    }

    @Override
    public void loadPublicationSeriesIndicatorsFromWOSCSVFiles() {
        loadPublicationSeriesIndicators(
            WOS_DIRECTORY,
            "webOfScience",
            ',',
            path -> true // No additional filtering for WOS files
        );
    }

    @Override
    public void loadPublicationSeriesIndicatorsFromSCImagoCSVFiles() {
        loadPublicationSeriesIndicators(
            SCIMAGO_DIRECTORY,
            "scimago",
            ';',
            // Additional filtering for SCImago files
            path -> path.getFileName().toString().startsWith("scimago")
        );
    }

    private void loadPublicationSeriesIndicators(String directory, String configKey, char separator,
                                                 Predicate<Path> additionalFilter) {
        var dirPath = Paths.get(directory);

        var mapping = IndicatorMappingConfigurationLoader.fetchPublicationSeriesCSVIndicatorMapping(
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
                            csvDataLoader.loadIndicatorData(
                                csvFile.normalize().toAbsolutePath().toString(),
                                mapping, this::processIndicatorsLine, mapping.yearParseRegex(),
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

    private void processIndicatorsLine(String[] line,
                                       IndicatorMappingConfigurationLoader.PublicationSeriesIndicatorMapping mapping,
                                       Integer year) {
        if (line.length == 1) {
            log.info("Invalid line format. Skipping...");
            return;
        }

        var eIssn = cleanIssn(line[mapping.eIssnColumn()]);
        var printIssn = cleanIssn(line[mapping.printIssnColumn()]);
        if (eIssn.isEmpty() && printIssn.isEmpty()) {
            log.info("ISSN is not specified. Skipping...");
            return;
        }

        if (mapping.eIssnColumn().equals(mapping.printIssnColumn())) {
            var tokens = eIssn.split(mapping.identifierDelimiter());
            eIssn = cleanIssn(tokens[0]);
            if (tokens.length == 2) {
                printIssn = cleanIssn(tokens[1]);
            } else {
                printIssn = eIssn;
            }
        }

        var publicationSeries = findOrCreatePublicationSeries(line, mapping, eIssn, printIssn);

        processIndicatorValues(line, mapping, publicationSeries, year);
    }

    private String cleanIssn(String issn) {
        return StringUtil.formatIssn(issn.trim().replace("N/A", ""));
    }

    private PublicationSeries findOrCreatePublicationSeries(String[] line,
                                                            IndicatorMappingConfigurationLoader.PublicationSeriesIndicatorMapping mapping,
                                                            String eIssn, String printIssn) {
        PublicationSeries publicationSeries = null;
        try {
            publicationSeries =
                publicationSeriesService.findPublicationSeriesByIssn(eIssn, printIssn);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        if (Objects.isNull(publicationSeries)) {
            var defaultLanguage =
                languageTagService.findLanguageTagByValue(mapping.defaultLanguage());
            var journalName = line[mapping.nameColumn()];
            publicationSeries =
                findPublicationSeriesByJournalName(journalName, defaultLanguage, eIssn, printIssn);
        }

        if (publicationSeries.getPrintISSN().equals(publicationSeries.getEISSN()) &&
            !eIssn.equals(printIssn)) {
            publicationSeries.setEISSN(eIssn);
            publicationSeries.setPrintISSN(printIssn);
            publicationSeriesService.save(publicationSeries);
        }

        return publicationSeries;
    }

    private PublicationSeries findPublicationSeriesByJournalName(String journalName,
                                                                 LanguageTag defaultLanguage,
                                                                 String eIssn, String printIssn) {
        var potentialHits = journalService.searchJournals(
            Arrays.stream(journalName.split(" ")).toList(), PageRequest.of(0, 2)).getContent();

        for (var potentialHit : potentialHits) {
            for (var title : potentialHit.getTitleOther().split("\\|")) {
                if (title.equals(journalName)) {
                    var publicationSeries =
                        publicationSeriesService.findOne(potentialHit.getDatabaseId());
                    // TODO: is this ok?
                    publicationSeries.setEISSN(eIssn);
                    publicationSeries.setPrintISSN(printIssn);
                    return publicationSeriesService.save(publicationSeries);
                }
            }
        }

        return createNewJournal(journalName, defaultLanguage, eIssn, printIssn);
    }

    private PublicationSeries createNewJournal(String journalName, LanguageTag defaultLanguage,
                                               String eIssn, String printIssn) {
        var newJournal = new JournalDTO();
        newJournal.setTitle(List.of(new MultilingualContentDTO(defaultLanguage.getId(),
            defaultLanguage.getLanguageTag(), journalName, 1)));
        newJournal.setNameAbbreviation(new ArrayList<>());
        newJournal.setContributions(new ArrayList<>());
        newJournal.setEissn(eIssn);
        newJournal.setPrintISSN(printIssn);
        newJournal.setLanguageTagIds(List.of(defaultLanguage.getId()));

        return journalService.createJournal(newJournal, true);
    }

    private void processIndicatorValues(String[] line,
                                        IndicatorMappingConfigurationLoader.PublicationSeriesIndicatorMapping mapping,
                                        PublicationSeries publicationSeries, Integer year) {
        for (var columnNumber : mapping.columnMapping().keySet()) {
            var indicatorMappingConfiguration = mapping.columnMapping().get(columnNumber);
            var indicatorCode = indicatorMappingConfiguration.mapsTo().trim();

            var indicator = indicatorService.getIndicatorByCode(indicatorCode);

            if (Objects.isNull(indicator)) {
                log.error("Invalid indicator code '{}'. Skipping...", indicatorCode);
                continue;
            }

            var indicatorValue = line[Integer.parseInt(columnNumber.trim())];
            var edition = "";
            if (Objects.nonNull(mapping.editionColumn())) {
                edition = line[mapping.editionColumn()];
            }

            String categoryIdentifier = null;
            if (indicatorMappingConfiguration.type().equals(EntityIndicatorType.BY_CATEGORY)) {
                categoryIdentifier = line[mapping.categoryColumn()].trim();

                if (mapping.categoryColumn().equals(Integer.parseInt(columnNumber))) {
                    if (!mapping.categoryDelimiter().isEmpty()) {
                        var fieldValues = categoryIdentifier.split(mapping.categoryDelimiter());
                        for (var fieldValue : fieldValues) {
                            categoryIdentifier = parseCategoryIdentifier(fieldValue,
                                mapping.categoryFromIndicatorDiffRegex());
                            indicatorValue = parseIndicatorValue(fieldValue,
                                indicatorMappingConfiguration.parseRegex());

                            saveIndicator(publicationSeries, indicator, indicatorValue,
                                categoryIdentifier, year,
                                EntityIndicatorSource.valueOf(mapping.source()), edition);
                        }
                        continue;
                    }
                }
            }

            indicatorValue =
                parseIndicatorValue(indicatorValue, indicatorMappingConfiguration.parseRegex());

            saveIndicator(publicationSeries, indicator, indicatorValue,
                categoryIdentifier, year, EntityIndicatorSource.valueOf(mapping.source()), edition);
        }
    }

    @Nullable
    private String parseIndicatorValue(String indicatorValue, String indicatorParseRule) {
        if (Objects.isNull(indicatorParseRule) || indicatorParseRule.isEmpty()) {
            return indicatorValue;
        }

        var valuePattern = Pattern.compile(indicatorParseRule.trim());
        var matcher = valuePattern.matcher(indicatorValue);

        if (matcher.find()) {
            return matcher.group().trim();
        } else {
            log.error(
                "Error while parsing indicator value from column {} using {}",
                indicatorValue, indicatorParseRule);
            return null;
        }
    }

    private String parseCategoryIdentifier(String fieldValue, String pattern) {
        var valuePattern = Pattern.compile(pattern);
        var matcher = valuePattern.matcher(fieldValue);

        if (matcher.find()) {
            return matcher.group().trim();
        } else {
            log.error(
                "Error while parsing category identifier column {} using {}. Returning whole field.",
                fieldValue, pattern);
            return fieldValue.trim();
        }
    }

    private void saveIndicator(PublicationSeries publicationSeries, Indicator indicator,
                               String indicatorValue, String categoryIdentifier, Integer year,
                               EntityIndicatorSource source, String edition) {
        if (Objects.isNull(indicatorValue) ||
            publicationSeriesIndicatorRepository.existsByPublicationSeriesIdAndSourceAndYearAndCategory(
                publicationSeries.getId(), source, year, categoryIdentifier, indicator.getCode())) {
            return;
        }

        var newJournalIndicator = new PublicationSeriesIndicator();
        newJournalIndicator.setIndicator(indicator);
        newJournalIndicator.setPublicationSeries(publicationSeries);
        newJournalIndicator.setCategoryIdentifier(categoryIdentifier);
        newJournalIndicator.setSource(source);
        newJournalIndicator.setTimestamp(LocalDateTime.now());
        newJournalIndicator.setFromDate(LocalDate.of(year, 1, 1));
        newJournalIndicator.setToDate(LocalDate.of(year, 12, 31));

        if (Objects.nonNull(edition) && !edition.isEmpty()) {
            newJournalIndicator.setEdition(edition);
        }

        switch (indicator.getContentType()) {
            case NUMBER:
                var valueToBeParsed =
                    indicatorValue.trim().replace("N/A", "").replaceAll("[,<>]", "");
                if (!valueToBeParsed.isEmpty()) {
                    newJournalIndicator.setNumericValue(Double.parseDouble(valueToBeParsed));
                }
                break;
            case BOOL:
                newJournalIndicator.setBooleanValue(Boolean.parseBoolean(indicatorValue));
                break;
            default:
                newJournalIndicator.setTextualValue(indicatorValue);
        }

        publicationSeriesIndicatorRepository.save(newJournalIndicator);
    }
}
