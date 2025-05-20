package rs.teslaris.core.service.impl.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.document.DocumentDownloadTracker;
import rs.teslaris.core.util.SessionUtil;

@Service
@Slf4j
public class NoOpDownloadTracker implements DocumentDownloadTracker {

    @Override
    public void saveDocumentDownload(Integer documentId) {
        // Do not save to index nor does it update the total count
        // implementation for that is in the another module
        log.info("STATISTICS - DOWNLOAD for Document with ID {} by {}.", documentId,
            SessionUtil.getJSessionId());
    }
}
