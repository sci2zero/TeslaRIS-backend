package rs.teslaris.core.service.impl.document;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
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
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ConferenceJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@Transactional
@Traceable
public class ConferenceServiceImpl extends EventServiceImpl implements ConferenceService {

    private final ConferenceJPAServiceImpl conferenceJPAService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final ConferenceRepository conferenceRepository;


    @Autowired
    public ConferenceServiceImpl(EventIndexRepository eventIndexRepository,
                                 MultilingualContentService multilingualContentService,
                                 PersonContributionService personContributionService,
                                 EventRepository eventRepository,
                                 IndexBulkUpdateService indexBulkUpdateService,
                                 EventsRelationRepository eventsRelationRepository,
                                 SearchService<EventIndex> searchService, EmailUtil emailUtil,
                                 CountryService countryService,
                                 CommissionRepository commissionRepository,
                                 ConferenceJPAServiceImpl conferenceJPAService,
                                 DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                 ConferenceRepository conferenceRepository) {
        super(eventIndexRepository, multilingualContentService, personContributionService,
            eventRepository, indexBulkUpdateService, commissionRepository,
            eventsRelationRepository,
            searchService,
            emailUtil, countryService);
        this.conferenceJPAService = conferenceJPAService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.conferenceRepository = conferenceRepository;
    }

    @Override
    public Page<ConferenceDTO> readAllConferences(Pageable pageable) {
        return conferenceJPAService.findAll(pageable).map(ConferenceConverter::toDTO);
    }

    @Override
    public ConferenceDTO readConferenceByOldId(Integer oldId) {
        return ConferenceConverter.toDTO(conferenceRepository.findConferenceByOldId(oldId)
            .orElseThrow(() -> new NotFoundException(
                "Conference with old ID " + oldId + " does not exist.")));
    }

    @Override
    public Page<EventIndex> searchConferences(List<String> tokens, Pageable pageable,
                                              Boolean returnOnlyNonSerialEvents,
                                              Boolean returnOnlySerialEvents,
                                              Integer commissionInstitutionId,
                                              Integer commissionId) {
        return searchEvents(tokens, pageable, EventType.CONFERENCE, returnOnlyNonSerialEvents,
            returnOnlySerialEvents, commissionInstitutionId, commissionId);
    }

    @Override
    public Page<EventIndex> searchConferencesForImport(List<String> names, String dateFrom,
                                                       String dateTo) {
        return searchEventsImport(names, dateFrom, dateTo);
    }

    @Override
    public ConferenceDTO readConference(Integer conferenceId) {
        Conference conference;
        try {
            conference = findConferenceById(conferenceId);
        } catch (NotFoundException e) {
            eventIndexRepository.findByDatabaseId(conferenceId)
                .ifPresent(eventIndexRepository::delete);
            throw e;
        }

        return ConferenceConverter.toDTO(conference);
    }

    @Override
    public Conference findConferenceById(Integer conferenceId) {
        return conferenceJPAService.findOne(conferenceId);
    }

    @Override
    @Nullable
    public Conference findConferenceByConfId(String confId) {
        return conferenceRepository.findConferenceByConfId(confId).orElse(null);
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

        savedConference.getName().stream().findFirst().ifPresent(mc -> {
            notifyAboutBasicCreation(savedConference.getId(), mc.getContent());
        });

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

        indexBulkUpdateService.removeIdFromRecord("document_publication", "event_id", conferenceId);
    }

    @Override
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexConferences() {
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
        return null;
    }

    private void setConferenceRelatedFields(Conference conference, ConferenceDTO conferenceDTO) {
        conference.setNumber(conferenceDTO.getNumber());
        conference.setFee(conferenceDTO.getFee());

        IdentifierUtil.validateAndSetIdentifier(
            conferenceDTO.getConfId(),
            conference.getId(),
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            eventRepository::existsByConfId,
            conference::setConfId,
            "confIdFormatError",
            "confIdExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            conferenceDTO.getOpenAlexId(),
            conference.getId(),
            "^S\\d{4,10}$",
            eventRepository::existsByOpenAlexId,
            conference::setOpenAlexId,
            "openAlexIdFormatError",
            "openAlexIdExistsError"
        );
    }

    @Override
    public boolean isIdentifierInUse(String identifier, Integer conferenceId) {
        return eventRepository.existsByConfId(identifier, conferenceId) ||
            eventRepository.existsByOpenAlexId(identifier, conferenceId);
    }

    @Override
    public void indexConference(Conference conference) {
        eventIndexRepository.findByDatabaseId(conference.getId()).ifPresent(index -> {
            indexConference(conference, index);
        });
    }

    @Override
    public void save(Conference conference) {
        conferenceRepository.save(conference);
    }

    private void indexConference(Conference conference, EventIndex index) {
        index.setDatabaseId(conference.getId());
        index.setEventType(EventType.CONFERENCE);

        indexEventCommonFields(index, conference);
        index.setOpenAlexId(conference.getOpenAlexId());
        eventIndexRepository.save(index);
    }

    @Override
    public void reindexConference(Integer conferenceId) {
        var conferenceToIndex = conferenceJPAService.findOne(conferenceId);
        var indexToUpdate =
            eventIndexRepository.findByDatabaseId(conferenceId).orElse(new EventIndex());
        indexConference(conferenceToIndex, indexToUpdate);
        reindexVolatileConferenceInformation(conferenceId);
    }

    @Override
    public void reindexVolatileConferenceInformation(Integer conferenceId) {
        eventIndexRepository.findByDatabaseId(conferenceId).ifPresent(eventIndex -> {
            eventIndex.setRelatedInstitutionIds(
                eventRepository.findInstitutionIdsByEventIdAndAuthorContribution(conferenceId)
                    .stream().toList());
            eventIndex.setClassifiedBy(
                commissionRepository.findCommissionsThatClassifiedEvent(conferenceId));

            eventIndexRepository.save(eventIndex);
        });
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
