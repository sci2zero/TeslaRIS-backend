package rs.teslaris.exporter.service.impl.eventlistener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.teslaris.core.applicationevent.ThesisUnarchivedEvent;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.service.interfaces.CommonExportService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExportEventListener {

    private final CommonExportService commonExportService;


    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    protected void handleThesisUnarchived(ThesisUnarchivedEvent event) {
        try {
            commonExportService.deleteDocumentFromCommonModel(
                event.thesisId(), ExportPublicationType.THESIS);
        } catch (Exception e) {
            log.error("Unable to delete export record for unarchived thesis with ID {}. Reason: {}",
                event.thesisId(), e.getMessage(), e);
        }
    }
}
