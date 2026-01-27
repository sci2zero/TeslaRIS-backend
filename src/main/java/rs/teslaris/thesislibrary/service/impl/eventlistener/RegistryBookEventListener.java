package rs.teslaris.thesislibrary.service.impl.eventlistener;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import rs.teslaris.core.applicationevent.RegistryBookInfoReindexEvent;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.thesislibrary.repository.RegistryBookEntryRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistryBookEventListener {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final RegistryBookEntryRepository registryBookEntryRepository;


    @EventListener
    protected void handleThesisRegistryBookInfoReindexEventEvent(
        RegistryBookInfoReindexEvent ignored) {
        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<DocumentPublicationIndex> chunk =
                documentPublicationIndexRepository.findByTypeIn(
                        List.of(DocumentPublicationType.THESIS.name()),
                        PageRequest.of(pageNumber, chunkSize,
                            Sort.by(Sort.Direction.ASC, "databaseId")))
                    .getContent();

            chunk.forEach(thesisIndex -> {
                try {
                    thesisIndex.setIsAddedToRegistryBook(
                        registryBookEntryRepository.isThesisInRegistryBook(
                            thesisIndex.getDatabaseId()));
                    documentPublicationIndexRepository.save(thesisIndex);
                } catch (Exception e) {
                    log.warn(
                        "Skipping indexing registry book info for THESIS {} due to indexing error: {}",
                        thesisIndex.getDatabaseId(),
                        e.getMessage());
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }
}
