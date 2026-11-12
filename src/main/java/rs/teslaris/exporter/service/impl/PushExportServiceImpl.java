package rs.teslaris.exporter.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.model.push.PushedEntityState;
import rs.teslaris.exporter.service.interfaces.PushExportService;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.NTriplesSerializer;
import rs.teslaris.exporter.util.push.RdfMapper;
import rs.teslaris.exporter.util.push.RdfMapperRegistry;
import rs.teslaris.exporter.util.push.SparqlUpdateClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushExportServiceImpl implements PushExportService {

    private static final int PAGE_SIZE = 100;

    private final MongoTemplate mongoTemplate;

    private final RdfMapperRegistry rdfMapperRegistry;

    private final SparqlUpdateClient sparqlUpdateClient;

    private final Map<String, ReentrantLock> targetLocks = new ConcurrentHashMap<>();


    @Override
    @Async("taskExecutor")
    public void pushEntities(PushTarget target, PushEntityType entityType, boolean force) {
        var lock = targetLocks.computeIfAbsent(target.identifier(), id -> new ReentrantLock());
        if (!lock.tryLock()) {
            log.info("Push to {} already in progress, skipping this run.", target.identifier());
            return;
        }

        try {
            pushAll(target, entityType, entityType.getSnapshotType(), force);
        } catch (Exception e) {
            log.error("Push of {} to {} not completed due to an error.", entityType.getCode(),
                target.identifier(), e);
        } finally {
            lock.unlock();
        }
    }

    private <T extends BaseExportEntity> void pushAll(PushTarget target,
                                                      PushEntityType entityType,
                                                      Class<T> snapshotType, boolean force) {
        var mapper = rdfMapperRegistry.find(target.profile(), snapshotType)
            .orElseThrow(() -> new IllegalStateException(
                "No RDF mapper for profile '" + target.profile() + "' and type " +
                    entityType.getCode()));

        int pageNumber = 0;
        int changed = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            var query = new Query().with(
                PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "databaseId")));
            var snapshots = mongoTemplate.find(query, snapshotType);

            changed += pushPage(target, entityType, mapper, snapshots, force);

            pageNumber++;
            hasNextPage = snapshots.size() == PAGE_SIZE;
        }

        log.info("Push of {} to {} completed, {} entities changed.", entityType.getCode(),
            target.identifier(), changed);
    }

    private <T extends BaseExportEntity> int pushPage(PushTarget target,
                                                      PushEntityType entityType,
                                                      RdfMapper<T> mapper, List<T> snapshots,
                                                      boolean force) {
        if (snapshots.isEmpty()) {
            return 0;
        }

        var states = findStates(target, entityType,
            snapshots.stream().map(BaseExportEntity::getDatabaseId).toList());

        var toDelete = new LinkedHashSet<String>();
        var toInsert = new LinkedHashSet<String>();
        var statesToSave = new ArrayList<PushedEntityState>();
        var statesToRemove = new ArrayList<PushedEntityState>();

        for (var snapshot : snapshots) {
            var state = states.get(snapshot.getDatabaseId());

            if (!force && Objects.nonNull(state) && Objects.nonNull(snapshot.getLastUpdated()) &&
                snapshot.getLastUpdated().equals(state.getSnapshotLastUpdated())) {
                continue;
            }

            if (Boolean.TRUE.equals(snapshot.getDeleted())) {
                if (Objects.nonNull(state)) {
                    toDelete.addAll(state.getTriples());
                    statesToRemove.add(state);
                }
                continue;
            }

            var triples = NTriplesSerializer.toLines(mapper.toRdf(snapshot, target));
            var oldTriples = Objects.nonNull(state) ?
                new HashSet<>(state.getTriples()) : new HashSet<String>();
            var newTriples = new HashSet<>(triples);

            oldTriples.stream().filter(triple -> !newTriples.contains(triple))
                .forEach(toDelete::add);
            triples.stream().filter(triple -> !oldTriples.contains(triple))
                .forEach(toInsert::add);

            if (Objects.isNull(state)) {
                state = new PushedEntityState(target.identifier(), entityType,
                    snapshot.getDatabaseId());
            }

            state.setSnapshotLastUpdated(snapshot.getLastUpdated());
            state.setTriples(triples);
            state.setPushedAt(new Date());
            statesToSave.add(state);
        }

        sparqlUpdateClient.deleteData(target, toDelete);
        sparqlUpdateClient.insertData(target, toInsert);

        statesToSave.forEach(mongoTemplate::save);
        statesToRemove.forEach(mongoTemplate::remove);

        return statesToSave.size() + statesToRemove.size();
    }

    private Map<Integer, PushedEntityState> findStates(PushTarget target,
                                                       PushEntityType entityType,
                                                       List<Integer> databaseIds) {
        var query = new Query();
        query.addCriteria(Criteria.where("target").is(target.identifier()));
        query.addCriteria(Criteria.where("entity_type").is(entityType.name()));
        query.addCriteria(Criteria.where("database_id").in(databaseIds));

        return mongoTemplate.find(query, PushedEntityState.class).stream()
            .collect(Collectors.toMap(PushedEntityState::getDatabaseId, Function.identity(),
                (first, second) -> first));
    }
}
