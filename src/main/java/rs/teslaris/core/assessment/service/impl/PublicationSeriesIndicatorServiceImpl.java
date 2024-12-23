package rs.teslaris.core.assessment.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
        var dirPath = Paths.get(WOS_DIRECTORY);

        var mapping = IndicatorMappingConfigurationLoader.fetchPublicationSeriesIndicatorMapping(
            "webOfScience");
        if (Objects.isNull(mapping)) {
            log.error("Configuration webOfScience does not exist");
            return;
        }

        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            try (var paths = Files.walk(dirPath)) {
                paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".csv"))
                    .forEach(csvFile -> {
                        csvDataLoader.loadIndicatorData(
                            csvFile.normalize().toAbsolutePath().toString(),
                            mapping, this::processIndicatorsLine, mapping.yearParseRegex());
                    });
            } catch (IOException e) {
                log.error("An error occurred while reading WOS files. Aborting. Reason: {}",
                    e.getMessage());
            }
        }
    }

    @Override
    public void loadPublicationSeriesIndicatorsFromSCImagoCSVFiles() {
        var dirPath = Paths.get(SCIMAGO_DIRECTORY);

        var mapping = IndicatorMappingConfigurationLoader.fetchPublicationSeriesIndicatorMapping(
            "scimago");
        if (Objects.isNull(mapping)) {
            log.error("Configuration scimago does not exist");
            return;
        }

        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            try (var paths = Files.walk(dirPath)) {
                paths.filter(
                        path -> Files.isRegularFile(path) &&
                            path.getFileName().toString().startsWith("clean") &&
                            path.toString().endsWith(".csv"))
                    .forEach(csvFile -> {
                        csvDataLoader.loadIndicatorData(
                            csvFile.normalize().toAbsolutePath().toString(),
                            mapping, this::processIndicatorsLine, mapping.yearParseRegex());
                    });
            } catch (IOException e) {
                log.error("An error occurred while reading WOS files. Aborting. Reason: {}",
                    e.getMessage());
            }
        }
    }

    private void processIndicatorsLine(String[] line,
                                       IndicatorMappingConfigurationLoader.PublicationSeriesIndicatorMapping mapping,
                                       Integer year) {
        if (line.length == 1) {
            log.info("Invalid line format, skipping...");
            return;
        }

        var eIssn = cleanIssn(line[mapping.eIssnColumn()]);
        var printIssn = cleanIssn(line[mapping.printIssnColumn()]);
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
        var publicationSeries =
            publicationSeriesService.findPublicationSeriesByIssn(eIssn, printIssn);

        if (Objects.isNull(publicationSeries)) {
            var defaultLanguage =
                languageTagService.findLanguageTagByValue(mapping.defaultLanguage());
            var journalName = line[mapping.nameColumn()];
            publicationSeries =
                findPublicationSeriesByJournalName(journalName, defaultLanguage, eIssn, printIssn);
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
            var indicatorCode = mapping.columnMapping().get(columnNumber);
            var indicator = indicatorService.getIndicatorByCode(indicatorCode);

            if (Objects.isNull(indicator)) {
                log.error("Invalid indicator code '{}'. Skipping...", indicatorCode);
                continue;
            }

            var indicatorValue = line[Integer.parseInt(columnNumber)];
            saveIndicator(publicationSeries, indicator, indicatorValue,
                line[mapping.categoryColumn()].trim(), year, mapping.source());
        }
    }

    private void saveIndicator(PublicationSeries publicationSeries, Indicator indicator,
                               String indicatorValue, String categoryIdentifier, Integer year,
                               String source) {
        var newJournalIndicator = new PublicationSeriesIndicator();
        newJournalIndicator.setIndicator(indicator);
        newJournalIndicator.setPublicationSeries(publicationSeries);
        newJournalIndicator.setCategoryIdentifier(categoryIdentifier);
        newJournalIndicator.setSource(EntityIndicatorSource.valueOf(source));
        newJournalIndicator.setTimestamp(LocalDateTime.now());
        newJournalIndicator.setFromDate(LocalDate.of(year, 1, 1));
        newJournalIndicator.setToDate(LocalDate.of(year, 12, 31));

        switch (indicator.getContentType()) {
            case NUMBER:
                var valueToBeParsed = indicatorValue.trim().replace("N/A", "").replace(",", "");
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
