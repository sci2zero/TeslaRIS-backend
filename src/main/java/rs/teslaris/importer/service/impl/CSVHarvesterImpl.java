package rs.teslaris.importer.service.impl;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.core.util.exceptionhandling.exception.DocumentHarvestException;
import rs.teslaris.importer.model.converter.harvest.CSVConverter;
import rs.teslaris.importer.service.interfaces.CSVHarvester;
import rs.teslaris.importer.utility.CommonImportUtility;

@Service
@RequiredArgsConstructor
public class CSVHarvesterImpl implements CSVHarvester {

    private static final List<String> TARGET_COLUMNS = List.of(
        "Document Type", "Authors", "Author full names", "Authors with affiliations", "Title",
        "Year", "Volume", "Issue|Number", "Page start", "Page end", "Pages", "Doi", "Link",
        "Abstract", "Author keywords", "Conference name", "Source title|Publication",
        "ISSN", "ISBN", "Conference code", "EID"
    );

    private final MongoTemplate mongoTemplate;

    private final MessageSource messageSource;


    @Override
    public Pair<String, String> getFormatDescription(String language) {
        var requiredCols = messageSource.getMessage(
            "csv.required.columns.description",
            new Object[] {},
            Locale.forLanguageTag(language)
        );

        var allParseableCols = messageSource.getMessage(
            "csv.parseable.columns.description",
            new Object[] {},
            Locale.forLanguageTag(language)
        );

        return new Pair<>(requiredCols, allParseableCols);
    }

    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId,
                                                               MultipartFile csvFile,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        try {
            List<String[]> processedRows = processCsvFile(csvFile);
            processedRows.forEach(
                row -> {
                    CSVConverter.toCommonImportModel(row)
                        .ifPresent(documentImport -> {
                            var existingImport = CommonImportUtility.findExistingImport(
                                documentImport.getIdentifier());
                            var embedding = CommonImportUtility.generateEmbedding(documentImport);

                            if (DeduplicationUtil.isDuplicate(existingImport, embedding)) {
                                return;
                            }

                            if (Objects.nonNull(embedding)) {
                                documentImport.setEmbedding(
                                    DeduplicationUtil.toDoubleList(embedding));
                            }

                            documentImport.getImportUsersId().add(userId);
                            mongoTemplate.save(documentImport, "documentImports");
                            newEntriesCount.merge(userId, 1, Integer::sum);
                        });
                });
            return newEntriesCount;
        } catch (IOException | CsvException e) {
            throw new DocumentHarvestException("Failed to process CSV file");
        }
    }

    private List<String[]> processCsvFile(MultipartFile csvFile) throws IOException, CsvException {
        try (var reader = new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8);
             var csvReader = new CSVReader(reader)) {

            List<String[]> allRows = csvReader.readAll();
            if (allRows.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, Integer> headerMap = createHeaderMap(allRows.getFirst());
            return normalizeRows(allRows, headerMap);
        }
    }

    private Map<String, Integer> createHeaderMap(String[] header) {
        var headerMap = new HashMap<String, Integer>();
        for (int i = 0; i < header.length; i++) {
            headerMap.put(header[i].toLowerCase().replaceAll("\\uFEFF+", ""), i);
        }
        return headerMap;
    }

    private List<String[]> normalizeRows(List<String[]> allRows, Map<String, Integer> headerMap) {
        var normalizedRows = new ArrayList<String[]>();
        normalizedRows.add(TARGET_COLUMNS.toArray(new String[0]));

        for (int i = 1; i < allRows.size(); i++) {
            var row = allRows.get(i);
            var normalizedRow = new String[TARGET_COLUMNS.size()];

            for (int j = 0; j < TARGET_COLUMNS.size(); j++) {
                normalizedRow[j] = getCellValue(row, TARGET_COLUMNS.get(j), headerMap);
            }

            normalizedRows.add(normalizedRow);
        }

        return normalizedRows;
    }

    private String getCellValue(String[] row, String columnPattern,
                                Map<String, Integer> headerMap) {
        var supportedColumnNames = columnPattern.toLowerCase().split("\\|");

        return headerMap.entrySet().stream()
            .filter(entry -> Arrays.asList(supportedColumnNames).contains(entry.getKey()))
            .findFirst()
            .map(entry -> {
                var columnIndex = entry.getValue();
                return columnIndex < row.length ? row[columnIndex] : "";
            })
            .orElse("");
    }
}
