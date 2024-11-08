package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.document.ConferenceConverter;
import rs.teslaris.core.dto.document.ConferenceBasicAdditionDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ConferenceJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;

@Service
@Transactional
public class ConferenceServiceImpl extends EventServiceImpl implements ConferenceService {

    private final ConferenceJPAServiceImpl conferenceJPAService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    @Autowired
    public ConferenceServiceImpl(EventIndexRepository eventIndexRepository,
                                 MultilingualContentService multilingualContentService,
                                 PersonContributionService personContributionService,
                                 EventRepository eventRepository,
                                 EventsRelationRepository eventsRelationRepository,
                                 SearchService<EventIndex> searchService, EmailUtil emailUtil,
                                 CountryService countryService,
                                 ConferenceJPAServiceImpl conferenceJPAService,
                                 DocumentPublicationIndexRepository documentPublicationIndexRepository) {
        super(eventIndexRepository, multilingualContentService, personContributionService,
            eventRepository, eventsRelationRepository, searchService, emailUtil, countryService);
        this.conferenceJPAService = conferenceJPAService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
    }

    @Override
    public Page<ConferenceDTO> readAllConferences(Pageable pageable) {
        return conferenceJPAService.findAll(pageable).map(ConferenceConverter::toDTO);
    }

    @Override
    public Page<EventIndex> searchConferences(List<String> tokens, Pageable pageable,
                                              Boolean returnOnlyNonSerialEvents,
                                              Boolean returnOnlySerialEvents) {
        return searchEvents(tokens, pageable, EventType.CONFERENCE, returnOnlyNonSerialEvents,
            returnOnlySerialEvents);
    }

    @Override
    public Page<EventIndex> searchConferencesForImport(List<String> names, String dateFrom,
                                                       String dateTo) {
        return searchEventsImport(names, dateFrom, dateTo);
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
    public Conference createConference(ConferenceDTO conferenceDTO, Boolean index) {
        var conference = new Conference();

        setEventCommonFields(conference, conferenceDTO);
        setConferenceRelatedFields(conference, conferenceDTO);

        var savedConference = conferenceJPAService.save(conference);

        if (index) {
            indexConference(savedConference, new EventIndex());
        }

        return savedConference;
    }

    @Override
    public Conference createConference(ConferenceBasicAdditionDTO conferenceDTO) {
        var conference = new Conference();

        conference.setName(
            multilingualContentService.getMultilingualContent(conferenceDTO.getName()));
        conference.setDateFrom(conferenceDTO.getDateFrom());
        conference.setDateTo(conferenceDTO.getDateTo());
        conference.setSerialEvent(false);

        var savedConference = conferenceJPAService.save(conference);

        notifyAboutBasicCreation(savedConference.getId());

        indexConference(savedConference, new EventIndex());

        return savedConference;
    }

    @Override
    public void updateConference(Integer conferenceId, ConferenceDTO conferenceDTO) {
        var conferenceToUpdate = findConferenceById(conferenceId);

        clearEventCommonFields(conferenceToUpdate);
        setEventCommonFields(conferenceToUpdate, conferenceDTO);
        setConferenceRelatedFields(conferenceToUpdate, conferenceDTO);

        conferenceJPAService.save(conferenceToUpdate);

        var indexToUpdate =
            eventIndexRepository.findByDatabaseId(conferenceId).orElse(new EventIndex());

        clearEventIndexCommonFields(indexToUpdate);
        indexConference(conferenceToUpdate, indexToUpdate);
    }

    @Override
    public void deleteConference(Integer conferenceId) {
        if (hasCommonUsage(conferenceId)) {
            throw new ConferenceReferenceConstraintViolationException(
                "Conference with given ID is in use and cannot be deleted.");
        }

        conferenceJPAService.delete(conferenceId);

        var index = eventIndexRepository.findByDatabaseId(conferenceId);
        index.ifPresent(eventIndexRepository::delete);
    }

    @Override
    public void forceDeleteConference(Integer conferenceId) {
        eventRepository.deleteAllPublicationsInEvent(conferenceId);
        eventRepository.deleteAllProceedingsInEvent(conferenceId);

        conferenceJPAService.delete(conferenceId);

        var index = eventIndexRepository.findByDatabaseId(conferenceId);
        index.ifPresent(eventIndexRepository::delete);

        documentPublicationIndexRepository.deleteByEventIdAndType(conferenceId,
            DocumentPublicationType.PROCEEDINGS.name());
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexConferences() {
        eventIndexRepository.deleteAll();
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Conference> chunk =
                conferenceJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((conference) -> indexConference(conference, new EventIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void setConferenceRelatedFields(Conference conference, ConferenceDTO conferenceDTO) {
        conference.setNumber(conferenceDTO.getNumber());
        conference.setFee(conferenceDTO.getFee());

        IdentifierUtil.validateAndSetIdentifier(
            conference.getConfId(),
            conference.getId(),
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            eventRepository::existsByConfId,
            conference::setConfId,
            "confIdFormatError",
            "confIdExistsError"
        );
    }

    private void indexConference(Conference conference, EventIndex index) {
        index.setDatabaseId(conference.getId());
        index.setEventType(EventType.CONFERENCE);

        indexEventCommonFields(index, conference);
        eventIndexRepository.save(index);
    }

    @Override
    public void reorderConferenceContributions(Integer conferenceId, Integer contributionId,
                                               Integer oldContributionOrderNumber,
                                               Integer newContributionOrderNumber) {
        var event = findOne(conferenceId);
        var contributions = event.getContributions().stream()
            .map(contribution -> (PersonContribution) contribution).collect(
                Collectors.toSet());

        personContributionService.reorderContributions(contributions, contributionId,
            oldContributionOrderNumber, newContributionOrderNumber);
    }
}
