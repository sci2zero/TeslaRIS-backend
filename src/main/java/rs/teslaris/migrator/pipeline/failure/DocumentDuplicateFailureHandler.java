package rs.teslaris.migrator.pipeline.failure;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentIdentifierUpdateDTO;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.migrator.pipeline.FailureHandler;
import rs.teslaris.migrator.pipeline.FailureResolution;

/**
 * Port of {@code OAIPMHLoaderImpl.saveWithDuplicateCheck}.
 * <p>
 * When a document cannot be created because an identifier is taken, the existing document is looked
 * up: if it is the same work, its identifiers are enriched and the item counts as resolved;
 * otherwise the identifiers are dropped from the incoming DTO and the creation is retried, so the
 * work is kept without stealing another document's identifiers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentDuplicateFailureHandler {

    private final DocumentPublicationService documentPublicationService;


    public <D extends DocumentDTO> FailureHandler<D> forType(Class<D> dtoClass) {
        return (item, exception, attempt) -> {
            var dto = item.dto();

            if (attempt > 1) {
                // Identifiers were already stripped and it still failed.
                return FailureResolution.skip();
            }

            var existingDuplicate = documentPublicationService.findDocumentByCommonIdentifier(
                dto.getDoi(), dto.getOpenAlexId(), dto.getScopusId(), dto.getWebOfScienceId());

            if (existingDuplicate.isEmpty()) {
                return FailureResolution.skip();
            }

            var duplicate = existingDuplicate.get();

            if (isSameWork(dto, duplicate)) {
                enrichIdentifiers(dto, duplicate);
                return FailureResolution.resolved(duplicate.getId());
            }

            log.info("Identifier collision for '{}' is not a true match, retrying without " +
                "identifiers.", item.sourceKey());

            dto.setDoi(null);
            dto.setOpenAlexId(null);
            dto.setScopusId(null);
            dto.setWebOfScienceId(null);

            return FailureResolution.retry();
        };
    }

    private boolean isSameWork(DocumentDTO dto, Document duplicate) {
        if (Objects.isNull(dto.getTitle()) || Objects.isNull(duplicate.getTitle())) {
            return false;
        }

        return dto.getTitle().stream().anyMatch(title -> duplicate.getTitle().stream()
            .anyMatch(existing -> existing.getContent().trim()
                .equalsIgnoreCase(title.getContent().trim())));
    }

    private void enrichIdentifiers(DocumentDTO dto, Document duplicate) {
        var updateRequest = new DocumentIdentifierUpdateDTO();
        var hasUpdate = false;

        if (!StringUtil.valueExists(duplicate.getDoi()) && StringUtil.valueExists(dto.getDoi())) {
            updateRequest.setDoi(dto.getDoi());
            hasUpdate = true;
        }

        if (!StringUtil.valueExists(duplicate.getScopusId()) &&
            StringUtil.valueExists(dto.getScopusId())) {
            updateRequest.setScopusId(dto.getScopusId());
            hasUpdate = true;
        }

        if (!StringUtil.valueExists(duplicate.getOpenAlexId()) &&
            StringUtil.valueExists(dto.getOpenAlexId())) {
            updateRequest.setOpenAlexId(dto.getOpenAlexId());
            hasUpdate = true;
        }

        if (!StringUtil.valueExists(duplicate.getWebOfScienceId()) &&
            StringUtil.valueExists(dto.getWebOfScienceId())) {
            updateRequest.setWebOfScienceId(dto.getWebOfScienceId());
            hasUpdate = true;
        }

        if (hasUpdate) {
            documentPublicationService.updateDocumentIdentifiers(duplicate.getId(), updateRequest);
        }
    }
}
