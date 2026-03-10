package rs.teslaris.exporter.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
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
public class CommonExportWorkerImpl {

    private final MongoTemplate mongoTemplate;


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
}
