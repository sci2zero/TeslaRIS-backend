package rs.teslaris.core.util.seeding;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CsvDataLoader {

    private static final String CSV_FILE_DIRECTORY = "src/main/resources/dbSeedData/";


    public void loadData(String fileName, Consumer<String[]> lineProcessor) {
        String filePath = CSV_FILE_DIRECTORY + fileName;

        processFile(filePath, reader -> {
            try {
                skipHeader(reader);
                String[] line;
                while ((line = reader.readNext()) != null) {
                    lineProcessor.accept(line); // Apply the processing function to each line
                }
            } catch (IOException | CsvValidationException e) {
                throw new RuntimeException(e); // Rethrow to ensure the caller handles it
            }
        });
    }

    public <T> void loadData(String filePath, T mappingConfiguration,
                             BiConsumer<String[], T> lineProcessor) {
        processFile(filePath, reader -> {
            try {
                String[] line;
                do {
                    line = reader.readNext();
                } while (Objects.isNull(line) || line.length == 1); // Skip empty lines

                skipHeader(reader);
                while ((line = reader.readNext()) != null) {
                    lineProcessor.accept(line,
                        mappingConfiguration); // Apply the processing function to each line
                }
            } catch (IOException | CsvValidationException e) {
                throw new RuntimeException(e); // Rethrow to ensure the caller handles it
            }
        });
    }

    private void processFile(String filePath, Consumer<CSVReader> readerProcessor) {
        if (Files.notExists(Paths.get(filePath))) {
            log.error("File not found: {}", filePath);
            return;
        }

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            readerProcessor.accept(reader);
            log.info("Successfully loaded from CSV: {}", filePath);
        } catch (IOException e) {
            log.error("Error while reading CSV file: {}. Reason: {}", filePath, e.getMessage());
        }
    }

    private void skipHeader(CSVReader reader) throws IOException, CsvValidationException {
        reader.readNext(); // Skip header line
    }
}
