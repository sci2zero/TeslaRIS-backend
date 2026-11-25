package rs.teslaris.revisioner.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PolicyValidationException;
import rs.teslaris.revisioner.model.qualityassessment.PolicyUploadMode;
import rs.teslaris.revisioner.service.impl.DataQualityPolicyServiceImpl;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.ProfileVersionComparator;

@SpringBootTest
public class DataQualityPolicyServiceTest {

    private static final String PROFILE = "ptcris";

    @TempDir
    Path configurationDirectory;

    @Mock
    private LanguageTagService languageTagService;

    @InjectMocks
    private DataQualityPolicyServiceImpl dataQualityPolicyService;

    private static String policyJson(String remarkKey, String metric) {
        return """
            {
              "minimumRequiredScore": 60.0,
              "metricDefinitions": {
                "PTCRIS-F1-01DSTRUCT-01": {
                  "title": {"en": "Size and type standards", "sr": "Standardi"},
                  "description": {"en": "Data elements must conform.", "sr": "Elementi."}
                }
              },
              "targetWeights": {"Document.title": 5},
              "dataQualityRemarks": {
                "%s": {
                  "title": {"en": "Title too long", "sr": "Naslov je predug"},
                  "message": {"en": "Title \\"{0}\\" is too long.", "sr": "Naslov \\"{0}\\"."},
                  "target": "Document.title",
                  "severity": "ERROR",
                  "dimension": "STRUCTURAL_CONSISTENCY",
                  "metric": "%s",
                  "blocking": true,
                  "points": 8,
                  "usedForFairCompliance": false,
                  "constraints": {"maxLength": 255}
                }
              }
            }
            """.formatted(remarkKey, metric);
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "whatever-the-user-called-it.json",
            "application/json", content.getBytes(StandardCharsets.UTF_8));
    }

    private MockedStatic<DataQualityAssessmentConfigurationLoader> loader(List<String> versions) {
        var mockedLoader = mockStatic(DataQualityAssessmentConfigurationLoader.class);

        mockedLoader.when(DataQualityAssessmentConfigurationLoader::listAvailableProfiles)
            .thenReturn(Set.of(PROFILE));
        mockedLoader.when(
                () -> DataQualityAssessmentConfigurationLoader.listProfileVersions(PROFILE))
            .thenReturn(versions);
        mockedLoader.when(
                () -> DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(PROFILE))
            .thenReturn(versions.isEmpty() ? null : versions.getLast());
        mockedLoader.when(DataQualityAssessmentConfigurationLoader::getConfigurationDirectory)
            .thenReturn(configurationDirectory.toString());
        mockedLoader.when(
                () -> DataQualityAssessmentConfigurationLoader.getProfileName(PROFILE,
                    versions.isEmpty() ? null : versions.getLast()))
            .thenReturn(Map.of("en", "PTCRIS Data Governance"));

        return mockedLoader;
    }

    private Path storedVersion(String version, String content) throws IOException {
        var path = configurationDirectory.resolve(PROFILE).resolve(version + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);

        return path;
    }

    @Test
    public void shouldListEveryStoredVersionOfPolicy() {
        try (var ignored = loader(List.of("1.0.0", "1.1.0"))) {
            var policies = dataQualityPolicyService.listPolicies();

            assertEquals(1, policies.size());
            assertEquals(PROFILE, policies.getFirst().profileName());
            assertEquals(List.of("1.0.0", "1.1.0"), policies.getFirst().versions());
            assertEquals("1.1.0", policies.getFirst().latestVersion());
        }
    }

    @Test
    public void shouldDownloadStoredVersionUnchanged() throws IOException {
        var content = policyJson("titleTooLong", "PTCRIS-F1-01DSTRUCT-01");

        try (var ignored = loader(List.of("1.0.0"))) {
            storedVersion("1.0.0", content);

            var resource = dataQualityPolicyService.downloadPolicyVersion(PROFILE, "1.0.0");

            assertEquals(content,
                new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void shouldRefuseDownloadOfUnknownVersion() {
        try (var ignored = loader(List.of("1.0.0"))) {
            assertThrows(NotFoundException.class,
                () -> dataQualityPolicyService.downloadPolicyVersion(PROFILE, "9.9.9"));
        }
    }

    @Test
    public void shouldRefuseUploadForUnknownPolicy() {
        try (var ignored = loader(List.of("1.0.0"))) {
            var uploaded = file(policyJson("titleTooLong", "PTCRIS-F1-01DSTRUCT-01"));

            assertThrows(NotFoundException.class,
                () -> dataQualityPolicyService.uploadPolicy("nonexistent",
                    PolicyUploadMode.NEW_VERSION, uploaded));
        }
    }

    @Test
    public void shouldOverwriteOnlyLatestVersionAndIgnoreUploadedFileName() throws IOException {
        var previous = policyJson("titleTooLong", "PTCRIS-F1-01DSTRUCT-01");
        var replacement = policyJson("titleTooShort", "PTCRIS-F1-01DSTRUCT-01");

        try (var ignored = loader(List.of("1.0.0", "1.1.0"))) {
            var oldest = storedVersion("1.0.0", previous);
            var latest = storedVersion("1.1.0", previous);

            var result = dataQualityPolicyService.uploadPolicy(PROFILE,
                PolicyUploadMode.OVERWRITE_CURRENT, file(replacement));

            assertEquals("1.1.0", result.version());
            assertFalse(result.versionCreated());
            assertEquals(replacement, Files.readString(latest));
            assertEquals(previous, Files.readString(oldest));
            assertFalse(Files.exists(
                configurationDirectory.resolve(PROFILE)
                    .resolve("whatever-the-user-called-it.json")));
        }
    }

    @Test
    public void shouldCreateNextVersionNumerically() throws IOException {
        var content = policyJson("titleTooLong", "PTCRIS-F1-01DSTRUCT-01");

        try (var ignored = loader(List.of("1.0.0", "1.9.0"))) {
            storedVersion("1.9.0", content);

            var result = dataQualityPolicyService.uploadPolicy(PROFILE,
                PolicyUploadMode.NEW_VERSION, file(content));

            assertEquals("1.9.1", result.version());
            assertTrue(result.versionCreated());
            assertEquals(content,
                Files.readString(configurationDirectory.resolve(PROFILE).resolve("1.9.1.json")));
        }
    }

    @Test
    public void shouldSkipAVersionThatIsAlreadyStored() {
        var content = policyJson("titleTooLong", "PTCRIS-F1-01DSTRUCT-01");

        try (var ignored = loader(List.of("1.0.0", "1.0.1"))) {
            var result = dataQualityPolicyService.uploadPolicy(PROFILE,
                PolicyUploadMode.NEW_VERSION, file(content));

            assertEquals("1.0.2", result.version());
        }
    }

    @Test
    public void shouldRefusePolicyWithUnknownProperty() {
        var content = policyJson("titleTooLong", "PTCRIS-F1-01DSTRUCT-01")
            .replace("\"minimumRequiredScore\"", "\"minimumRequiredScoreTypo\"");

        try (var ignored = loader(List.of("1.0.0"))) {
            var exception = assertThrows(PolicyValidationException.class,
                () -> dataQualityPolicyService.uploadPolicy(PROFILE,
                    PolicyUploadMode.NEW_VERSION, file(content)));

            assertFalse(exception.getViolations().isEmpty());
            assertFalse(Files.exists(configurationDirectory.resolve(PROFILE)));
        }
    }

    @Test
    public void shouldRefusePolicyWhoseRuleNamesUnknownMetric() {
        var content = policyJson("titleTooLong", "NO-SUCH-METRIC");

        try (var ignored = loader(List.of("1.0.0"))) {
            var exception = assertThrows(PolicyValidationException.class,
                () -> dataQualityPolicyService.uploadPolicy(PROFILE,
                    PolicyUploadMode.NEW_VERSION, file(content)));

            assertEquals("unknownPolicyMetricMessage",
                exception.getViolations().get("dataQualityRemarks.titleTooLong.metric"));
        }
    }

    @Test
    public void shouldRefusePolicyWithInvalidSeverity() {
        var content = policyJson("titleTooLong", "PTCRIS-F1-01DSTRUCT-01")
            .replace("\"ERROR\"", "\"CATASTROPHIC\"");

        try (var ignored = loader(List.of("1.0.0"))) {
            assertThrows(PolicyValidationException.class,
                () -> dataQualityPolicyService.uploadPolicy(PROFILE,
                    PolicyUploadMode.NEW_VERSION, file(content)));
        }
    }

    @Test
    public void shouldRefusePolicyWithScoreOutOfRange() {
        var content = policyJson("titleTooLong", "PTCRIS-F1-01DSTRUCT-01")
            .replace("60.0", "160.0");

        try (var ignored = loader(List.of("1.0.0"))) {
            var exception = assertThrows(PolicyValidationException.class,
                () -> dataQualityPolicyService.uploadPolicy(PROFILE,
                    PolicyUploadMode.NEW_VERSION, file(content)));

            assertEquals("policyScoreOutOfRangeMessage",
                exception.getViolations().get("minimumRequiredScore"));
        }
    }

    @Test
    public void shouldRefuseEmptyFile() {
        try (var ignored = loader(List.of("1.0.0"))) {
            var uploaded = file("");

            var exception = assertThrows(PolicyValidationException.class,
                () -> dataQualityPolicyService.uploadPolicy(PROFILE,
                    PolicyUploadMode.NEW_VERSION, uploaded));

            assertEquals("emptyPolicyFileMessage", exception.getViolations().get("file"));
        }
    }

    @Test
    public void shouldWarnAboutMissingLanguageWithoutRefusing() {
        var content = policyJson("titleTooLong", "PTCRIS-F1-01DSTRUCT-01")
            .replace("\"sr\": \"Naslov je predug\"", "\"pt\": \"Titulo\"");

        try (var ignored = loader(List.of("1.0.0"))) {
            var result = dataQualityPolicyService.uploadPolicy(PROFILE,
                PolicyUploadMode.NEW_VERSION, file(content));

            assertTrue(result.warnings()
                .contains("dataQualityRemarks.titleTooLong.title.sr"));
        }
    }

    @Test
    public void shouldOrderVersionsByNumericSegment() {
        assertTrue(ProfileVersionComparator.INSTANCE.compare("1.10.0", "1.9.0") > 0);
        assertTrue(ProfileVersionComparator.INSTANCE.compare("1.2.0", "1.10.0") < 0);
        assertEquals(0, ProfileVersionComparator.INSTANCE.compare("1.2.0", "1.2.0"));
    }
}
