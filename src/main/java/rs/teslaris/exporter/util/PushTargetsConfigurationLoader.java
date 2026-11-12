package rs.teslaris.exporter.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;

@Component
public class PushTargetsConfigurationLoader {

    private static PushTargetsConfiguration pushTargetsConfiguration = null;

    private static String externalOverrideConfiguration;


    public PushTargetsConfigurationLoader(
        @Value("${export.push.configuration}") String externalOverrideConfiguration) {
        PushTargetsConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    protected static void reloadConfiguration() {
        try {
            pushTargetsConfiguration = ConfigurationLoaderUtil.loadConfiguration(
                PushTargetsConfiguration.class,
                "src/main/resources/export/pushTargetsConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload push targets configuration: " + e.getMessage());
        }
    }

    public static Optional<PushTarget> getTargetByIdentifier(String identifier) {
        if (Objects.isNull(pushTargetsConfiguration)) {
            reloadConfiguration();
        }

        return pushTargetsConfiguration.targets().stream()
            .filter(target -> target.identifier().equals(identifier))
            .findFirst();
    }

    public enum AuthType {
        VIVO_FORM,
        BASIC,
        NONE
    }

    public record PushTargetsConfiguration(
        @JsonProperty(value = "targets", required = true) List<PushTarget> targets
    ) {
    }

    public record PushTarget(
        @JsonProperty(value = "identifier", required = true) String identifier,
        @JsonProperty(value = "profile", required = true) String profile,
        @JsonProperty(value = "enabled", required = true) boolean enabled,
        @JsonProperty(value = "endpointUrl", required = true) String endpointUrl,
        @JsonProperty(value = "graph") String graph,
        @JsonProperty(value = "namespace", required = true) String namespace,
        @JsonProperty(value = "auth", required = true) Auth auth,
        @JsonProperty(value = "batchSize", required = true) int batchSize,
        @JsonProperty(value = "requestTimeoutSeconds") Integer requestTimeoutSeconds,
        @JsonProperty(value = "supportedLanguages") List<String> supportedLanguages,
        @JsonProperty(value = "entityTypes", required = true) List<String> entityTypes
    ) {
    }

    public record Auth(
        @JsonProperty(value = "type", required = true) AuthType type,
        @JsonProperty(value = "username") String username,
        @JsonProperty(value = "passwordEnv") String passwordEnv
    ) {
    }
}
