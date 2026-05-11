package rs.teslaris.core.service.impl.document;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.ReindexExternalIndicatorsEvent;
import rs.teslaris.core.converter.document.PerformanceRelatedOutputConverter;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.PerformanceRelatedOutput;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.PerformanceRelatedOutputRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.PerformanceRelatedOutputJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PerformanceRelatedOutputService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.session.SessionUtil;

@Service
@Traceable
@Slf4j
public class PerformanceRelatedOutputServiceImpl extends DocumentPublicationServiceImpl implements
    PerformanceRelatedOutputService {

    private final PerformanceRelatedOutputJPAServiceImpl performanceRelatedOutputJPAService;

    private final LanguageTagService languageTagService;

    private final PerformanceRelatedOutputRepository performanceRelatedOutputRepository;


    @Autowired
    public PerformanceRelatedOutputServiceImpl(
        MultilingualContentService multilingualContentService,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        SearchService<DocumentPublicationIndex> searchService,
        OrganisationUnitService organisationUnitService,
        DocumentRepository documentRepository,
        DocumentFileService documentFileService,
        CitationService citationService,
        ApplicationEventPublisher applicationEventPublisher,
        PersonContributionService personContributionService,
        ExpressionTransformer expressionTransformer,
        EventService eventService,
        CommissionRepository commissionRepository,
        SearchFieldsLoader searchFieldsLoader,
        OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService,
        InvolvementRepository involvementRepository,
        OrganisationUnitOutputConfigurationService organisationUnitOutputConfigurationService,
        DocumentLookupService documentLookupService,
        CountryService countryService,
        PerformanceRelatedOutputJPAServiceImpl performanceRelatedOutputJPAService,
        LanguageTagService languageTagService,
        PerformanceRelatedOutputRepository performanceRelatedOutputRepository) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService,
            documentLookupService,
            countryService);
        this.performanceRelatedOutputJPAService = performanceRelatedOutputJPAService;
        this.languageTagService = languageTagService;
        this.performanceRelatedOutputRepository = performanceRelatedOutputRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceRelatedOutput findPerformanceRelatedOutputById(
        Integer performanceRelatedOutputId) {
        return performanceRelatedOutputJPAService.findOne(performanceRelatedOutputId);
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceRelatedOutputDTO readPerformanceRelatedOutputById(
        Integer performanceRelatedOutputId) {
        PerformanceRelatedOutput performanceRelatedOutput;
        try {
            performanceRelatedOutput =
                performanceRelatedOutputJPAService.findOne(performanceRelatedOutputId);
        } catch (NotFoundException e) {
            log.info(
                "Trying to read non-existent PERFORMANCE_RELATED_OUTPUT with ID {}. Clearing index.",
                performanceRelatedOutputId);
            this.clearIndexWhenFailedRead(performanceRelatedOutputId,
                DocumentPublicationType.PERFORMANCE_RELATED_OUTPUT);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !performanceRelatedOutput.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return PerformanceRelatedOutputConverter.toDTO(performanceRelatedOutput);
    }

    @Override
    @Transactional
    public PerformanceRelatedOutput createPerformanceRelatedOutput(
        PerformanceRelatedOutputDTO performanceRelatedOutputDTO, Boolean index) {
        var newPerformanceRelatedOutput = new PerformanceRelatedOutput();

        checkForDocumentDate(performanceRelatedOutputDTO);
        setCommonFields(newPerformanceRelatedOutput, performanceRelatedOutputDTO);
        setPerformanceRelatedOutputRelatedFields(newPerformanceRelatedOutput,
            performanceRelatedOutputDTO);

        var savedPerformanceRelatedOutput =
            performanceRelatedOutputJPAService.save(newPerformanceRelatedOutput);

        if (index) {
            indexPerformanceRelatedOutput(savedPerformanceRelatedOutput,
                new DocumentPublicationIndex());
        }

        sendNotifications(savedPerformanceRelatedOutput, Collections.emptySet());

        return savedPerformanceRelatedOutput;
    }

    @Override
    @Transactional
    public void editPerformanceRelatedOutput(Integer performanceRelatedOutputId,
                                             PerformanceRelatedOutputDTO performanceRelatedOutputDTO) {
        var performanceRelatedOutputToUpdate =
            performanceRelatedOutputJPAService.findOne(performanceRelatedOutputId);

        var oldContributorIds =
            performanceRelatedOutputToUpdate.getContributors().stream()
                .filter(c -> Objects.nonNull(c.getPerson()))
                .map(c -> c.getPerson().getId())
                .collect(Collectors.toSet());

        checkForDocumentDate(performanceRelatedOutputDTO);
        clearCommonFields(performanceRelatedOutputToUpdate);
        setCommonFields(performanceRelatedOutputToUpdate, performanceRelatedOutputDTO);

        if (Objects.nonNull(performanceRelatedOutputToUpdate.getLanguages())) {
            performanceRelatedOutputToUpdate.getLanguages().clear();
        } else {
            performanceRelatedOutputToUpdate.setLanguages(new HashSet<>());
        }

        setPerformanceRelatedOutputRelatedFields(performanceRelatedOutputToUpdate,
            performanceRelatedOutputDTO);

        var updatedPerformanceRelatedOutput =
            performanceRelatedOutputJPAService.save(performanceRelatedOutputToUpdate);

        indexPerformanceRelatedOutput(performanceRelatedOutputToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                    performanceRelatedOutputId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(updatedPerformanceRelatedOutput, oldContributorIds);
    }

    @Override
    @Transactional
    public void deletePerformanceRelatedOutput(Integer performanceRelatedOutputId) {
        var performanceRelatedOutputToDelete =
            performanceRelatedOutputJPAService.findOne(performanceRelatedOutputId);

        deleteProofsAndFileItems(performanceRelatedOutputToDelete);

        performanceRelatedOutputJPAService.delete(performanceRelatedOutputId);
        this.delete(performanceRelatedOutputId);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(performanceRelatedOutputId));
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexPerformanceRelatedOutputs() {
        // Super service does the initial deletion

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            performanceRelatedOutputJPAService::findAll,
            performanceRelatedOutput -> {
                var index = indexPerformanceRelatedOutput(performanceRelatedOutput,
                    new DocumentPublicationIndex());
                applicationEventPublisher.publishEvent(new ReindexExternalIndicatorsEvent(index));
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void indexPerformanceRelatedOutput(PerformanceRelatedOutput performanceRelatedOutput) {
        indexPerformanceRelatedOutput(performanceRelatedOutput,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                performanceRelatedOutput.getId()).orElse(new DocumentPublicationIndex()));
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceRelatedOutputDTO readPerformanceRelatedOutputByOldId(Integer oldId) {
        var performanceRelatedOutput =
            performanceRelatedOutputRepository.findPerformanceRelatedOutputByOldIdsContains(oldId);
        if (performanceRelatedOutput.isEmpty() || (!SessionUtil.isUserLoggedIn() &&
            !performanceRelatedOutput.get().getApproveStatus().equals(ApproveStatus.APPROVED))) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return PerformanceRelatedOutputConverter.toDTO(performanceRelatedOutput.get());
    }

    private void setPerformanceRelatedOutputRelatedFields(
        PerformanceRelatedOutput performanceRelatedOutput, PerformanceRelatedOutputDTO dto) {
        performanceRelatedOutput.setType(dto.getType());

        performanceRelatedOutput.setProducer(
            multilingualContentService.getMultilingualContent(dto.getProducer()));
        performanceRelatedOutput.setDistributor(
            multilingualContentService.getMultilingualContent(dto.getDistributor()));
        performanceRelatedOutput.setSourceTitle(
            multilingualContentService.getMultilingualContent(dto.getSourceTitle()));
        performanceRelatedOutput.setOtherActors(
            multilingualContentService.getMultilingualContent(dto.getOtherActors()));

        if (CollectionOperations.containsValues(dto.getLanguageTagIds())) {
            dto.getLanguageTagIds().forEach(languageTagId -> performanceRelatedOutput.getLanguages()
                .add(languageTagService.findLanguageTagById(languageTagId)));
        }
    }

    private DocumentPublicationIndex indexPerformanceRelatedOutput(
        PerformanceRelatedOutput performanceRelatedOutput, DocumentPublicationIndex index) {
        indexCommonFields(performanceRelatedOutput, index);

        index.setType(DocumentPublicationType.PERFORMANCE_RELATED_OUTPUT.name());

        index.setApa(
            citationService.craftCitationInGivenStyle("apa", index, LanguageAbbreviations.ENGLISH));

        documentPublicationIndexRepository.save(index);

        return index;
    }
}
