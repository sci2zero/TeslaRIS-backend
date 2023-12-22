package rs.teslaris.core.service.interfaces.document;

import java.util.List;
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

    Page<EventIndex> searchConferences(List<String> tokens, Pageable pageable);

    ConferenceDTO readConference(Integer conferenceId);

    Conference findConferenceById(Integer conferenceId);

    Conference createConference(ConferenceDTO conferenceDTO);

    Conference createConference(ConferenceBasicAdditionDTO conferenceDTO);

    void updateConference(ConferenceDTO conferenceDTO, Integer conferenceId);

    void deleteConference(Integer conferenceId);
}
