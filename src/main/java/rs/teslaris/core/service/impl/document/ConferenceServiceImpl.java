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
import rs.teslaris.core.service.impl.document.strategydecorator.ConferenceJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;

@Service
@Transactional
public class ConferenceServiceImpl extends EventServiceImpl implements ConferenceService {

    private final ConferenceJPAServiceImpl conferenceJPAService;

    private final ConferenceRepository conferenceRepository;

    @Autowired
    public ConferenceServiceImpl(EventRepository eventRepository,
                                 PersonContributionService personContributionService,
                                 MultilingualContentService multilingualContentService,
                                 ConferenceJPAServiceImpl conferenceJPAService,
                                 ConferenceRepository conferenceRepository) {
        super(eventRepository, personContributionService, multilingualContentService);
        this.conferenceRepository = conferenceRepository;
        this.conferenceJPAService = conferenceJPAService;
    }

    @Override
    public Page<ConferenceDTO> readAllConferences(Pageable pageable) {
        return conferenceJPAService.findAll(pageable).map(ConferenceConverter::toDTO);
    }

    @Override
    public ConferenceDTO readConference(Integer conferenceId) {
        return ConferenceConverter.toDTO(findConferenceById(conferenceId));
    }

    @Override
    public Conference findConferenceById(Integer conferenceId) {
        return conferenceJPAService.findOne(conferenceId);
    }

    @Override
    public Conference createConference(ConferenceDTO conferenceDTO) {
        var conference = new Conference();
        conference.setContributions(new HashSet<>());

        setEventCommonFields(conference, conferenceDTO);
        setConferenceRelatedFields(conference, conferenceDTO);

        return conferenceJPAService.save(conference);
    }

    @Override
    public void updateConference(ConferenceDTO conferenceDTO, Integer conferenceId) {
        var conferenceToUpdate = findConferenceById(conferenceId);

        clearEventCommonFields(conferenceToUpdate);
        setConferenceRelatedFields(conferenceToUpdate, conferenceDTO);

        conferenceJPAService.save(conferenceToUpdate);
    }

    @Override
    public void deleteConference(Integer conferenceId) {

        if (hasCommonUsage(conferenceId)) {
            throw new ConferenceReferenceConstraintViolationException(
                "Conference with given ID is in use and cannot be deleted.");
        }

        this.conferenceJPAService.delete(conferenceId);
    }

    private void setConferenceRelatedFields(Conference conference, ConferenceDTO conferenceDTO) {
        conference.setNumber(conference.getNumber());
        conference.setFee(conference.getFee());
    }
}
