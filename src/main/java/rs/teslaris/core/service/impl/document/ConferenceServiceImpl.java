package rs.teslaris.core.service.impl.document;

import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@Transactional
public class ConferenceServiceImpl extends EventServiceImpl implements ConferenceService {

    private final ConferenceRepository conferenceRepository;

    @Autowired
    public ConferenceServiceImpl(EventRepository eventRepository,
                                 PersonContributionService personContributionService,
                                 MultilingualContentService multilingualContentService,
                                 ConferenceRepository conferenceRepository) {
        super(eventRepository, personContributionService, multilingualContentService);
        this.conferenceRepository = conferenceRepository;
    }


    @Override
    public Conference findConferenceById(Integer conferenceId) {
        return conferenceRepository.findById(conferenceId)
            .orElseThrow(() -> new NotFoundException("Conference with given ID does not exist."));
    }

    @Override
    public Conference createConference(ConferenceDTO conferenceDTO) {
        var conference = new Conference();
        setEventCommonFields(conference, conferenceDTO);

        setConferenceRelatedFields(conference, conferenceDTO);

        return conferenceRepository.save(conference);
    }

    @Override
    public void updateConference(ConferenceDTO conferenceDTO, Integer conferenceId) {
        var conferenceToUpdate = findConferenceById(conferenceId);

        clearEventCommonFields(conferenceToUpdate);
        setConferenceRelatedFields(conferenceToUpdate, conferenceDTO);

        conferenceRepository.save(conferenceToUpdate);
    }

    @Override
    public void deleteConference(Integer conferenceId) {
        var conferenceToDelete = findConferenceById(conferenceId);

        if (hasCommonUsage(conferenceId)) {
            return;
        }

        conferenceRepository.delete(conferenceToDelete);
    }

    private void setConferenceRelatedFields(Conference conference, ConferenceDTO conferenceDTO) {
        conference.setNumber(conference.getNumber());
        conference.setFee(conference.getFee());
    }
}
