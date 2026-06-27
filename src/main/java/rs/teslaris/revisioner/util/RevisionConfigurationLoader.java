package rs.teslaris.revisioner.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;
import rs.teslaris.core.util.search.StringUtil;

@Component
public class RevisionConfigurationLoader {

    private static String externalOverrideConfiguration;

    private static LanguageTagService languageTagService;

    private static RevisionConfig revisionConfig;


    @Autowired
    public RevisionConfigurationLoader(@Value("${assessment.classifications.priority-mapping}")
                                       String externalOverrideConfiguration,
                                       LanguageTagService languageTagService) {
        RevisionConfigurationLoader.externalOverrideConfiguration = externalOverrideConfiguration;
        RevisionConfigurationLoader.languageTagService = languageTagService;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    protected static void reloadConfiguration() {
        try {
            revisionConfig = ConfigurationLoaderUtil.loadConfiguration(
                RevisionConfig.class,
                "src/main/resources/revision/revisionConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload revision configuration: " + e.getMessage());
        }
    }

    public static Set<String> listExcludedFieldsForType(String entityType) {
        return new HashSet<>(
            revisionConfig.fieldExclusions().getOrDefault(entityType, Collections.emptyList()));
    }

    public static Map<String, String> getMigrationMappings(String entityType) {
        return revisionConfig.migrationMappings().getOrDefault(entityType, Collections.emptyMap());
    }

    public static Set<MultiLingualContent> getDataQualityRemark(String remarkKey,
                                                                Object... params) {
        return StringUtil.buildMultilingualContent(languageTagService,
            revisionConfig.dataQualityRemarks.get(remarkKey), params);
    }

    private record RevisionConfig(
        @JsonProperty(value = "fieldExclusions", required = true)
        Map<String, List<String>> fieldExclusions,

        @JsonProperty(value = "migrationMappings", required = true)
        Map<String, Map<String, String>> migrationMappings,

        @JsonProperty(value = "dataQualityRemarks", required = true)
        Map<String, Map<String, String>> dataQualityRemarks
    ) {
    }
}
