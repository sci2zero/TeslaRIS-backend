package rs.teslaris.revisioner.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.RevisionRestoreException;
import rs.teslaris.core.util.restoration.DegradedReference;
import rs.teslaris.core.util.restoration.RestorationContext;
import rs.teslaris.revisioner.converter.RevisionConverter;
import rs.teslaris.revisioner.dto.RevisionDTO;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.RevisionCreateEvent;
import rs.teslaris.revisioner.model.RevisionType;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.restorer.RevisionRestorer;
import rs.teslaris.revisioner.service.interfaces.RevisionService;
import rs.teslaris.revisioner.util.CompressionUtil;
import rs.teslaris.revisioner.util.ObjectMapperProvider;
import rs.teslaris.revisioner.util.RevisionConfigurationLoader;
import rs.teslaris.revisioner.util.RevisionHydratorRegistry;
import rs.teslaris.revisioner.util.RevisionRestorerRegistry;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevisionServiceImpl implements RevisionService {

    private final EntityRevisionRepository revisionRepository;

    private final RevisionHydratorRegistry revisionHydratorRegistry;

    private final RevisionRestorerRegistry revisionRestorerRegistry;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final ObjectMapper objectMapper = ObjectMapperProvider.provideObjectmapper();


    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void createRevisionIfChanged(RevisionCreateEvent event) {
        if (event.duringRestoration()) {
            // The restore records its own revision with the state the entity actually reached.
            return;
        }

        try {
            var newJson = canonicalize(
                objectMapper.writeValueAsString(event.newObject()),
                event.entityType());

            var newHash = sha256(newJson);

            var latestRevision =
                revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
                    event.entityType(), event.entityId());

            if (event.revisionType().equals(RevisionType.UPDATE)) {
                var oldJson = canonicalize(
                    objectMapper.writeValueAsString(event.oldObject()),
                    event.entityType());

                newJson = normalizeIds(oldJson, newJson);

                var oldHash = sha256(oldJson);
                newHash = sha256(newJson);

                if (latestRevision.isPresent() && oldHash.equals(newHash)) {
                    return;
                }
            }

            if (latestRevision.isPresent() &&
                latestRevision.get().getContentHash().equals(newHash)) {
                // Nothing changed relative to what is already the newest recorded state.
                return;
            }

            var revision =
                EntityRevision.builder()
                    .majorVersion(1)
                    .minorVersion(0)
                    .entityType(event.entityType())
                    .entityId(event.entityId())
                    .revisionTimestamp(Instant.now())
                    .contentHash(newHash)
                    .compressedContent(CompressionUtil.compress(newJson))
                    .build();

            revision.setAdminNote(getAdminNote(event.revisionType()));

            latestRevision.ifPresent(latest -> {
                    revision.setMajorVersion(
                        latest.getMajorVersion() +
                            (event.revisionType().equals(RevisionType.ENRICHMENT) ? 1 : 0));
                    revision.setMinorVersion(latest.getMinorVersion() + 1);
                }
            );

            applicationEventPublisher.publishEvent(
                new DataQualityAssessmentEvent(revision, newJson));

            revisionRepository.save(revision);

            log.info(
                "Created {} revision for entity '{}' (ID={}), revisionId={}, hash={}.",
                event.revisionType(),
                event.entityType(),
                event.entityId(),
                revision.getId(),
                newHash
            );
        } catch (Exception e) {
            log.error(
                "Failed to create {} revision for entity '{}' (ID={}).",
                event.revisionType(),
                event.entityType(),
                event.entityId(),
                e
            );

            throw new IllegalStateException(
                String.format(
                    "Unable to create %s revision for %s with ID %d.",
                    event.revisionType(),
                    event.entityType(),
                    event.entityId()
                ),
                e
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createRevisionFromCurrentState(String entityType, Integer entityId,
                                                  String profileName) {
        if (revisionRepository
            .findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId)
            .isPresent()) {
            return false;
        }

        var restorer = revisionRestorerRegistry.get(entityType);

        if (restorer.isEmpty()) {
            log.warn("No revision restorer registered for entity type '{}', unable to capture " +
                "current state of entity with ID {}.", entityType, entityId);
            return false;
        }

        Object currentState;

        try {
            currentState = restorer.get().readCurrentState(entityId);
        } catch (Exception e) {
            log.warn("Unable to read current state of entity '{}' (ID={}). Reason: {}",
                entityType, entityId, e.getMessage());
            return false;
        }

        if (Objects.isNull(currentState)) {
            return false;
        }

        try {
            var json = canonicalize(objectMapper.writeValueAsString(currentState), entityType);

            var revision =
                EntityRevision.builder()
                    .majorVersion(1)
                    .minorVersion(0)
                    .entityType(entityType)
                    .entityId(entityId)
                    .revisionTimestamp(Instant.now())
                    .contentHash(sha256(json))
                    .compressedContent(CompressionUtil.compress(json))
                    .build();

            revision.setAdminNote("revisionBackfill");

            revisionRepository.save(revision);

            applicationEventPublisher.publishEvent(
                new DataQualityAssessmentEvent(revision, json, profileName));

            log.info("Captured current state of entity '{}' (ID={}) as revision 1.0.",
                entityType, entityId);

            return true;
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize current state of entity '{}' (ID={}).",
                entityType, entityId, e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevisionDTO> getRevisions(String entityType, Integer entityId) {
        return revisionRepository
            .findByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId)
            .stream()
            .map(RevisionConverter::toDTO)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getRevisionAtTimestamp(String entityType, Integer entityId,
                                                   Instant timestamp) {
        Class<?> dtoClass =
            revisionHydratorRegistry.getDtoClass(entityType);

        return revisionRepository
            .findTopByEntityTypeAndEntityIdAndRevisionTimestampLessThanEqualOrderByRevisionTimestampDesc(
                entityType,
                entityId,
                timestamp
            )
            .map(r -> CompressionUtil.decompress(r.getCompressedContent()))
            .map(json -> {
                try {
                    var tree = objectMapper.readTree(json);

                    var renames = RevisionConfigurationLoader.getMigrationMappings(entityType);

                    applyFieldRenames(tree, renames);

                    Object dto = objectMapper.treeToValue(tree, dtoClass);

                    revisionHydratorRegistry
                        .get(entityType)
                        .ifPresent(h -> hydrateUnchecked(h, dto));

                    return objectMapper.writeValueAsString(dto);
                } catch (Exception e) {
                    throw new LoadingException(
                        "Unable to load revisions at given timestamp. Reason: " + e.getMessage());
                }
            });
    }

    @Override
    @Transactional
    public void restoreRevision(String entityType, Integer entityId, Integer majorVersion,
                                Integer minorVersion) {
        var restorer = revisionRestorerRegistry
            .get(entityType)
            .orElseThrow(() -> new RevisionRestoreException(
                String.format("Restoring revisions of type %s is not supported.", entityType)));

        var revision = revisionRepository
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                entityType, entityId, majorVersion, minorVersion)
            .orElseThrow(() -> new NotFoundException(
                String.format("Revision %d.%d of %s with ID %d does not exist.",
                    majorVersion, minorVersion, entityType, entityId)));

        var isLatestRevision = revisionRepository
            .findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId)
            .map(latestRevision ->
                Objects.equals(latestRevision.getMajorVersion(), majorVersion) &&
                    Objects.equals(latestRevision.getMinorVersion(), minorVersion))
            .orElse(false);

        if (isLatestRevision) {
            log.info("Revision {}.{} of entity '{}' (ID={}) is already the current state, " +
                "skipping restore.", majorVersion, minorVersion, entityType, entityId);
            return;
        }

        var json = CompressionUtil.decompress(revision.getCompressedContent());

        try {
            var tree = objectMapper.readTree(json);

            applyFieldRenames(tree, RevisionConfigurationLoader.getMigrationMappings(entityType));

            var dto = objectMapper.treeToValue(tree, restorer.dtoClass());

            // References deleted since this state was captured are dropped or degraded rather than
            // failing the restore. Whatever had to give way is collected here and stored with the
            // revision that records the restore.
            var degradedReferences = RestorationContext.collectDuring(() -> {
                restoreUnchecked(restorer, entityId, dto);
                return null;
            });

            recordRestoredRevision(entityType, entityId, revision, json, restorer,
                degradedReferences);
        } catch (Exception e) {
            log.error("Failed to restore revision {}.{} of entity '{}' (ID={}).",
                majorVersion, minorVersion, entityType, entityId, e);

            if (e instanceof RevisionRestoreException) {
                throw (RevisionRestoreException) e;
            }

            throw new RevisionRestoreException(
                String.format("Unable to restore revision %d.%d. Reason: %s",
                    majorVersion, minorVersion, e.getMessage()));
        }

        log.info("Restored entity '{}' (ID={}) to revision {}.{}.",
            entityType, entityId, majorVersion, minorVersion);
    }

    private void recordRestoredRevision(String entityType, Integer entityId,
                                        EntityRevision restoredRevision, String restoredJson,
                                        RevisionRestorer<?> restorer,
                                        List<DegradedReference> degradedReferences)
        throws JsonProcessingException {

        var latestRevision = revisionRepository
            .findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId)
            .orElse(restoredRevision);

        var sameMajorLine = Objects.equals(
            restoredRevision.getMajorVersion(), latestRevision.getMajorVersion());

        var achievedJson = readAchievedState(entityType, entityId, restorer, restoredJson);

        var revision =
            EntityRevision.builder()
                .majorVersion(latestRevision.getMajorVersion() + (sameMajorLine ? 0 : 1))
                .minorVersion(sameMajorLine ? latestRevision.getMinorVersion() + 1 : 0)
                .entityType(entityType)
                .entityId(entityId)
                .revisionTimestamp(Instant.now())
                .contentHash(sha256(achievedJson))
                .compressedContent(CompressionUtil.compress(achievedJson))
                .restorationWarnings(new ArrayList<>(degradedReferences))
                .build();

        revision.setAdminNote(
            "revisionRestore:" +
                restoredRevision.getMajorVersion() + "." +
                restoredRevision.getMinorVersion()
        );

        revisionRepository.save(revision);

        if (!degradedReferences.isEmpty()) {
            log.warn("Restore of entity '{}' (ID={}) to revision {}.{} degraded {} reference(s).",
                entityType, entityId, restoredRevision.getMajorVersion(),
                restoredRevision.getMinorVersion(), degradedReferences.size());
        }

        applicationEventPublisher.publishEvent(
            new DataQualityAssessmentEvent(revision, achievedJson));

        log.info("Recorded restored state of entity '{}' (ID={}) as revision {}.{}.",
            entityType, entityId, revision.getMajorVersion(), revision.getMinorVersion());
    }

    /**
     * The state the entity actually reached, which is not necessarily the state that was asked for.
     */
    private String readAchievedState(String entityType, Integer entityId,
                                     RevisionRestorer<?> restorer, String restoredJson)
        throws JsonProcessingException {

        Object currentState;

        try {
            currentState = restorer.readCurrentState(entityId);
        } catch (Exception e) {
            log.warn("Unable to read back restored entity '{}' (ID={}), recording the requested " +
                "state instead. Reason: {}", entityType, entityId, e.getMessage());
            return restoredJson;
        }

        if (Objects.isNull(currentState)) {
            return restoredJson;
        }

        return canonicalize(objectMapper.writeValueAsString(currentState), entityType);
    }

    private String canonicalize(String json, String entityType)
        throws JsonProcessingException {

        var tree = normalize(objectMapper.readTree(json));

        removeIgnoredFields(tree,
            RevisionConfigurationLoader.listExcludedFieldsForType(entityType));

        return objectMapper.writeValueAsString(tree);
    }

    private String normalizeIds(String oldJson, String newJson) throws JsonProcessingException {
        JsonNode oldNode = objectMapper.readTree(oldJson);
        JsonNode newNode = objectMapper.readTree(newJson);

        if (oldNode instanceof ObjectNode oldObject &&
            newNode instanceof ObjectNode newObject &&
            oldObject.has("id")) {

            newObject.set("id", oldObject.get("id"));

            return objectMapper.writeValueAsString(newObject);
        }

        return newJson;
    }

    private String sha256(String value) {
        return DigestUtils.sha256Hex(value);
    }

    private JsonNode normalize(JsonNode node) {
        if (Objects.isNull(node)) {
            return NullNode.instance;
        }

        if (node.isTextual() && node.textValue().isBlank()) {
            return NullNode.instance;
        }

        if (node.isObject()) {
            var result = objectMapper.createObjectNode();

            node.fields().forEachRemaining(entry ->
                result.set(entry.getKey(), normalize(entry.getValue()))
            );

            return result;
        }

        if (node.isArray()) {
            var result = objectMapper.createArrayNode();

            node.forEach(child ->
                result.add(normalize(child))
            );

            return result;
        }

        return node;
    }

    private void removeIgnoredFields(JsonNode node, Set<String> ignoredFields) {
        if (ignoredFields.isEmpty()) {
            return;
        }

        if (node instanceof ObjectNode objectNode) {
            ignoredFields.forEach(objectNode::remove);

            objectNode.fields()
                .forEachRemaining(entry ->
                    removeIgnoredFields(entry.getValue(), ignoredFields)
                );
        }

        if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(child ->
                removeIgnoredFields(child, ignoredFields)
            );
        }
    }

    private void applyFieldRenames(JsonNode node, Map<String, String> renames) {
        if (node instanceof ObjectNode objectNode) {
            renames.forEach((oldName, newName) -> {
                var value = objectNode.remove(oldName);

                if (Objects.nonNull(value)) {
                    objectNode.set(newName, value);
                }
            });

            objectNode.fields().forEachRemaining(entry ->
                applyFieldRenames(entry.getValue(), renames)
            );
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(child ->
                applyFieldRenames(child, renames)
            );
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void hydrateUnchecked(RevisionHydrator<?> hydrator, Object dto) {
        ((RevisionHydrator<T>) hydrator).hydrate((T) dto);
    }

    @SuppressWarnings("unchecked")
    private <T> void restoreUnchecked(RevisionRestorer<?> restorer, Integer entityId, Object dto) {
        ((RevisionRestorer<T>) restorer).restore(entityId, (T) dto);
    }

    private String getAdminNote(RevisionType revisionType) {
        if (revisionType.equals(RevisionType.CREATE)) {
            return "revisionCreate";
        } else if (revisionType.equals(RevisionType.UPDATE)) {
            return "revisionUpdate";
        } else if (revisionType.equals(RevisionType.ENRICHMENT)) {
            return "revisionEnrichment";
        }

        return "";
    }
}
