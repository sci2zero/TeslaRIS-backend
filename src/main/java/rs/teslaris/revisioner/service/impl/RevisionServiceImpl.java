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
import java.util.function.Function;
import java.util.stream.Collectors;
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
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.revisioner.dto.QualityReportResponseDTO;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.RevisionCreateEvent;
import rs.teslaris.revisioner.model.RevisionType;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.service.interfaces.RevisionService;
import rs.teslaris.revisioner.util.CompressionUtil;
import rs.teslaris.revisioner.util.ObjectMapperProvider;
import rs.teslaris.revisioner.util.RevisionConfigurationLoader;
import rs.teslaris.revisioner.util.RevisionHydratorRegistry;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevisionServiceImpl implements RevisionService {

    private final EntityRevisionRepository repository;

    private final RevisionHydratorRegistry revisionHydratorRegistry;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final ObjectMapper objectMapper = ObjectMapperProvider.provideObjectmapper();


    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void createRevisionIfChanged(RevisionCreateEvent event) {
        try {
            var newJson = canonicalize(
                objectMapper.writeValueAsString(event.newObject()),
                event.entityType());

            var newHash = sha256(newJson);

            if (event.revisionType().equals(RevisionType.UPDATE)) {
                var oldJson = canonicalize(
                    objectMapper.writeValueAsString(event.oldObject()),
                    event.entityType());

                newJson = normalizeIds(oldJson, newJson);

                var oldHash = sha256(oldJson);
                newHash = sha256(newJson);

                if (oldHash.equals(newHash)) {
                    return;
                }
            }

            var revision =
                EntityRevision.builder()
                    .entityType(event.entityType())
                    .entityId(event.entityId())
                    .revisionTimestamp(Instant.now())
                    .contentHash(newHash)
                    .compressedContent(CompressionUtil.compress(newJson))
                    .build();

            applicationEventPublisher.publishEvent(
                new DataQualityAssessmentEvent(revision, newJson));

            repository.save(revision);

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
    @Transactional(readOnly = true)
    public List<Instant> getRevisionTimestamps(String entityType, Integer entityId) {
        return repository
            .findByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId)
            .stream()
            .map(EntityRevision::getRevisionTimestamp)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getRevisionAtTimestamp(String entityType, Integer entityId,
                                                   Instant timestamp) {
        Class<?> dtoClass =
            revisionHydratorRegistry.getDtoClass(entityType);

        return repository
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
    @Transactional(readOnly = true)
    public List<QualityReportResponseDTO> getQualityReportAtTimestamp(String entityType,
                                                                      Integer entityId,
                                                                      Instant timestamp) {
        var entityRevision = repository
            .findTopByEntityTypeAndEntityIdAndRevisionTimestampLessThanEqualOrderByRevisionTimestampDesc(
                entityType, entityId, timestamp);

        if (entityRevision.isEmpty()) {
            return List.of();
        }

        var qualityReport = new ArrayList<QualityReportResponseDTO>();

        entityRevision.get().getAssessments().forEach(assessment -> {
            var currentReportText = new ArrayList<MultilingualContentDTO>();

            assessment.getIssues().forEach(issue -> {
                var newRemarks = RevisionConfigurationLoader.getDataQualityRemark(issue.getKey(),
                    issue.getParameters().toArray());

                Map<String, MultilingualContentDTO> existingRemarks =
                    currentReportText
                        .stream()
                        .collect(Collectors.toMap(
                            MultilingualContentDTO::getLanguageTag,
                            Function.identity(),
                            (left, right) -> left));

                for (var newRemark : newRemarks) {
                    var languageTag = newRemark.getLanguage().getLanguageTag();

                    var existing = existingRemarks.get(languageTag);

                    if (Objects.isNull(existing)) {
                        var dto = new MultilingualContentDTO(
                            newRemark.getLanguage().getId(),
                            newRemark.getLanguage().getLanguageTag(),
                            newRemark.getContent(),
                            newRemark.getPriority()
                        );

                        qualityReport.add(
                            new QualityReportResponseDTO(
                                assessment.getProfileName() + " (" +
                                    assessment.getProfileVersion() + ")",
                                currentReportText
                            )
                        );
                        existingRemarks.put(languageTag, dto);
                    } else {
                        existing.setContent(
                            existing.getContent()
                                + System.lineSeparator()
                                + System.lineSeparator()
                                + newRemark.getContent()
                        );
                    }
                }
            });
        });

        return qualityReport;
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
}
