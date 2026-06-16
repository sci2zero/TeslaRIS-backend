package rs.teslaris.revisioner.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.RevisionCreateEvent;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.service.interfaces.RevisionService;
import rs.teslaris.revisioner.util.CompressionUtil;

@Service
@RequiredArgsConstructor
public class RevisionServiceImpl implements RevisionService {

    private final EntityRevisionRepository repository;

    private final ObjectMapper objectMapper =
        JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .addModule(new JavaTimeModule())
            .build();


    @Override
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public boolean createRevisionIfChanged(RevisionCreateEvent event) {
        try {
            var oldJson = canonicalize(objectMapper.writeValueAsString(event.oldObject()),
                event.entityType());
            var newJson = canonicalize(objectMapper.writeValueAsString(event.newObject()),
                event.entityType());

            var oldHash = sha256(oldJson);
            var newHash = sha256(newJson);

            if (oldHash.equals(newHash)) {
                return false;
            }

            repository.save(
                EntityRevision.builder()
                    .entityType(event.entityType())
                    .entityId(event.entityId())
                    .revisionTimestamp(Instant.now())
                    .contentHash(newHash)
                    .compressedContent(
                        CompressionUtil.compress(newJson)
                    )
                    .build()
            );

            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> getRevisionJsons(String entityType, Integer entityId) {
        return repository
            .findByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId)
            .stream()
            .map(r -> LocalDateTime.ofInstant(r.getRevisionTimestamp(), ZoneId.systemDefault())
            )
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public <T> Optional<T> getRevisionAtDate(String entityType, Integer entityId, Instant timestamp,
                                             Class<T> dtoClass) {
        return repository
            .findTopByEntityTypeAndEntityIdAndRevisionTimestampLessThanEqualOrderByRevisionTimestampDesc(
                entityType,
                entityId,
                timestamp
            ).map(r ->
                CompressionUtil.decompress(r.getCompressedContent())
            ).map(json -> {
                try {
                    return objectMapper.readValue(json, dtoClass);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
    }

    private String canonicalize(String json, String entityType)
        throws JsonProcessingException {

        var tree = normalize(objectMapper.readTree(json));

        removeIgnoredFields(tree,
            Set.of("peerReviewed", "openAccess", "publicationStatus"));

        return objectMapper.writeValueAsString(tree);
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
        if (node instanceof ObjectNode objectNode) {

            ignoredFields.forEach(objectNode::remove);

            objectNode.fields()
                .forEachRemaining(entry ->
                    removeIgnoredFields(
                        entry.getValue(),
                        ignoredFields
                    )
                );
        }

        if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(child ->
                removeIgnoredFields(
                    child,
                    ignoredFields
                )
            );
        }
    }

    private void applyFieldRenames(JsonNode node, Map<String, String> renames) {
        if (!(node instanceof ObjectNode objectNode)) {
            return;
        }

        renames.forEach((oldName, newName) -> {
            var value = objectNode.remove(oldName);

            if (Objects.nonNull(value)) {
                objectNode.set(newName, value);
            }
        });

        objectNode.fields()
            .forEachRemaining(entry ->
                applyFieldRenames(entry.getValue(), renames));
    }
}
