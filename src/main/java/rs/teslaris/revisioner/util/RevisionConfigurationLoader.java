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
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

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

    public static Set<MultiLingualContent> getDataQualityRemark(String issueKey,
                                                                Object... params) {
        return StringUtil.buildMultilingualContent(languageTagService,
            revisionConfig.dataQualityRemarks.get(issueKey).message, params);
    }

    public static Triple<IssueSeverity, QualityDimension, Boolean> getIssueSeverityAndDimension(
        String issueKey) {
        var remark = revisionConfig.dataQualityRemarks.get(issueKey);
        return new Triple<>(remark.severity, remark.dimension, remark.blocking);
    }

    public static int getTotalRuleCount() {
        return revisionConfig.dataQualityRemarks().size();
    }

    private record RevisionConfig(
        @JsonProperty(value = "fieldExclusions", required = true)
        Map<String, List<String>> fieldExclusions,

        @JsonProperty(value = "migrationMappings", required = true)
        Map<String, Map<String, String>> migrationMappings,

        @JsonProperty(value = "dataQualityRemarks", required = true)
        Map<String, DataQualityRemark> dataQualityRemarks
    ) {
    }

    private record DataQualityRemark(
        @JsonProperty(value = "message", required = true)
        Map<String, String> message,

        @JsonProperty(value = "severity", required = true)
        IssueSeverity severity,

        @JsonProperty(value = "dimension", required = true)
        QualityDimension dimension,

        @JsonProperty(value = "blocking", required = true)
        boolean blocking,

        @JsonProperty(value = "weight", required = true)
        double weight
    ) {
    }
}
