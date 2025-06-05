package rs.teslaris.core.service.impl.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.document.DocumentDownloadTracker;
import rs.teslaris.core.util.tracing.SessionTrackingUtil;

@Service
@Slf4j
@Traceable
public class NoOpDownloadTracker implements DocumentDownloadTracker {

    @Override
    public void saveDocumentDownload(Integer documentId) {
        // Do not save to index nor does it update the total count
        // implementation for that is in the another module
        log.info(
            "STATISTICS - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - TYPE: DOCUMENT_DOWNLOAD - ID: {}",
            SessionTrackingUtil.getCurrentTracingContextId(),
            SessionTrackingUtil.getJSessionId(),
            SessionTrackingUtil.getCurrentClientIP(),
            documentId
        );
    }
}
