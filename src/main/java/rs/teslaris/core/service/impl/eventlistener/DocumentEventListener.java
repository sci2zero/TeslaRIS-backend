package rs.teslaris.core.service.impl.eventlistener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.teslaris.core.applicationevent.DocumentClaimedEvent;
import rs.teslaris.core.applicationevent.DocumentValidationStatusChangedEvent;
import rs.teslaris.core.applicationevent.PersonEmploymentOUHierarchyStructureChangedEvent;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@Component
@RequiredArgsConstructor
public class DocumentEventListener {

    private final DocumentPublicationService documentPublicationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    @Async("taskExecutor")
    @EventListener
    protected void handlePersonEmploymentOUHierarchyStructureChangedEvent(
        PersonEmploymentOUHierarchyStructureChangedEvent event) {
        documentPublicationService.reindexEmploymentInformationForAllPersonPublications(
            event.getPersonId());
    }

    /**
     * The claim is already committed, so the index is brought in line afterwards, outside of the
     * lock which the claim held.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    protected void handleDocumentClaimed(DocumentClaimedEvent event) {
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            event.documentId()).ifPresent(index -> {
            if (event.authorIndex() < index.getAuthorIds().size()) {
                index.getAuthorIds().set(event.authorIndex(), event.personId());
            }

            index.getClaimerIds().clear();
            index.getClaimerOrdinals().clear();
            documentPublicationIndexRepository.save(index);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    protected void handleDocumentValidationStatusChanged(
        DocumentValidationStatusChangedEvent event) {
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            event.documentId()).ifPresent(index -> {
            index.setIsApproved(event.metadataValid());
            index.setAreFilesValid(event.filesValid());
            index.setIsPubliclyVisible(event.publiclyVisible());
            documentPublicationIndexRepository.save(index);
        });
    }
}
