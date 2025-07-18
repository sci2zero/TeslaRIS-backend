package rs.teslaris.assessment.service.impl.indicator;


import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.assessment.dto.IFCategoryRanksDTO;
import rs.teslaris.assessment.dto.IFTableResponseDTO;
import rs.teslaris.assessment.dto.indicator.PublicationSeriesIndicatorResponseDTO;
import rs.teslaris.assessment.model.indicator.EntityIndicator;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.model.indicator.PublicationSeriesIndicator;
import rs.teslaris.assessment.repository.indicator.EntityIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.indicator.IndicatorService;
import rs.teslaris.assessment.service.interfaces.indicator.PublicationSeriesIndicatorService;
import rs.teslaris.assessment.util.EntityIndicatorType;
import rs.teslaris.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.seeding.CsvDataLoader;

@Service
@Slf4j
@Transactional
@Traceable
public class PublicationSeriesIndicatorServiceImpl extends EntityIndicatorServiceImpl
    implements PublicationSeriesIndicatorService {

    private final PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    private final CsvDataLoader csvDataLoader;

    private final JournalService journalService;

    private final TaskManagerService taskManagerService;

    private final JournalIndexRepository journalIndexRepository;

    @Value("${assessment.indicators.publication-series.wos}")
    private String WOS_DIRECTORY;

    @Value("${assessment.indicators.publication-series.scimago}")
    private String SCIMAGO_DIRECTORY;

    @Value("${assessment.indicators.publication-series.erih-plus}")
    private String ERIH_PLUS_DIRECTORY;

    @Value("${assessment.indicators.publication-series.mks}")
    private String SLAVISTS_DIRECTORY;


    @Autowired
    public PublicationSeriesIndicatorServiceImpl(IndicatorService indicatorService,
                                                 EntityIndicatorRepository entityIndicatorRepository,
                                                 DocumentFileService documentFileService,
                                                 PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository,
                                                 CsvDataLoader csvDataLoader,
                                                 JournalService journalService,
                                                 TaskManagerService taskManagerService,
                                                 JournalIndexRepository journalIndexRepository) {
        super(indicatorService, entityIndicatorRepository, documentFileService);
        this.publicationSeriesIndicatorRepository = publicationSeriesIndicatorRepository;
        this.csvDataLoader = csvDataLoader;
        this.journalService = journalService;
        this.taskManagerService = taskManagerService;
        this.journalIndexRepository = journalIndexRepository;
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
    public void computeFiveYearIFRank(List<Integer> classificationYears) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<JournalIndex> chunk =
                journalIndexRepository.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((journalIndex) -> {
                var currentJournal = journalService.findJournalById(journalIndex.getDatabaseId());
                classificationYears.forEach(classificationYear -> {
                    var currentJournalRankIndicators =
                        publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeriesAndCodeAndSourceAndYear(
                            journalIndex.getDatabaseId(), "currentJIFRank", classificationYear,
                            EntityIndicatorSource.WEB_OF_SCIENCE);

                    if (currentJournalRankIndicators.isEmpty()) {
                        return;
                    }

                    var distinctCategoryIdentifiers = currentJournalRankIndicators.stream()
                        .map(PublicationSeriesIndicator::getCategoryIdentifier)
                        .filter(Objects::nonNull)
                        .filter(categoryIdentifier -> !categoryIdentifier.isEmpty())
                        .distinct()
                        .toList();

                    distinctCategoryIdentifiers.forEach(categoryIdentifier -> {
                        performIF5RankComputation(classificationYear, currentJournal,
                            categoryIdentifier,
                            currentJournalRankIndicators.getFirst().getEdition());
                    });
                });
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void performIF5RankComputation(Integer classificationYear, Journal currentJournal,
                                           String categoryIdentifier, String edition) {
        var journalInSameCategoryIds =
            publicationSeriesIndicatorRepository.findIndicatorsForCategoryAndYearAndSource(
                categoryIdentifier, classificationYear, EntityIndicatorSource.WEB_OF_SCIENCE);
        var allIF5Values =
            publicationSeriesIndicatorRepository.findJournalIndicatorsForIdsAndCodeAndYearAndSource(
                journalInSameCategoryIds, "fiveYearJIF", classificationYear,
                EntityIndicatorSource.WEB_OF_SCIENCE);

        var sortedIF5Values = allIF5Values.stream()
            .filter((indicator) -> Objects.nonNull(indicator.getNumericValue()))
            .sorted(
                Comparator.comparing(EntityIndicator::getNumericValue, Comparator.reverseOrder()))
            .toList();

        var rankMap = new HashMap<Double, Integer>();
        AtomicInteger currentRank = new AtomicInteger(1);

        // Build rank map based on value occurrences
        sortedIF5Values.forEach(indicator ->
            rankMap.computeIfAbsent(indicator.getNumericValue(), v -> currentRank.getAndIncrement())
        );

        int rank = rankMap.getOrDefault(
            sortedIF5Values.stream()
                .filter(ind -> Objects.equals(ind.getPublicationSeries().getId(),
                    currentJournal.getId()))
                .map(EntityIndicator::getNumericValue)
                .findFirst()
                .orElse(Double.NaN),
            sortedIF5Values.size()
        );

        var if5Rank = new PublicationSeriesIndicator();
        if5Rank.setCategoryIdentifier(categoryIdentifier);
        if5Rank.setFromDate(LocalDate.of(classificationYear, 1, 1));
        if5Rank.setToDate(LocalDate.of(classificationYear, 12, 31));
        if5Rank.setPublicationSeries(currentJournal);
        if5Rank.setIndicator(indicatorService.getIndicatorByCode("fiveYearJIFRank"));
        if5Rank.setTextualValue(rank + "/" + sortedIF5Values.size());
        if5Rank.setEdition(edition);
        if5Rank.setTimestamp(LocalDateTime.now());
        if5Rank.setSource(EntityIndicatorSource.WEB_OF_SCIENCE);

        var existingIndicatorValue =
            publicationSeriesIndicatorRepository.existsByPublicationSeriesIdAndSourceAndYearAndCategory(
                currentJournal.getId(), if5Rank.getSource(), if5Rank.getFromDate(),
                categoryIdentifier,
                "fiveYearJIFRank");
        existingIndicatorValue.ifPresent(publicationSeriesIndicatorRepository::delete);

        publicationSeriesIndicatorRepository.save(if5Rank);
    }

    @Override
    public void scheduleIF5RankComputation(LocalDateTime timeToRun,
                                           List<Integer> classificationYears, Integer userId) {

        taskManagerService.scheduleTask(
            "Publication_Series_IF5Rank_Compute-" + EntityIndicatorSource.WEB_OF_SCIENCE.name() +
                "-" + StringUtils.join(classificationYears, "_") +
                "-" + UUID.randomUUID(),
            timeToRun,
            () -> computeFiveYearIFRank(classificationYears), userId);
    }

    @Override
    public IFTableResponseDTO getIFTableContent(Integer publicationSeriesId,
                                                Integer fromYear, Integer toYear) {
        var contentIndicators = IndicatorMappingConfigurationLoader.getIFTableContent();
        var indicatorValues = contentIndicators.stream()
            .flatMap(indicatorCode -> publicationSeriesIndicatorRepository
                .findIndicatorsForPublicationSeriesAndCodeInPeriod(
                    publicationSeriesId, indicatorCode, fromYear, toYear)
                .stream())
            .toList();

        var ifResponse = new IFTableResponseDTO();
        ifResponse.setIf2Values(extractIFValues(indicatorValues, contentIndicators.getFirst()));
        ifResponse.setIf5Values(extractIFValues(indicatorValues, contentIndicators.get(2)));

        var groupedByCategory = indicatorValues.stream()
            .filter(ind -> ind.getCategoryIdentifier() != null)
            .collect(Collectors.groupingBy(PublicationSeriesIndicator::getCategoryIdentifier));

        var tableContent = groupedByCategory.entrySet().stream()
            .map(
                entry -> createCategoryContent(entry.getKey(), entry.getValue(), contentIndicators))
            .toList();

        ifResponse.setIfTableContent(tableContent);
        return ifResponse;
    }

    private List<Pair<Integer, String>> extractIFValues(List<PublicationSeriesIndicator> indicators,
                                                        String indicatorCode) {
        return indicators.stream()
            .filter(ind -> ind.getIndicator().getCode().equals(indicatorCode))
            .sorted(Comparator.comparingInt(ind -> ind.getFromDate().getYear()))
            .map(ind -> new Pair<>(ind.getFromDate().getYear(),
                String.valueOf(ind.getNumericValue())))
            .toList();
    }

    private IFCategoryRanksDTO createCategoryContent(String category,
                                                     List<PublicationSeriesIndicator> indicators,
                                                     List<String> contentIndicators) {
        var categoryContent = new IFCategoryRanksDTO();
        categoryContent.setCategory(category);

        categoryContent.setIf2Ranks(
            extractCategoryRanks(indicators, contentIndicators.get(1), category));
        categoryContent.setIf5Ranks(
            extractCategoryRanks(indicators, contentIndicators.getLast(), category));

        return categoryContent;
    }

    private List<Pair<Integer, String>> extractCategoryRanks(
        List<PublicationSeriesIndicator> indicators, String indicatorCode, String category) {
        return indicators.stream()
            .filter(ind -> ind.getIndicator().getCode().equals(indicatorCode) &&
                category.equals(ind.getCategoryIdentifier()))
            .sorted(Comparator.comparingInt(ind -> ind.getFromDate().getYear()))
            .map(ind -> new Pair<>(ind.getFromDate().getYear(), ind.getTextualValue()))
            .toList();
    }

    @Override
    public void scheduleIndicatorLoading(LocalDateTime timeToRun, EntityIndicatorSource source,
                                         Integer userId) {
        Runnable handlerFunction = switch (source) {
            case WEB_OF_SCIENCE -> this::loadPublicationSeriesIndicatorsFromWOSCSVFiles;
            case SCIMAGO -> this::loadPublicationSeriesIndicatorsFromSCImagoCSVFiles;
            case ERIH_PLUS -> this::loadPublicationSeriesIndicatorsFromErihPlusCSVFiles;
            case MKS_SLAVISTS -> this::loadPublicationSeriesIndicatorsFromSlavistCSVFiles;
            default -> null;
        };

        taskManagerService.scheduleTask(
            "Publication_Series_Indicator_Load-" + source.name() + "-" + UUID.randomUUID(),
            timeToRun,
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

    @Override
    public void loadPublicationSeriesIndicatorsFromErihPlusCSVFiles() {
        loadPublicationSeriesIndicators(
            ERIH_PLUS_DIRECTORY,
            "erihPlus",
            ';',
            path -> true
        );
    }

    @Override
    public void loadPublicationSeriesIndicatorsFromSlavistCSVFiles() {
        loadPublicationSeriesIndicators(
            SLAVISTS_DIRECTORY,
            "mksSlavists",
            ',',
            path -> true
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
                            csvDataLoader.loadAssessmentData(
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

        if (Objects.nonNull(mapping.discriminator()) && !mapping.discriminator().isEmpty()) {
            var tokens = mapping.discriminator().split("§");
            var discriminatorColumnValue = line[Integer.parseInt(tokens[0])];
            if (!discriminatorColumnValue.contains(
                tokens[1])) { // TODO: Maybe use regex here? Is contains enough?
                log.info("Discriminator check failed (value is {}), skipping column.",
                    discriminatorColumnValue);
                return;
            }
        }

        var eIssn =
            Objects.nonNull(mapping.eIssnColumn()) ? cleanIssn(line[mapping.eIssnColumn()]) : "";
        var printIssn = Objects.nonNull(mapping.printIssnColumn()) ?
            cleanIssn(line[mapping.printIssnColumn()]) : "";
        var issnSpecified = !eIssn.isEmpty() || !printIssn.isEmpty();

        if (issnSpecified && mapping.eIssnColumn().equals(mapping.printIssnColumn())) {
            var tokens = eIssn.split(mapping.identifierDelimiter());
            eIssn = cleanIssn(tokens[0]);
            if (tokens.length == 2) {
                printIssn = cleanIssn(tokens[1]);
            } else {
                printIssn = eIssn;
            }
        }

        var publicationSeries =
            journalService.findOrCreatePublicationSeries(line, mapping.defaultLanguage(),
                line[mapping.nameColumn()].trim(), eIssn, printIssn, issnSpecified);

        LocalDate startDate, endDate;
        if (Objects.nonNull(year)) {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        } else {
            var yearPattern = Pattern.compile(mapping.yearParseRegex());

            Function<Integer, LocalDate> parseDateFromColumn = columnIndex -> {
                if (Objects.isNull(columnIndex)) {
                    return null;
                }
                var matcher = yearPattern.matcher(line[columnIndex].trim());
                if (matcher.find()) {
                    try {
                        return LocalDate.parse(matcher.group());
                    } catch (Exception exception) {
                        return LocalDate.of(Integer.parseInt(matcher.group()), 1, 1);
                    }
                }
                throw new IllegalArgumentException("Invalid date format in column: " + columnIndex);
            };

            startDate = parseDateFromColumn.apply(mapping.startDateColumn());
            if (startDate == null) {
                startDate = LocalDate.now();
            }

            endDate = parseDateFromColumn.apply(mapping.endDateColumn());
        }

        processIndicatorValues(line, mapping, publicationSeries, startDate, endDate);
    }

    private String cleanIssn(String issn) {
        return StringUtil.formatIssn(issn.trim().toUpperCase().replace("N/A", ""));
    }

    private void processIndicatorValues(String[] line,
                                        IndicatorMappingConfigurationLoader.PublicationSeriesIndicatorMapping mapping,
                                        PublicationSeries publicationSeries, LocalDate startDate,
                                        LocalDate endDate) {
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
                                categoryIdentifier, EntityIndicatorSource.valueOf(mapping.source()),
                                edition, startDate, endDate);
                        }
                        continue;
                    }
                }
            }

            indicatorValue =
                parseIndicatorValue(indicatorValue, indicatorMappingConfiguration.parseRegex());

            saveIndicator(publicationSeries, indicator, indicatorValue, categoryIdentifier,
                EntityIndicatorSource.valueOf(mapping.source()), edition, startDate, endDate);
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
            if (matcher.groupCount() > 0) {
                return matcher.group(1).trim();
            } else {
                return matcher.group().trim();
            }
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
                               String indicatorValue, String categoryIdentifier,
                               EntityIndicatorSource source, String edition, LocalDate startDate,
                               LocalDate endDate) {
        if (Objects.isNull(indicatorValue)) {
            return;
        }

        var existingIndicatorValue =
            publicationSeriesIndicatorRepository.existsByPublicationSeriesIdAndSourceAndYearAndCategory(
                publicationSeries.getId(), source, startDate, categoryIdentifier,
                indicator.getCode());
        existingIndicatorValue.ifPresent(publicationSeriesIndicatorRepository::delete);

        var newJournalIndicator = new PublicationSeriesIndicator();
        newJournalIndicator.setIndicator(indicator);
        newJournalIndicator.setPublicationSeries(publicationSeries);
        newJournalIndicator.setCategoryIdentifier(categoryIdentifier);
        newJournalIndicator.setSource(source);
        newJournalIndicator.setTimestamp(LocalDateTime.now());

        newJournalIndicator.setFromDate(startDate);
        newJournalIndicator.setToDate(endDate);

        if (Objects.nonNull(edition) && !edition.isEmpty()) {
            newJournalIndicator.setEdition(edition);
        }

        switch (indicator.getContentType()) {
            case NUMBER:
                indicatorValue = indicatorValue.trim();
                var correctionValue = 0.0;
                if (indicatorValue.contains("<")) {
                    correctionValue = -0.01;
                } else if (indicatorValue.contains(">")) {
                    correctionValue = 0.01;
                }

                var valueToBeParsed =
                    indicatorValue.toUpperCase().replace("N/A", "").replaceAll("[,<>]", "");
                if (!valueToBeParsed.isEmpty()) {
                    newJournalIndicator.setNumericValue(
                        Double.parseDouble(valueToBeParsed) + correctionValue);
                }
                break;
            case BOOL:
                if (indicatorValue.isEmpty()) {
                    newJournalIndicator.setBooleanValue(false);
                } else if ("true".equalsIgnoreCase(indicatorValue) ||
                    "false".equalsIgnoreCase(indicatorValue)) {
                    newJournalIndicator.setBooleanValue(
                        Boolean.parseBoolean(indicatorValue.trim()));
                } else {
                    newJournalIndicator.setBooleanValue(true);
                }
                break;
            default:
                newJournalIndicator.setTextualValue(indicatorValue.trim());
        }

        publicationSeriesIndicatorRepository.save(newJournalIndicator);
    }
}
