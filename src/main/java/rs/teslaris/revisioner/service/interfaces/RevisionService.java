package rs.teslaris.revisioner.service.interfaces;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import rs.teslaris.revisioner.dto.QualityReportResponseDTO;
import rs.teslaris.revisioner.model.RevisionCreateEvent;

public interface RevisionService {

    void createRevisionIfChanged(RevisionCreateEvent event);

    List<Instant> getRevisionTimestamps(String entityType, Integer entityId);

    Optional<String> getRevisionAtTimestamp(String entityType, Integer entityId, Instant timestamp);

    List<QualityReportResponseDTO> getQualityReportAtTimestamp(String entityType, Integer entityId,
                                                               Instant timestamp);
}
