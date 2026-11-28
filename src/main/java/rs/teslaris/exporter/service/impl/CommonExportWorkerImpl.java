package rs.teslaris.exporter.service.impl;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.model.common.ExportPublicationType;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonExportWorkerImpl {

    private final MongoTemplate mongoTemplate;

    /**
     * Until when (epoch millis) export record deletions are skipped because Mongo was found
     * unreachable. Without it, every visibility change waits out the full server selection timeout
     * and logs the same failure again.
     */
    private final AtomicLong mongoUnavailableUntil = new AtomicLong(0);

    @Value("${mongo.unavailable-retry-after-seconds:300}")
    private long mongoUnavailableRetryAfterSeconds;


    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public <T, E> void exportEntities(
        BiFunction<Pageable, Boolean, Page<T>> repositoryFunction,
        BiFunction<T, Boolean, E> converter,
        Class<E> exportClass,
        Function<T, Integer> idGetter,
        boolean allTime,
        ExportPublicationType exportPublicationType
    ) {
        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk =
                repositoryFunction.apply(PageRequest.of(pageNumber, chunkSize), allTime)
                    .getContent();

            for (T entity : chunk) {
                var query = new Query();
                query.addCriteria(Criteria.where("database_id").is(idGetter.apply(entity)));

                if (Objects.nonNull(exportPublicationType)) {
                    query.addCriteria(Criteria.where("type").is(exportPublicationType.name()));
                }

                query.limit(1);

                var exportEntry = converter.apply(entity, true);

                mongoTemplate.remove(query, exportClass);
                mongoTemplate.save(exportEntry);
            }

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    /**
     * Marks the export record of a record that is no longer public as deleted, instead of
     * removing it, so the push and OAI-PMH both learn that it is gone. The new timestamp makes
     * the next push send the deletion.
     */
    public <E> long markExportEntityDeleted(Integer databaseId, Class<E> exportClass,
                                            ExportPublicationType exportPublicationType) {
        var query = new Query();
        query.addCriteria(Criteria.where("database_id").is(databaseId));

        if (Objects.nonNull(exportPublicationType)) {
            query.addCriteria(Criteria.where("type").is(exportPublicationType.name()));
        }

        var unavailableUntil = mongoUnavailableUntil.get();
        if (System.currentTimeMillis() < unavailableUntil) {
            log.debug("Mongo is unreachable, skipping deletion of {} export record with ID {}.",
                exportClass.getSimpleName(), databaseId);
            return 0;
        }

        var update = new Update().set("deleted", true).set("last_updated", new Date());

        try {
            return mongoTemplate.updateMulti(query, update, exportClass).getModifiedCount();
        } catch (DataAccessResourceFailureException e) {
            var retryAt = System.currentTimeMillis() +
                Duration.ofSeconds(mongoUnavailableRetryAfterSeconds).toMillis();

            if (mongoUnavailableUntil.compareAndSet(unavailableUntil, retryAt)) {
                log.warn(
                    "Mongo is unreachable, export record deletions are skipped for the next {} " +
                        "seconds. Reason: {}", mongoUnavailableRetryAfterSeconds, e.getMessage());
            }

            return 0;
        }
    }

    /**
     * Tells whether the export record is a tombstone. Empty when there is no export record, or
     * when Mongo is unreachable.
     */
    public <E extends BaseExportEntity> Optional<Boolean> isExportEntityDeleted(
        Integer databaseId, Class<E> exportClass) {
        if (System.currentTimeMillis() < mongoUnavailableUntil.get()) {
            log.debug("Mongo is unreachable, skipping lookup of {} export record with ID {}.",
                exportClass.getSimpleName(), databaseId);
            return Optional.empty();
        }

        var query = new Query();
        query.addCriteria(Criteria.where("database_id").is(databaseId));

        try {
            return Optional.ofNullable(mongoTemplate.findOne(query, exportClass))
                .map(exportEntity -> Boolean.TRUE.equals(exportEntity.getDeleted()));
        } catch (DataAccessResourceFailureException e) {
            log.warn("Unable to read {} export record with ID {}. Reason: {}",
                exportClass.getSimpleName(), databaseId, e.getMessage());
            return Optional.empty();
        }
    }

    public <E> long deleteExportEntity(Integer databaseId, Class<E> exportClass,
                                       ExportPublicationType exportPublicationType) {
        var query = new Query();
        query.addCriteria(Criteria.where("database_id").is(databaseId));

        if (Objects.nonNull(exportPublicationType)) {
            query.addCriteria(Criteria.where("type").is(exportPublicationType.name()));
        }

        var unavailableUntil = mongoUnavailableUntil.get();
        if (System.currentTimeMillis() < unavailableUntil) {
            log.debug("Mongo is unreachable, skipping deletion of {} export record with ID {}.",
                exportClass.getSimpleName(), databaseId);
            return 0;
        }

        try {
            return mongoTemplate.remove(query, exportClass).getDeletedCount();
        } catch (DataAccessResourceFailureException e) {
            var retryAt = System.currentTimeMillis() +
                Duration.ofSeconds(mongoUnavailableRetryAfterSeconds).toMillis();

            // Concurrent callers that timed out together report it only once.
            if (mongoUnavailableUntil.compareAndSet(unavailableUntil, retryAt)) {
                log.warn(
                    "Mongo is unreachable, export record deletions are skipped for the next {} " +
                        "seconds. Reason: {}", mongoUnavailableRetryAfterSeconds, e.getMessage());
            }

            return 0;
        }
    }
}
