package rs.teslaris.exporter.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
