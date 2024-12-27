package rs.teslaris.core.util.seeding;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CsvDataLoader {

    private static final String CSV_FILE_DIRECTORY = "src/main/resources/dbSeedData/";


    public void loadData(String fileName, Consumer<String[]> lineProcessor) {
        String filePath = CSV_FILE_DIRECTORY + fileName;

        if (Files.notExists(Paths.get(filePath))) {
            log.error("File not found: {}", filePath);
            return;
        }

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            reader.readNext(); // Skip header line
            while ((line = reader.readNext()) != null) {
                lineProcessor.accept(line);  // Apply the processing function to each line
            }
            log.info("Successfully loaded from CSV: {}", fileName);
        } catch (IOException | CsvValidationException e) {
            log.error("Error while reading CSV file: {}. Reason: {}", fileName, e.getMessage());
        }
    }
}
