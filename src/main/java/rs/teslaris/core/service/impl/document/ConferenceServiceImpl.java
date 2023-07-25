package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.ConferenceConverter;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceInUseException;
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
    public Page<ConferenceDTO> readAllConferences(Pageable pageable) {
        return conferenceRepository.findAll(pageable).map(ConferenceConverter::toDTO);
    }

    @Override
    public ConferenceDTO readConference(Integer conferenceId) {
        return ConferenceConverter.toDTO(findConferenceById(conferenceId));
    }

    @Override
    public Conference findConferenceById(Integer conferenceId) {
        return conferenceRepository.findById(conferenceId)
            .orElseThrow(() -> new NotFoundException("Conference with given ID does not exist."));
    }

    @Override
    public Conference createConference(ConferenceDTO conferenceDTO) {
        var conference = new Conference();
        conference.setContributions(new HashSet<>());

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
            throw new ConferenceInUseException(
                "Conference with given ID is in use and cannot be deleted.");
        }

        conferenceRepository.delete(conferenceToDelete);
    }

    private void setConferenceRelatedFields(Conference conference, ConferenceDTO conferenceDTO) {
        conference.setNumber(conference.getNumber());
        conference.setFee(conference.getFee());
    }
}
