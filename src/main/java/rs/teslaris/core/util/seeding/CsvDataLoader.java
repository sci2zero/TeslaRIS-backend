package rs.teslaris.core.util.seeding;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;

@Component
@Slf4j
public class CsvDataLoader {

    private static final String CSV_FILE_DIRECTORY = "src/main/resources/dbSeedData/";


    public void loadData(String fileName, Consumer<String[]> lineProcessor, char separator) {
        String filePath = CSV_FILE_DIRECTORY + fileName;

        processFile(filePath, separator, reader -> {
            try {
                reader.readNext(); // skip header
                String[] line;
                while ((line = reader.readNext()) != null) {
                    lineProcessor.accept(line);
                }
            } catch (Exception e) {
                throw new LoadingException(e.getMessage());
            }
        });
    }

    public <T> void loadIndicatorData(String filePath, T mappingConfiguration,
                                      TriConsumer<String[], T, Integer> lineProcessor,
                                      String yearParseRegex, char separator) {
        processFile(filePath, separator, reader -> {
            try {
                String[] line;
                do {
                    line = reader.readNext();
                } while (Objects.isNull(line) || line.length == 1); // Skip empty lines

                var headerLine = Strings.join(Arrays.asList(line), ';');
                var yearPattern = Pattern.compile(yearParseRegex);
                var matcher = yearPattern.matcher(headerLine);

                Integer year;
                if (matcher.find()) {
                    year = Integer.parseInt(matcher.group());
                } else {
                    throw new LoadingException("Error while parsing date from header line");
                }

                while ((line = reader.readNext()) != null) {
                    lineProcessor.accept(line,
                        mappingConfiguration, year);
                }
            } catch (Exception e) {
                throw new LoadingException(e.getMessage());
            }
        });
    }

    private void processFile(String filePath, char delimiter,
                             Consumer<CSVReader> readerProcessor) {
        if (Files.notExists(Paths.get(filePath))) {
            log.error("File not found: {}", filePath);
            return;
        }

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(filePath)).withCSVParser(
            new CSVParserBuilder().withSeparator(delimiter).build()).build()) {
            readerProcessor.accept(reader);
            log.info("Successfully loaded from CSV: {}", filePath);
        } catch (IOException e) {
            log.error("Error while reading CSV file: {}. Reason: {}", filePath, e.getMessage());
        }
    }
}
