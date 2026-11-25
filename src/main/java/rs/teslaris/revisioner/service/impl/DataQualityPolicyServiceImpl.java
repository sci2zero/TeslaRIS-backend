package rs.teslaris.revisioner.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PolicyValidationException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.revisioner.dto.PolicyFileSummaryDTO;
import rs.teslaris.revisioner.dto.PolicyUploadResultDTO;
import rs.teslaris.revisioner.model.qualityassessment.PolicyUploadMode;
import rs.teslaris.revisioner.service.interfaces.DataQualityPolicyService;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.DataQualityProfileValidator;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityPolicyServiceImpl implements DataQualityPolicyService {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    private static final String VALIDATION_FAILED_MESSAGE = "policyValidationFailedMessage";

    private final LanguageTagService languageTagService;


    @Override
    @Transactional(readOnly = true)
    public List<PolicyFileSummaryDTO> listPolicies() {
        return DataQualityAssessmentConfigurationLoader.listAvailableProfiles()
            .stream()
            .sorted()
            .map(profileName -> {
                var latestVersion =
                    DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(profileName);

                return new PolicyFileSummaryDTO(
                    profileName,
                    StringUtil.buildMultilingualContentDTO(languageTagService,
                        DataQualityAssessmentConfigurationLoader.getProfileName(profileName,
                            latestVersion)),
                    DataQualityAssessmentConfigurationLoader.listProfileVersions(profileName),
                    latestVersion);
            })
            .toList();
    }

    @Override
    public InputStreamResource downloadPolicyVersion(String profileName, String version) {
        var path = assertVersionExists(profileName, version);

        try {
            return new InputStreamResource(new ByteArrayInputStream(Files.readAllBytes(path)));
        } catch (IOException e) {
            throw new StorageException(
                "Failed to read policy " + profileName + " " + version + ". Reason: " +
                    e.getMessage());
        }
    }

    @Override
    public PolicyUploadResultDTO uploadPolicy(String profileName, PolicyUploadMode mode,
                                              MultipartFile file) {
        assertProfileExists(profileName);

        var content = readContent(file);
        var outcome = DataQualityProfileValidator.validate(content);

        if (!outcome.valid()) {
            throw new PolicyValidationException(VALIDATION_FAILED_MESSAGE, outcome.violations());
        }

        var latestVersion = resolveLatestVersion(profileName);
        var targetVersion = PolicyUploadMode.OVERWRITE_CURRENT.equals(mode)
            ? latestVersion
            : nextVersion(profileName, latestVersion);

        write(profileName, targetVersion, content,
            PolicyUploadMode.OVERWRITE_CURRENT.equals(mode));

        log.info("Policy '{}' version '{}' written in {} mode", profileName, targetVersion, mode);

        return new PolicyUploadResultDTO(profileName.toLowerCase(), targetVersion, mode,
            PolicyUploadMode.NEW_VERSION.equals(mode), outcome.warnings());
    }

    private String readContent(MultipartFile file) {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new PolicyValidationException(VALIDATION_FAILED_MESSAGE,
                Map.of("file", "emptyPolicyFileMessage"));
        }

        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new PolicyValidationException(VALIDATION_FAILED_MESSAGE,
                Map.of("file", "policyFileTooLargeMessage"));
        }

        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PolicyValidationException(VALIDATION_FAILED_MESSAGE,
                Map.of("file", "unreadablePolicyFileMessage"));
        }
    }

    /**
     * The uploaded name carries no information that is trusted, thus the path is built from the
     * profile and the resolved version alone.
     */
    private void write(String profileName, String version, String content, boolean overwrite) {
        var targetPath = profilePath(profileName, version);
        var previousContent = overwrite ? readExisting(targetPath) : null;

        try {
            Files.createDirectories(targetPath.toAbsolutePath().getParent());

            var temporaryPath = Path.of(targetPath + ".upload");
            Files.writeString(temporaryPath, content, StandardCharsets.UTF_8);
            Files.move(temporaryPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to write policy " + profileName + " " + version + ". Reason: " +
                    e.getMessage());
        }

        try {
            DataQualityAssessmentConfigurationLoader.reloadConfiguration();
        } catch (RuntimeException e) {
            restore(targetPath, previousContent);
            throw e;
        }
    }

    private void restore(Path targetPath, String previousContent) {
        try {
            if (Objects.isNull(previousContent)) {
                Files.deleteIfExists(targetPath);
            } else {
                Files.writeString(targetPath, previousContent, StandardCharsets.UTF_8);
            }

            DataQualityAssessmentConfigurationLoader.reloadConfiguration();
        } catch (IOException | RuntimeException e) {
            log.error("Failed to restore policy file {} after a failed reload", targetPath, e);
        }
    }

    private String readExisting(Path targetPath) {
        try {
            return Files.exists(targetPath)
                ? Files.readString(targetPath, StandardCharsets.UTF_8)
                : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Increments the last numeric segment, which the version comparator orders numerically.
     */
    private String nextVersion(String profileName, String latestVersion) {
        var separatorIndex = latestVersion.lastIndexOf('.');
        var prefix = separatorIndex == -1 ? "" : latestVersion.substring(0, separatorIndex + 1);
        var lastSegment = latestVersion.substring(separatorIndex + 1);

        if (!lastSegment.chars().allMatch(Character::isDigit) || lastSegment.isEmpty() ||
            lastSegment.length() > 9) {
            throw new PolicyValidationException(VALIDATION_FAILED_MESSAGE,
                Map.of("version", "cannotDerivePolicyVersionMessage"));
        }

        var existingVersions =
            DataQualityAssessmentConfigurationLoader.listProfileVersions(profileName);
        var next = Integer.parseInt(lastSegment);

        String candidate;
        do {
            next++;
            candidate = prefix + next;
        } while (existingVersions.contains(candidate));

        return candidate;
    }

    private String resolveLatestVersion(String profileName) {
        var latestVersion =
            DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(profileName);

        if (Objects.isNull(latestVersion)) {
            throw new NotFoundException("Policy " + profileName + " has no version.");
        }

        return latestVersion;
    }

    private void assertProfileExists(String profileName) {
        if (Objects.isNull(profileName) ||
            !DataQualityAssessmentConfigurationLoader.listAvailableProfiles()
                .contains(profileName.toLowerCase())) {
            throw new NotFoundException("Policy " + profileName + " does not exist.");
        }
    }

    private Path assertVersionExists(String profileName, String version) {
        assertProfileExists(profileName);

        if (!DataQualityAssessmentConfigurationLoader.listProfileVersions(profileName)
            .contains(version)) {
            throw new NotFoundException(
                "Policy " + profileName + " has no version " + version + ".");
        }

        return profilePath(profileName, version);
    }

    private Path profilePath(String profileName, String version) {
        return Path.of(DataQualityAssessmentConfigurationLoader.getConfigurationDirectory(),
            profileName.toLowerCase(), version + ".json");
    }
}
