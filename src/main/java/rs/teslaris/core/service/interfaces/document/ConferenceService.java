package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.model.document.Conference;

@Service
public interface ConferenceService {

    Conference findConferenceById(Integer conferenceId);

    Conference createConference(ConferenceDTO conferenceDTO);

    void updateConference(ConferenceDTO conferenceDTO, Integer conferenceId);

    void deleteConference(Integer conferenceId);
}
