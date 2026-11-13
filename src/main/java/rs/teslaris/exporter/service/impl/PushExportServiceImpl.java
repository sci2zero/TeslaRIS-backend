package rs.teslaris.exporter.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import rs.teslaris.exporter.model.push.PushedSharedNodeState;
import rs.teslaris.exporter.service.interfaces.PushExportService;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.NTriplesSerializer;
import rs.teslaris.exporter.util.push.RdfMapper;
import rs.teslaris.exporter.util.push.RdfMapperRegistry;
import rs.teslaris.exporter.util.push.SharedNode;
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

        // Shared nodes that the new triples link to, and URIs that entities stopped linking to.
        var sharedNodes = new LinkedHashMap<String, SharedNode>();
        var droppedDependencies = new LinkedHashSet<String>();

        for (var snapshot : snapshots) {
            var state = states.get(snapshot.getDatabaseId());

            if (!force && Objects.nonNull(state) && Objects.nonNull(snapshot.getLastUpdated()) &&
                snapshot.getLastUpdated().equals(state.getSnapshotLastUpdated())) {
                continue;
            }

            if (Boolean.TRUE.equals(snapshot.getDeleted())) {
                if (Objects.nonNull(state)) {
                    toDelete.addAll(state.getTriples());
                    droppedDependencies.addAll(dependenciesOf(state));
                    statesToRemove.add(state);
                }
                continue;
            }

            var mapping = mapper.map(snapshot, target);
            var triples = NTriplesSerializer.toLines(mapping.model());
            var oldTriples = Objects.nonNull(state) ?
                new HashSet<>(state.getTriples()) : new HashSet<String>();
            var newTriples = new HashSet<>(triples);

            oldTriples.stream().filter(triple -> !newTriples.contains(triple))
                .forEach(toDelete::add);
            triples.stream().filter(triple -> !oldTriples.contains(triple))
                .forEach(toInsert::add);

            mapping.sharedNodes().forEach(node -> sharedNodes.putIfAbsent(node.uri(), node));
            var dependencies = mapping.sharedNodes().stream().map(SharedNode::uri)
                .distinct().sorted().toList();
            if (Objects.nonNull(state)) {
                dependenciesOf(state).stream().filter(uri -> !dependencies.contains(uri))
                    .forEach(droppedDependencies::add);
            }

            if (Objects.isNull(state)) {
                state = new PushedEntityState(target.identifier(), entityType,
                    snapshot.getDatabaseId());
            }

            state.setSnapshotLastUpdated(snapshot.getLastUpdated());
            state.setTriples(triples);
            state.setDependencies(new ArrayList<>(dependencies));
            state.setPushedAt(new Date());
            statesToSave.add(state);
        }

        droppedDependencies.removeAll(sharedNodes.keySet());

        var changedIds = new ArrayList<Integer>();
        statesToSave.forEach(state -> changedIds.add(state.getDatabaseId()));
        statesToRemove.forEach(state -> changedIds.add(state.getDatabaseId()));

        var sharedStates = findSharedStates(target,
            Stream.concat(sharedNodes.keySet().stream(), droppedDependencies.stream()).toList());
        var sharedStatesToSave = new ArrayList<PushedSharedNodeState>();
        var sharedStatesToRemove = new ArrayList<PushedSharedNodeState>();

        sharedNodes.values().forEach(node -> {
            var sharedState = sharedStates.get(node.uri());
            // The first entity that links to a node creates it. Later entities with the same
            // node do not change it. A forced push replaces it with the current triples.
            if (Objects.nonNull(sharedState) && !force) {
                return;
            }

            var triples = NTriplesSerializer.toLines(node.model());
            var oldTriples = Objects.nonNull(sharedState) ?
                new HashSet<>(sharedState.getTriples()) : new HashSet<String>();
            if (oldTriples.equals(new HashSet<>(triples))) {
                return;
            }

            var newTriples = new HashSet<>(triples);
            oldTriples.stream().filter(triple -> !newTriples.contains(triple))
                .forEach(toDelete::add);
            triples.stream().filter(triple -> !oldTriples.contains(triple))
                .forEach(toInsert::add);

            if (Objects.isNull(sharedState)) {
                sharedState = new PushedSharedNodeState(target.identifier(), node.uri());
            }
            sharedState.setTriples(triples);
            sharedState.setPushedAt(new Date());
            sharedStatesToSave.add(sharedState);
        });

        droppedDependencies.forEach(uri -> {
            var sharedState = sharedStates.get(uri);
            if (Objects.nonNull(sharedState) &&
                !isLinkedByOtherEntities(target, entityType, changedIds, uri)) {
                toDelete.addAll(sharedState.getTriples());
                sharedStatesToRemove.add(sharedState);
            }
        });

        sparqlUpdateClient.deleteData(target, toDelete);
        sparqlUpdateClient.insertData(target, toInsert);

        sharedStatesToSave.forEach(mongoTemplate::save);
        statesToSave.forEach(mongoTemplate::save);
        statesToRemove.forEach(mongoTemplate::remove);
        sharedStatesToRemove.forEach(mongoTemplate::remove);

        return statesToSave.size() + statesToRemove.size();
    }

    private static List<String> dependenciesOf(PushedEntityState state) {
        return Objects.nonNull(state.getDependencies()) ? state.getDependencies() : List.of();
    }

    // The states of the changed entities are not saved yet, so they are not counted.
    private boolean isLinkedByOtherEntities(PushTarget target, PushEntityType entityType,
                                            List<Integer> changedIds, String uri) {
        var query = new Query();
        query.addCriteria(Criteria.where("target").is(target.identifier()));
        query.addCriteria(Criteria.where("dependencies").is(uri));
        query.addCriteria(new Criteria().norOperator(
            Criteria.where("entity_type").is(entityType.name()).and("database_id")
                .in(changedIds)));

        return mongoTemplate.count(query, PushedEntityState.class) > 0;
    }

    private Map<String, PushedSharedNodeState> findSharedStates(PushTarget target,
                                                                List<String> uris) {
        if (uris.isEmpty()) {
            return Map.of();
        }

        var query = new Query();
        query.addCriteria(Criteria.where("target").is(target.identifier()));
        query.addCriteria(Criteria.where("uri").in(uris));

        return mongoTemplate.find(query, PushedSharedNodeState.class).stream()
            .collect(Collectors.toMap(PushedSharedNodeState::getUri, Function.identity(),
                (first, second) -> first));
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
