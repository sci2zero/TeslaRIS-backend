package rs.teslaris.revisioner.util.dataquality;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public class DataQualityAssessmentConfigurationLoader {

    private static final String CONFIG_DIRECTORY = "src/main/resources/dataQualityAssessment";

    private static LanguageTagService languageTagService;

    private static Map<String, DataQualityProfile> dataQualityProfiles = new HashMap<>();


    @Autowired
    public DataQualityAssessmentConfigurationLoader(LanguageTagService languageTagService) {
        DataQualityAssessmentConfigurationLoader.languageTagService = languageTagService;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10))
    protected static void reloadConfiguration() {
        try {
            Map<String, DataQualityProfile> loadedProfiles = new HashMap<>();

            try (var files = Files.list(Path.of(CONFIG_DIRECTORY))) {
                files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        var profile = stripExtension(path.getFileName().toString());

                        try {
                            DataQualityProfile config =
                                ConfigurationLoaderUtil.loadConfiguration(
                                    DataQualityProfile.class,
                                    path.toString(),
                                    null);

                            loadedProfiles.put(profile, config);

                            log.info("Loaded data quality profile '{}'", profile);
                        } catch (IOException e) {
                            throw new StorageException(
                                "Failed loading profile" + profile + ". Reason: " + e.getMessage());
                        }
                    });
            }

            dataQualityProfiles = loadedProfiles;
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload data quality profiles. Reason: " + e.getMessage());
        }
    }

    public static Set<String> listAvailableProfiles() {
        return dataQualityProfiles.keySet();
    }

    public static String getProfileVersion(String profileName) {
        return dataQualityProfiles.get(profileName.toLowerCase()).version();
    }

    public static Set<MultiLingualContent> getDataQualityRemark(String profile, String issueKey,
                                                                Object... params) {
        var remark = getRemark(profile.toLowerCase(), issueKey);

        return StringUtil.buildMultilingualContent(languageTagService, remark.message(), params);
    }

    @Nullable
    public static Triple<IssueSeverity, QualityDimension, Boolean> getIssueSeverityAndDimension(
        String profile, String issueKey) {
        var remark = getRemark(profile, issueKey);

        if (Objects.isNull(remark)) {
            log.error("No remark '{}' in profile '{}'", issueKey, profile);
            return null;
        }

        return new Triple<>(remark.severity(), remark.dimension(), remark.blocking());
    }

    public static double getIssuePoints(String profile, String issueKey) {
        var remark = getRemark(profile, issueKey);

        return Objects.isNull(remark) ? 0 : remark.points();
    }

    public static int getTotalRuleCount(String profile, String targetPrefix) {
        return Math.toIntExact(getProfile(profile).values().stream()
            .filter(remark -> Objects.nonNull(remark.target()))
            .filter(remark -> remark.target().startsWith(targetPrefix))
            .count());
    }

    public static double getTotalPointsRaw(String profile, String targetPrefix) {
        return getProfile(profile).values().stream()
            .filter(remark -> Objects.nonNull(remark.target()))
            .filter(remark -> remark.target().startsWith(targetPrefix))
            .mapToDouble(DataQualityRemark::points)
            .sum();
    }

    private static Map<String, DataQualityRemark> getProfile(String profile) {
        return dataQualityProfiles.getOrDefault(profile,
            new DataQualityProfile("1.0.0", Collections.emptyMap())).dataQualityRemarks();
    }

    @Nullable
    private static DataQualityRemark getRemark(String profile, String issueKey) {
        var remark = getProfile(profile.toLowerCase()).get(issueKey);

        if (Objects.isNull(remark)) {
            log.error("Missing issue '{}' in profile '{}'", issueKey, profile);
        }

        return remark;
    }

    private static String stripExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return index == -1
            ? filename
            : filename.substring(0, index);
    }


    private record DataQualityProfile(
        @JsonProperty(value = "version", required = true)
        String version,

        @JsonProperty(value = "dataQualityRemarks", required = true)
        Map<String, DataQualityRemark> dataQualityRemarks
    ) {
    }

    public record DataQualityRemark(
        @JsonProperty(value = "message", required = true)
        Map<String, String> message,

        @JsonProperty(value = "target")
        String target,

        @JsonProperty(value = "severity", required = true)
        IssueSeverity severity,

        @JsonProperty(value = "dimension", required = true)
        QualityDimension dimension,

        @JsonProperty(value = "blocking", required = true)
        boolean blocking,

        @JsonProperty(value = "points", required = true)
        double points
    ) {
    }
}
