package rs.teslaris.revisioner.util.dataquality;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexmodel.EntityType;
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

    private static Map<String, NavigableMap<String, DataQualityProfile>> dataQualityProfiles =
        new HashMap<>();


    @Autowired
    public DataQualityAssessmentConfigurationLoader(LanguageTagService languageTagService) {
        DataQualityAssessmentConfigurationLoader.languageTagService = languageTagService;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10))
    protected static void reloadConfiguration() {
        try {
            Map<String, NavigableMap<String, DataQualityProfile>> loadedProfiles =
                new HashMap<>();

            try (var profileDirs = Files.list(Path.of(CONFIG_DIRECTORY))) {
                profileDirs
                    .filter(Files::isDirectory)
                    .forEach(profileDir -> {
                        var profileName = profileDir.getFileName().toString().toLowerCase();

                        NavigableMap<String, DataQualityProfile> versions = new TreeMap<>();

                        try (var versionFiles = Files.list(profileDir)) {
                            versionFiles
                                .filter(Files::isRegularFile)
                                .filter(p -> p.toString().endsWith(".json"))
                                .forEach(path -> {

                                    try {
                                        DataQualityProfile config =
                                            ConfigurationLoaderUtil.loadConfiguration(
                                                DataQualityProfile.class,
                                                path.toString(),
                                                null);

                                        var version = StringUtil.stripExtension(
                                            path.getFileName().toString());

                                        versions.put(version, preprocess(config, version));

                                        log.info("Loaded profile '{}' version '{}'", profileName,
                                            version);
                                    } catch (IOException e) {
                                        throw new StorageException(
                                            "Failed loading profile" + profileName + ". Reason: " +
                                                e.getMessage());
                                    }

                                });
                        } catch (IOException e) {
                            throw new StorageException(
                                "Failed loading profile" + profileName + ". Reason: " +
                                    e.getMessage());
                        }

                        loadedProfiles.put(profileName, versions);

                    });

            }

            dataQualityProfiles = loadedProfiles;
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload data quality profiles. Reason: " + e.getMessage());
        }
    }

    public static DataQualityProfile preprocess(DataQualityProfile config, String version) {
        Map<String, Double> totalPointsByTarget = new HashMap<>();
        Map<String, EnumMap<QualityDimension, Double>>
            totalPointsByTargetAndDimension =
            new HashMap<>();

        for (var remark : config.dataQualityRemarks().values()) {
            double weightedPoints = remark.points() *
                config.targetWeights().getOrDefault(remark.target(), 1.0);

            totalPointsByTarget.merge(remark.target(), weightedPoints,
                Double::sum);

            totalPointsByTargetAndDimension
                .computeIfAbsent(
                    remark.target(),
                    k -> new EnumMap<>(QualityDimension.class))
                .merge(
                    remark.dimension(),
                    weightedPoints,
                    Double::sum);
        }

        return new DataQualityProfile(
            version,
            config.targetWeights(),
            config.dataQualityRemarks(),
            totalPointsByTarget,
            totalPointsByTargetAndDimension
        );
    }

    private static DataQualityProfile getProfile(String profileName, String version) {
        var versions =
            dataQualityProfiles.get(profileName.toLowerCase());

        if (Objects.isNull(versions)) {
            throw new IllegalArgumentException(
                "Unknown profile: " + profileName);
        }

        var profile = versions.get(version);

        if (Objects.isNull(profile)) {
            throw new IllegalArgumentException(
                "Unknown profile version: "
                    + profileName
                    + " "
                    + version);
        }

        return profile;
    }

    public static Set<String> listAvailableProfiles() {
        return dataQualityProfiles.keySet();
    }

    public static String getLatestProfileVersion(String profileName) {
        return dataQualityProfiles
            .get(profileName.toLowerCase())
            .lastEntry()
            .getKey();
    }

    public static Map<String, Double> getTargetWeights(String profileName, String version) {
        return dataQualityProfiles.get(profileName.toLowerCase()).get(version).targetWeights();
    }

    public static Set<MultiLingualContent> getDataQualityRemark(String profile, String version,
                                                                String issueKey,
                                                                Object... params) {
        var remark = getRemark(profile.toLowerCase(), version, issueKey);

        return StringUtil.buildMultilingualContent(languageTagService, remark.message(), params);
    }

    @Nullable
    public static Triple<IssueSeverity, QualityDimension, Boolean> getIssueSeverityAndDimension(
        String profile, String version, String issueKey) {
        var remark = getRemark(profile, version, issueKey);

        if (Objects.isNull(remark)) {
            log.error("No remark '{}' in profile '{}'", issueKey, profile);
            return null;
        }

        return new Triple<>(remark.severity(), remark.dimension(), remark.blocking());
    }

    public static int getTotalRuleCount(String profile, String version, String targetPrefix) {
        return Math.toIntExact(
            getRemarksForProfile(profile, version)
                .values().stream()
                .filter(remark -> Objects.nonNull(remark.target()))
                .filter(remark -> remark.target().startsWith(targetPrefix))
                .count()
        );
    }

    public static double getTotalPointsWeighed(String profile, String version,
                                               String targetPrefix) {
        return dataQualityProfiles
            .get(profile.toLowerCase())
            .get(version)
            .totalPointsByTarget()
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(targetPrefix))
            .mapToDouble(Map.Entry::getValue)
            .sum();
    }

    public static double getTotalPointsWeighed(String profile, String version, String targetPrefix,
                                               QualityDimension dimension) {
        return dataQualityProfiles
            .get(profile.toLowerCase())
            .get(version)
            .totalPointsByTargetAndDimension()
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(targetPrefix))
            .map(Map.Entry::getValue)
            .mapToDouble(map -> map.getOrDefault(dimension, 0.0))
            .sum();
    }

    private static Map<String, DataQualityRemark> getRemarksForProfile(String profile,
                                                                       String version) {
        return dataQualityProfiles.getOrDefault(profile, new TreeMap<>())
            .getOrDefault(version,
                new DataQualityProfile(
                    "1.0.0",
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap()
                )
            ).dataQualityRemarks();
    }

    @Nullable
    private static DataQualityRemark getRemark(String profile, String version, String issueKey) {
        var remark = getRemarksForProfile(profile.toLowerCase(), version).get(issueKey);

        if (Objects.isNull(remark)) {
            log.error("Missing issue '{}' in profile '{}'", issueKey, profile);
        }

        return remark;
    }

    public static List<Map.Entry<String, DataQualityRemark>> getRulesForTarget(
        String profile,
        String version,
        String targetPrefix) {

        return getProfile(profile.toLowerCase(), version)
            .dataQualityRemarks()
            .entrySet()
            .stream()
            .filter(entry -> Objects.nonNull(entry.getValue().target()))
            .filter(entry -> entry.getValue().target().startsWith(targetPrefix))
            .sorted(Map.Entry.comparingByKey())
            .toList();
    }

    public static DataQualityRemark getIssue(String profile, String version, String issueKey) {
        return getRemark(profile.toLowerCase(), version, issueKey);
    }

    public static double getWeightedPoints(String profile, String version,
                                           DataQualityRemark remark) {

        if (Objects.isNull(remark)) {
            return 0;
        }

        return remark.points() *
            getTargetWeights(profile, version).getOrDefault(remark.target(), 1.0);
    }

    @Nullable
    public static String getTargetTypeFromEntityType(EntityType entityType) {
        return switch (entityType) {
            case BOOK_SERIES -> "PubSeries";
            case PUBLICATION, MONOGRAPH, PROCEEDINGS -> "Document";
            case EVENT, CONFERENCE, EXHIBITION, COURSE, OTHER_EVENT -> "Event";
            case JOURNAL -> "PublicationSeries";
            case ORGANISATION_UNIT -> "OrganisationUnit";
            case PERSON -> "Person";
            case PUBLISHER -> "Publisher";
            case PRIZE -> "Prize";
            case PROJECT -> "Project";
            default -> null;
        };
    }

    private record DataQualityProfile(
        @JsonProperty(value = "version")
        String version,

        @JsonProperty(value = "targetWeights", required = true)
        Map<String, Double> targetWeights,

        @JsonProperty(value = "dataQualityRemarks", required = true)
        Map<String, DataQualityRemark> dataQualityRemarks,

        Map<String, Double> totalPointsByTarget,

        Map<String, EnumMap<QualityDimension, Double>> totalPointsByTargetAndDimension
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
