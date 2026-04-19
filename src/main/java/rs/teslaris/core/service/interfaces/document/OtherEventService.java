package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.model.document.OtherEvent;

@Service
public interface OtherEventService {

    OtherEvent findOtherEventById(Integer otherEventId);

    Page<OtherEventDTO> readAllOtherEvents(Pageable pageable);

    Page<EventIndex> searchOtherEventsForImport(List<String> names, String dateFrom, String dateTo);

    OtherEventDTO readOtherEvent(Integer otherEventId);

    OtherEvent createOtherEvent(OtherEventDTO dto, Boolean index);

    void updateOtherEvent(Integer id, OtherEventDTO dto);

    void deleteOtherEvent(Integer id);

    void forceDeleteOtherEvent(Integer otherEventId);

    CompletableFuture<Void> reindexOtherEvents();

    void indexOtherEvent(OtherEvent otherEvent);

    void reindexOtherEvent(Integer otherEventId);

    void reindexVolatileOtherEventInformation(Integer otherEventId);

    void reorderOtherEventContributions(Integer otherEventId, Integer contributionId,
                                        Integer oldContributionOrderNumber,
                                        Integer newContributionOrderNumber);

    boolean isIdentifierInUse(String identifier, Integer otherEventId);
}
