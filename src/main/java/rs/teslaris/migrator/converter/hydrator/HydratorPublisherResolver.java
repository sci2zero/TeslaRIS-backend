package rs.teslaris.migrator.converter.hydrator;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PublisherBasicAdditionDTO;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.migrator.model.MigrationItemStatus;
import rs.teslaris.migrator.model.MigrationRecordLog;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * CW-001-MAP-000157: a book's publisher is only named in the curriculum, so it is created on
 * demand as a stub, matched by its name.
 */
@Component
@RequiredArgsConstructor
public class HydratorPublisherResolver implements RunScopedCache {

    private final PublisherService publisherService;

    private final CountryService countryService;

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;

    private final MigrationRecordLogRepository recordLogRepository;

    private final Map<String, Integer> publisherIds = new ConcurrentHashMap<>();


    public Integer resolveOrCreate(String name, String language,
                                   HydratorCVModel.PublicationLocation location) {
        if (Objects.isNull(name) || name.isBlank()) {
            return null;
        }

        return publisherIds.computeIfAbsent("name:" + conversionUtil.normalise(name),
            key -> recordLogRepository.findTargetEntityId(HydratorSource.NAME,
                    MigrationEntityType.PUBLISHER, key)
                .orElseGet(() -> create(key, name.trim(), language, location)));
    }

    @Override
    public void clearCache() {
        publisherIds.clear();
    }

    private Integer create(String key, String name, String language,
                           HydratorCVModel.PublicationLocation location) {
        var dto = new PublisherBasicAdditionDTO();
        dto.setName(conversionUtil.multilingualContent(name, language));

        if (Objects.nonNull(location) && Objects.nonNull(location.country()) &&
            Objects.nonNull(location.country().code()) && !location.country().code().isBlank()) {
            countryService.findCountryByCode(location.country().code().trim())
                .ifPresent(country -> dto.setCountryId(country.getId()));
        }

        var created = publisherService.createPublisher(dto);
        migrationLog.stubCreated(HydratorSource.NAME, MigrationEntityType.PUBLISHER.name(), key,
            created.getId(), name);
        recordLogRepository.record(MigrationRecordLog.builder()
            .source(HydratorSource.NAME)
            .entityType(MigrationEntityType.PUBLISHER)
            .sourceKey(key)
            .status(MigrationItemStatus.CREATED)
            .targetEntityId(created.getId())
            .processedAt(Instant.now())
            .build());

        return created.getId();
    }
}
