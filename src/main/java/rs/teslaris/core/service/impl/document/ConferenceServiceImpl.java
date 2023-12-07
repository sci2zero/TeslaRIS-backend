package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.ConferenceConverter;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ConferenceJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;

@Service
@Transactional
public class ConferenceServiceImpl extends EventServiceImpl implements ConferenceService {

    private final ConferenceJPAServiceImpl conferenceJPAService;

    @Autowired
    public ConferenceServiceImpl(EventRepository eventRepository,
                                 PersonContributionService personContributionService,
                                 MultilingualContentService multilingualContentService,
                                 EventIndexRepository eventIndexRepository,
                                 ConferenceJPAServiceImpl conferenceJPAService) {
        super(eventRepository, personContributionService, multilingualContentService,
            eventIndexRepository);
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
        var index = new EventIndex();
        index.setEventType(EventType.CONFERENCE);

        setEventCommonFields(conference, conferenceDTO);
        setConferenceRelatedFields(conference, conferenceDTO);

        indexEventCommonFields(index, conference);

        var savedConference = conferenceJPAService.save(conference);

        index.setDatabaseId(savedConference.getId());
        eventIndexRepository.save(index);

        return savedConference;
    }

    @Override
    public void updateConference(ConferenceDTO conferenceDTO, Integer conferenceId) {
        var conferenceToUpdate = findConferenceById(conferenceId);

        clearEventCommonFields(conferenceToUpdate);
        setConferenceRelatedFields(conferenceToUpdate, conferenceDTO);

        conferenceJPAService.save(conferenceToUpdate);

        EventIndex indexToUpdate = new EventIndex();
        var indexToUpdateOptional = eventIndexRepository.findByDatabaseId(conferenceId);
        if (indexToUpdateOptional.isPresent()) {
            indexToUpdate = indexToUpdateOptional.get();
        }

        clearEventIndexCommonFields(indexToUpdate);
        indexEventCommonFields(indexToUpdate, conferenceToUpdate);
        indexToUpdate.setDatabaseId(conferenceToUpdate.getId());

        eventIndexRepository.save(indexToUpdate);
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
