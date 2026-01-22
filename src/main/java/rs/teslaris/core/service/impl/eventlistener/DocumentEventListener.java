package rs.teslaris.core.service.impl.eventlistener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.teslaris.core.applicationevent.PersonEmploymentOUHierarchyStructureChangedEvent;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@Component
@RequiredArgsConstructor
public class DocumentEventListener {

    private final DocumentPublicationService documentPublicationService;


    @Async("taskExecutor")
    @EventListener
    protected void handlePersonEmploymentOUHierarchyStructureChangedEvent(
        PersonEmploymentOUHierarchyStructureChangedEvent event) {
        documentPublicationService.reindexEmploymentInformationForAllPersonPublications(
            event.getPersonId());
    }
}
