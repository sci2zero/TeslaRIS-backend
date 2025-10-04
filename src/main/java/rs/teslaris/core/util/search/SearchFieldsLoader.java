package rs.teslaris.core.util.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.functional.Triple;

@Component
@RequiredArgsConstructor
public class SearchFieldsLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, SearchFields> cache = new ConcurrentHashMap<>();

    private final LanguageTagService languageTagService;


    @Scheduled(fixedRate = 1000 * 60 * 10) // Reload every 10 minutes
    public void reloadConfigurations() {
        for (String fileName : cache.keySet()) {
            try {
                loadConfiguration(fileName);
            } catch (IOException e) {
                throw new StorageException(
                    "Failed to reload classification mapping configuration for " + fileName + ": " +
                        e.getMessage());
            }
        }
    }

    public synchronized void loadConfiguration(String fileName) throws IOException {
        String baseFileName = "src/main/resources/searchFieldConfiguration/";
        SearchFields searchFields =
            objectMapper.readValue(new File(baseFileName + fileName), SearchFields.class);
        cache.put(fileName, searchFields);
    }

    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        String fileName, Boolean onlyExportFields) {
        lazyLoadCache(fileName);

        return cache.get(fileName).fields().stream()
            .filter(searchField -> !onlyExportFields ||
                searchField.canExport().equals(true))
            .map(thesisSearchField -> {
                var displayFieldName = new ArrayList<MultilingualContentDTO>();
                AtomicInteger priority = new AtomicInteger(1);
                thesisSearchField.displayName().forEach((lang, value) -> {
                    var languageTag = languageTagService.findLanguageTagByValue(lang.toUpperCase());
                    displayFieldName.add(
                        new MultilingualContentDTO(languageTag.getId(),
                            languageTag.getLanguageTag(),
                            value, priority.getAndIncrement()));
                });
                return new Triple<String, List<MultilingualContentDTO>, String>(
                    thesisSearchField.fieldName(), displayFieldName, thesisSearchField.type());
            }).toList();
    }

    public String getSearchFieldLocalizedName(String fileName, String searchFieldName,
                                              String lang) {
        lazyLoadCache(fileName);

        lang = lang.toLowerCase();
        var foundField = cache.get(fileName).fields().stream()
            .filter(field -> field.canExport() && field.fieldName.equals(searchFieldName))
            .findFirst();
        if (foundField.isEmpty()) {
            return searchFieldName;
        }

        if (!foundField.get().displayName.containsKey(lang)) {
            lang = "en";
        }

        return foundField.get().displayName.getOrDefault(lang, searchFieldName);
    }

    private void lazyLoadCache(String fileName) {
        if (!cache.containsKey(fileName)) {
            try {
                loadConfiguration(fileName);
            } catch (IOException e) {
                throw new LoadingException(
                    "Unable to load search configuration " + fileName); // should never happen
            }
        }
    }

    public SearchFields getConfiguration(String fileName) {
        lazyLoadCache(fileName);
        return cache.get(fileName);
    }

    public record SearchFields(
        @JsonProperty(value = "fields", required = true) List<SearchField> fields
    ) {
    }

    public record SearchField(
        @JsonProperty(value = "fieldName", required = true) String fieldName,
        @JsonProperty(value = "type", required = true) String type,
        @JsonProperty(value = "rule") String rule,
        @JsonProperty(value = "displayName", required = true) Map<String, String> displayName,
        @JsonProperty(value = "canExport", defaultValue = "false") Boolean canExport
    ) {
    }
}

