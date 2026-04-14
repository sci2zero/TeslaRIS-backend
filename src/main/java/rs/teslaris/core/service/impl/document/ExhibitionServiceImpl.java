package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.ExhibitionConverter;
import rs.teslaris.core.dto.document.ExhibitionDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.Exhibition;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.repository.document.ExhibitionRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ExhibitionJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.ExhibitionService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.FunctionalUtil;

@Service
@Traceable
@Slf4j
public class ExhibitionServiceImpl extends EventServiceImpl implements ExhibitionService {

    private final ExhibitionJPAServiceImpl exhibitionJPAService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final ExhibitionRepository exhibitionRepository;


    @Autowired
    public ExhibitionServiceImpl(EventIndexRepository eventIndexRepository,
                                 MultilingualContentService multilingualContentService,
                                 PersonContributionService personContributionService,
                                 EventRepository eventRepository,
                                 IndexBulkUpdateService indexBulkUpdateService,
                                 CommissionRepository commissionRepository,
                                 EventsRelationRepository eventsRelationRepository,
                                 SearchService<EventIndex> searchService,
                                 CountryService countryService,
                                 OrganisationUnitService organisationUnitService,
                                 DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                 ResearchAreaService researchAreaService,
                                 ExhibitionJPAServiceImpl exhibitionJPAService,
                                 DocumentPublicationIndexRepository documentPublicationIndexRepository1,
                                 ExhibitionRepository exhibitionRepository) {
        super(eventIndexRepository, multilingualContentService, personContributionService,
            eventRepository, indexBulkUpdateService, commissionRepository,
            documentPublicationIndexRepository,
            eventsRelationRepository, searchService, countryService,
            organisationUnitService,
            researchAreaService);
        this.exhibitionJPAService = exhibitionJPAService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository1;
        this.exhibitionRepository = exhibitionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExhibitionDTO> readAllExhibitions(Pageable pageable) {
        return exhibitionJPAService.findAll(pageable).map(ExhibitionConverter::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ExhibitionDTO readExhibitionByOldId(Integer oldId) {
        return ExhibitionConverter.toDTO(exhibitionRepository.findExhibitionByOldIdsContains(oldId)
            .orElseThrow(() -> new NotFoundException(
                "Exhibition with old ID " + oldId + " does not exist.")));
    }

    @Override
    public Page<EventIndex> searchExhibitions(List<String> tokens, Pageable pageable,
                                              Boolean returnOnlyNonSerialEvents,
                                              Boolean returnOnlySerialEvents,
                                              Integer commissionInstitutionId,
                                              Integer commissionId,
                                              Boolean emptyEventsOnly) {
        return searchEvents(tokens, pageable, List.of(EventType.CONFERENCE),
            returnOnlyNonSerialEvents, returnOnlySerialEvents,
            commissionInstitutionId, commissionId, emptyEventsOnly);
    }

    @Override
    public Page<EventIndex> searchExhibitionsForImport(List<String> names, String dateFrom,
                                                       String dateTo) {
        return searchEventsImport(names, dateFrom, dateTo);
    }

    @Override
    @Transactional(readOnly = true)
    public ExhibitionDTO readExhibition(Integer exhibitionId) {
        Exhibition exhibition;
        try {
            exhibition = findExhibitionById(exhibitionId);
        } catch (NotFoundException e) {
            eventIndexRepository.findByEventTypeAndDatabaseId(EventType.EXHIBITION, exhibitionId)
                .ifPresent(eventIndexRepository::delete);
            throw e;
        }

        return ExhibitionConverter.toDTO(exhibition);
    }

    @Override
    @Transactional
    public Exhibition findExhibitionById(Integer exhibitionId) {
        return exhibitionJPAService.findOne(exhibitionId);
    }

    @Override
    @Transactional
    public Exhibition findRaw(Integer exhibitionId) {
        return exhibitionRepository.findRaw(exhibitionId)
            .orElseThrow(() -> new NotFoundException("Exhibition with given ID does not exist."));
    }

    @Override
    @Transactional
    public Exhibition createExhibition(ExhibitionDTO exhibitionDTO, Boolean index) {
        var exhibition = new Exhibition();

        setEventCommonFields(exhibition, exhibitionDTO);
        setExhibitionRelatedFields(exhibition, exhibitionDTO);

        var savedExhibition = exhibitionJPAService.save(exhibition);

        if (index) {
            indexExhibition(savedExhibition, new EventIndex());
        }

        return savedExhibition;
    }

    @Override
    @Transactional
    public void updateExhibition(Integer exhibitionId, ExhibitionDTO exhibitionDTO) {
        var exhibitionToUpdate = findExhibitionById(exhibitionId);

        clearEventCommonFields(exhibitionToUpdate);
        setEventCommonFields(exhibitionToUpdate, exhibitionDTO);
        setExhibitionRelatedFields(exhibitionToUpdate, exhibitionDTO);

        exhibitionJPAService.save(exhibitionToUpdate);

        var indexToUpdate =
            eventIndexRepository.findByDatabaseId(exhibitionId).orElse(new EventIndex());

        clearEventIndexCommonFields(indexToUpdate);
        indexExhibition(exhibitionToUpdate, indexToUpdate);
    }

    @Override
    @Transactional
    public void deleteExhibition(Integer exhibitionId) {
        var exhibition = exhibitionJPAService.findOne(exhibitionId);
        exhibition.getContributions().forEach(contribution -> {
            contribution.setDeleted(true);
            personContributionService.save(contribution);
        });

        exhibitionJPAService.delete(exhibitionId);

        var index = eventIndexRepository.findByDatabaseId(exhibitionId);
        index.ifPresent(eventIndexRepository::delete);
    }

    @Override
    @Transactional
    public void forceDeleteExhibition(Integer exhibitionId) {
        exhibitionJPAService.delete(exhibitionId);

        completeForceDeletion(exhibitionId);
    }

    @Override
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexExhibitions() {
        // Super service does the initial deletion

        FunctionalUtil.performBulkOperation(
            exhibitionJPAService::findAll,
            Sort.by(Sort.Direction.ASC, "id"),
            (exhibition) -> indexExhibition(exhibition, new EventIndex())
        );

        return null;
    }

    private void setExhibitionRelatedFields(Exhibition exhibition, ExhibitionDTO exhibitionDTO) {
        if (!exhibition.getSerialEvent()) {
            exhibition.setNumber(exhibitionDTO.getNumber());
        }

        exhibition.setFee(exhibitionDTO.getFee());
    }

    @Override
    @Transactional
    public boolean isIdentifierInUse(String identifier, Integer exhibitionId) {
        return false; // Always false, until we decide to add exhibition identifiers
    }

    @Override
    @Transactional(readOnly = true)
    public void indexExhibition(Exhibition exhibition) {
        eventIndexRepository.findByDatabaseId(exhibition.getId()).ifPresent(index -> {
            indexExhibition(exhibition, index);
        });
    }

    @Override
    @Transactional
    public void save(Exhibition exhibition) {
        exhibitionRepository.save(exhibition);
    }

    private void indexExhibition(Exhibition exhibition, EventIndex index) {
        index.setDatabaseId(exhibition.getId());
        index.setEventType(EventType.EXHIBITION);

        indexEventCommonFields(index, exhibition);
        eventIndexRepository.save(index);
    }

    @Override
    @Transactional
    public void reindexExhibition(Integer exhibitionId) {
        var exhibitionToIndex = exhibitionJPAService.findOne(exhibitionId);
        var indexToUpdate =
            eventIndexRepository.findByDatabaseId(exhibitionId).orElse(new EventIndex());
        indexExhibition(exhibitionToIndex, indexToUpdate);
        reindexVolatileExhibitionInformation(exhibitionId);
    }

    @Override
    @Transactional
    public void reindexVolatileExhibitionInformation(Integer exhibitionId) {
        eventIndexRepository.findByDatabaseId(exhibitionId)
            .ifPresent(this::setEventCommonVolatileFields);
    }

    @Override
    @Transactional
    public void reorderExhibitionContributions(Integer exhibitionId, Integer contributionId,
                                               Integer oldContributionOrderNumber,
                                               Integer newContributionOrderNumber) {
        reorderEventContributions(
            exhibitionId,
            contributionId,
            oldContributionOrderNumber,
            newContributionOrderNumber
        );
    }
}
