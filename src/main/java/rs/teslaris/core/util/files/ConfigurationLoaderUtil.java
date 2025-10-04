package rs.teslaris.core.util.files;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

public class ConfigurationLoaderUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static synchronized <T> T loadConfiguration(
        Class<T> clazz,
        String defaultConfigurationPath,
        String overridePath
    ) throws IOException {
        T config = objectMapper.readValue(
            new FileInputStream(defaultConfigurationPath),
            clazz
        );

        // Checking for external override
        if (Objects.isNull(overridePath) || overridePath.isBlank()) {
            return config;
        }

        var externalFile = new File(overridePath);
        if (externalFile.exists()) {
            var updater = objectMapper.readerForUpdating(config);
            config = updater.readValue(externalFile);
        }

        return config;
    }
}
