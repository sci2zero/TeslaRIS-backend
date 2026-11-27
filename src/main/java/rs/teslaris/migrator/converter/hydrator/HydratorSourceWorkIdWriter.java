package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.identifier.DocumentIdentifierDTO;
import rs.teslaris.core.model.identifier.DocumentIdentifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.IdentifierRepository;
import rs.teslaris.core.service.interfaces.identifier.DocumentIdentifierService;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Keeps the curriculum's own id of an output ("source-work-id") on the document made from it, as
 * the {@link HydratorSource#SOURCE_WORK_IDENTIFIER_CODE} other identifier. A document several
 * curricula list (a co-author's copy, a repeated entry) collects the id of each.
 */
@Component
@RequiredArgsConstructor
public class HydratorSourceWorkIdWriter implements RunScopedCache {

    private final IdentifierRepository identifierRepository;

    private final EntityIdentifierRepository entityIdentifierRepository;

    private final DocumentIdentifierService documentIdentifierService;

    private final HydratorOutputConverters converters;

    private final MigrationLog migrationLog;

    private Integer identifierId;


    public void attach(MigrationEntityType type, HydratorCVModel.Curriculum record,
                       HydratorCVModel.Output output, Integer documentId) {
        var value = converters.sourceWorkId(output);

        if (Objects.isNull(documentId) || Objects.isNull(value)) {
            return;
        }

        // Written by an earlier run of the same item
        if (entityIdentifierRepository.existsByValueForType(value, DocumentIdentifier.class,
            null)) {
            return;
        }

        var reason = store(documentId, value);

        if (Objects.nonNull(reason)) {
            migrationLog.valueDropped(HydratorSource.NAME, type.name(),
                converters.outputKey(record, output), HydratorMappingRule.cvWithoutRule(),
                "source work id '" + value + "' not kept: " + reason);
        }
    }

    @Override
    public synchronized void clearCache() {
        identifierId = null;
    }

    // The reason it was not stored, or null
    private String store(Integer documentId, String value) {
        var definition = identifierId();

        if (Objects.isNull(definition)) {
            return "identifier '" + HydratorSource.SOURCE_WORK_IDENTIFIER_CODE + "' is not defined";
        }

        var dto = new DocumentIdentifierDTO();
        dto.setDocumentId(documentId);
        dto.setIdentifierId(definition);
        dto.setValue(value);

        try {
            documentIdentifierService.createDocumentIdentifier(dto, null);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private synchronized Integer identifierId() {
        if (Objects.isNull(identifierId)) {
            var identifier =
                identifierRepository.findByCode(HydratorSource.SOURCE_WORK_IDENTIFIER_CODE);
            identifierId = Objects.isNull(identifier) ? null : identifier.getId();
        }

        return identifierId;
    }
}
