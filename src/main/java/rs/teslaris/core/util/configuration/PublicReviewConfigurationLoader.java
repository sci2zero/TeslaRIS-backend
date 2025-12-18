package rs.teslaris.core.util.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;

@Component
public class PublicReviewConfigurationLoader {

    private static PublicReviewConfiguration publicReviewConfiguration = null;

    private static String externalOverrideConfiguration;


    @Autowired
    public PublicReviewConfigurationLoader(@Value("${assessment.rules.configuration}")
                                           String externalOverrideConfiguration) {
        PublicReviewConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    private static void reloadConfiguration() {
        try {
            publicReviewConfiguration = ConfigurationLoaderUtil.loadConfiguration(
                PublicReviewConfiguration.class,
                "src/main/resources/thesisLibrary/publicReviewConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload public review configuration: " + e.getMessage());
        }
    }

    public static LocalDate getCutoffDate(boolean shortened) {
        var periodDescription = shortened ?
            publicReviewConfiguration.shortenedPublicReviewLength :
            publicReviewConfiguration.regularPublicReviewLength;

        return LocalDate.now().minus(periodDescription.value, periodDescription.timeUnit);
    }

    public static Integer getLengthInDays(boolean shortened) {
        var periodDescription = shortened ?
            publicReviewConfiguration.shortenedPublicReviewLength :
            publicReviewConfiguration.regularPublicReviewLength;

        return (int) Math.floor(periodDescription.toDays());
    }

    private record PublicReviewConfiguration(
        @JsonProperty(value = "regularPublicReviewLength", required = true)
        PeriodDescription regularPublicReviewLength,

        @JsonProperty(value = "shortenedPublicReviewLength", required = true)
        PeriodDescription shortenedPublicReviewLength
    ) {
    }

    private record PeriodDescription(
        @JsonProperty(value = "value", required = true) Integer value,
        @JsonProperty(value = "timeUnit", required = true) ChronoUnit timeUnit
    ) {
        public double toDays() {
            return switch (timeUnit) {
                case HOURS -> value / 24.0;
                case HALF_DAYS -> value / 2.0;
                case DAYS -> value.doubleValue();
                case WEEKS -> value * 7.0;
                case MONTHS -> value * 30.0;
                default -> 0.0;
            };
        }
    }
}

