package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.OtherEventConverter;
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.OtherEvent;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.repository.document.OtherEventRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.OtherEventJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.OtherEventService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.FunctionalUtil;

@Service
@Traceable
@Slf4j
public class OtherEventServiceImpl extends EventServiceImpl implements OtherEventService {

    private final OtherEventJPAServiceImpl otherEventJPAService;

    private final OtherEventRepository otherEventRepository;


    @Autowired
    public OtherEventServiceImpl(EventIndexRepository eventIndexRepository,
                                 MultilingualContentService multilingualContentService,
                                 PersonContributionService personContributionService,
                                 EventRepository eventRepository,
                                 IndexBulkUpdateService indexBulkUpdateService,
                                 CommissionRepository commissionRepository,
                                 DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 EventsRelationRepository eventsRelationRepository,
                                 SearchService<EventIndex> searchService,
                                 CountryService countryService,
                                 OrganisationUnitService organisationUnitService,
                                 ResearchAreaService researchAreaService,
                                 OtherEventJPAServiceImpl otherEventJPAService,
                                 OtherEventRepository otherEventRepository) {
        super(eventIndexRepository, multilingualContentService, personContributionService,
            eventRepository, indexBulkUpdateService, commissionRepository,
            documentPublicationIndexRepository, applicationEventPublisher, eventsRelationRepository,
            searchService, countryService, organisationUnitService, researchAreaService);
        this.otherEventJPAService = otherEventJPAService;
        this.otherEventRepository = otherEventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OtherEventDTO> readAllOtherEvents(Pageable pageable) {
        return otherEventJPAService.findAll(pageable).map(OtherEventConverter::toDTO);
    }

    @Override
    public Page<EventIndex> searchOtherEventsForImport(List<String> names, String dateFrom,
                                                       String dateTo) {
        return searchEventsImport(names, dateFrom, dateTo);
    }

    @Override
    @Transactional(readOnly = true)
    public OtherEventDTO readOtherEvent(Integer id) {
        OtherEvent event;
        try {
            event = findOtherEventById(id);
        } catch (NotFoundException e) {
            eventIndexRepository.findByEventTypeAndDatabaseId(EventType.OTHER_EVENT, id)
                .ifPresent(eventIndexRepository::delete);
            throw e;
        }

        return OtherEventConverter.toDTO(event);
    }

    @Transactional
    public OtherEvent findOtherEventById(Integer otherEventId) {
        return otherEventJPAService.findOne(otherEventId);
    }

    @Override
    @Transactional
    public OtherEvent createOtherEvent(OtherEventDTO dto, Boolean index) {
        var event = new OtherEvent();

        event.setType(dto.getType());
        setEventCommonFields(event, EventType.OTHER_EVENT, dto, new HashSet<>());

        var saved = otherEventJPAService.save(event);

        if (index) {
            indexOtherEvent(saved, new EventIndex());
        }

        return saved;
    }

    @Override
    @Transactional
    public void updateOtherEvent(Integer id, OtherEventDTO dto) {
        var event = findOtherEventById(id);

        var oldContributorIds = clearEventCommonFields(event);
        event.setType(dto.getType());

        setEventCommonFields(event, EventType.OTHER_EVENT, dto, oldContributorIds);

        otherEventJPAService.save(event);

        var index = eventIndexRepository.findByDatabaseId(id).orElse(new EventIndex());
        clearEventIndexCommonFields(index);
        indexOtherEvent(event, index);
    }

    @Override
    @Transactional
    public void deleteOtherEvent(Integer otherEventId) {
        var event = otherEventJPAService.findOne(otherEventId);
        eventRepository.deleteEventContributions(otherEventId);
        updateIndexedPersonContributions(event);

        otherEventJPAService.delete(otherEventId);
        eventIndexRepository.findByDatabaseId(otherEventId).ifPresent(eventIndexRepository::delete);
    }

    @Override
    public void forceDeleteOtherEvent(Integer otherEventId) {
        otherEventJPAService.delete(otherEventId);

        completeForceDeletion(otherEventId);
    }

    @Override
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexOtherEvents() {
        FunctionalUtil.performBulkOperation(
            otherEventJPAService::findAll,
            Sort.by(Sort.Direction.ASC, "id"),
            event -> indexOtherEvent(event, new EventIndex())
        );

        return null;
    }

    private void indexOtherEvent(OtherEvent event, EventIndex index) {
        index.setDatabaseId(event.getId());
        index.setEventType(EventType.OTHER_EVENT);
        index.setOtherEventType(event.getType());

        indexEventCommonFields(index, event);
        eventIndexRepository.save(index);
    }

    @Override
    @Transactional
    public void reindexOtherEvent(Integer otherEventId) {
        var otherEventToIndex = otherEventJPAService.findOne(otherEventId);
        var indexToUpdate =
            eventIndexRepository.findByDatabaseId(otherEventId).orElse(new EventIndex());
        indexOtherEvent(otherEventToIndex, indexToUpdate);
        reindexVolatileOtherEventInformation(otherEventId);
    }

    @Override
    public void indexOtherEvent(OtherEvent otherEvent) {
        eventIndexRepository.findByDatabaseId(otherEvent.getId())
            .ifPresent(index -> {
                indexOtherEvent(otherEvent, index);
                reindexVolatileOtherEventInformation(index.getDatabaseId());

                eventIndexRepository.save(index);
            });
    }

    @Override
    @Transactional
    public void reindexVolatileOtherEventInformation(Integer otherEventId) {
        eventIndexRepository.findByDatabaseId(otherEventId)
            .ifPresent(this::setEventCommonVolatileFields);
    }

    @Override
    @Transactional
    public void reorderOtherEventContributions(Integer otherEventId, Integer contributionId,
                                               Integer oldContributionOrderNumber,
                                               Integer newContributionOrderNumber) {
        reorderEventContributions(
            otherEventId,
            contributionId,
            oldContributionOrderNumber,
            newContributionOrderNumber
        );
    }

    @Override
    public boolean isIdentifierInUse(String identifier, Integer otherEventId) {
        return false; // Always false, until we decide to add other event identifiers
    }
}
