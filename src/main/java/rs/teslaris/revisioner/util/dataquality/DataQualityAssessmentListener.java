package rs.teslaris.revisioner.util.dataquality;

import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.util.DataQualityCalculator;
import rs.teslaris.revisioner.util.ObjectMapperProvider;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataQualityAssessmentListener {

    private final DataQualityCalculator calculator;

    private final EntityRevisionRepository entityRevisionRepository;

    private final DataQualityAssessmentRepository repository;


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DataQualityAssessmentEvent event) {
        var profiles = DataQualityAssessmentConfigurationLoader.listAvailableProfiles();

        profiles.forEach(profileName -> {
            var assessment = DataQualityAssessment
                .builder()
                .revision(event.entityRevision())
                .engineVersion("1.0.0")
                .profileVersion(
                    DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(profileName))
                .profileName(profileName)
                .startedAt(Instant.now())
                .build();

            var targetType = resolveTargetType(event.entityRevision().getEntityType());

            if (Objects.isNull(targetType)) {
                log.error("Unable to find target type for {} and entity id {{}}",
                    event.entityRevision().getEntityType(), event.entityRevision().getId());
                return;
            }

            event.entityRevision().addAssessment(assessment);

            calculator.assessDataQuality(assessment, event.json(),
                ObjectMapperProvider.provideObjectmapper(), repository, targetType);

            entityRevisionRepository.save(event.entityRevision());
        });
    }

    private String resolveTargetType(String entityType) {
        try {
            return DataQualityAssessmentConfigurationLoader.getTargetTypeFromEntityType(
                EntityType.valueOf(entityType));
        } catch (IllegalArgumentException ex) {
            try {
                DocumentPublicationType.valueOf(entityType);
                return DataQualityAssessmentConfigurationLoader.getTargetTypeFromEntityType(
                    EntityType.PUBLICATION);
            } catch (IllegalArgumentException ignored) {
                throw ex;
            }
        }
    }
}
