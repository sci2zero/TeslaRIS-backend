package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ConferenceBasicAdditionDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.model.document.Conference;

@Service
public interface ConferenceService {

    Page<ConferenceDTO> readAllConferences(Pageable pageable);

    Page<EventIndex> searchConferences(List<String> tokens, Pageable pageable,
                                       Boolean returnOnlyNonSerialEvents,
                                       Boolean returnOnlySerialEvents,
                                       Integer commissionInstitutionId,
                                       Integer commissionId);

    Page<EventIndex> searchConferencesForImport(List<String> names, String dateFrom, String dateTo);

    ConferenceDTO readConference(Integer conferenceId);

    Conference findConferenceById(Integer conferenceId);

    Conference findRaw(Integer conferenceId);

    Conference findConferenceByConfId(String confId);

    Conference createConference(ConferenceDTO conferenceDTO, Boolean index);

    Conference createConference(ConferenceBasicAdditionDTO conferenceDTO);

    void updateConference(Integer conferenceId, ConferenceDTO conferenceDTO);

    void deleteConference(Integer conferenceId);

    void forceDeleteConference(Integer conferenceId);

    CompletableFuture<Void> reindexConferences();

    void reindexConference(Integer conferenceId);

    void reindexVolatileConferenceInformation(Integer conferenceId);

    void reorderConferenceContributions(Integer conferenceId, Integer contributionId,
                                        Integer oldContributionOrderNumber,
                                        Integer newContributionOrderNumber);

    ConferenceDTO readConferenceByOldId(Integer oldId);

    boolean isIdentifierInUse(String identifier, Integer conferenceId);

    void indexConference(Conference conference);

    void save(Conference conference);
}
