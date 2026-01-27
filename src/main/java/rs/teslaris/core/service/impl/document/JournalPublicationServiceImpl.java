package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.JournalPublicationConverter;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.JournalPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.session.SessionUtil;

@Service
@Traceable
@Slf4j
public class JournalPublicationServiceImpl extends DocumentPublicationServiceImpl
    implements JournalPublicationService {

    private final JournalPublicationJPAServiceImpl journalPublicationJPAService;

    private final JournalService journalService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final ProceedingsPublicationRepository proceedingsPublicationRepository;


    @Autowired
    public JournalPublicationServiceImpl(MultilingualContentService multilingualContentService,
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
                                         JournalPublicationJPAServiceImpl journalPublicationJPAService,
                                         JournalService journalService,
                                         DocumentPublicationIndexRepository documentPublicationIndexRepository1,
                                         ProceedingsPublicationRepository proceedingsPublicationRepository) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService);
        this.journalPublicationJPAService = journalPublicationJPAService;
        this.journalService = journalService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository1;
        this.proceedingsPublicationRepository = proceedingsPublicationRepository;
    }

    @Override
    @Transactional
    public JournalPublication findJournalPublicationById(Integer publicationId) {
        return journalPublicationJPAService.findOne(publicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public JournalPublicationResponseDTO readJournalPublicationById(Integer publicationId) {
        JournalPublication publication;
        try {
            publication = findJournalPublicationById(publicationId);
        } catch (NotFoundException e) {
            log.info("Trying to read non-existent JOURNAL_PUBLICATION with ID {}. Clearing index.",
                publicationId);
            this.clearIndexWhenFailedRead(publicationId,
                DocumentPublicationType.JOURNAL_PUBLICATION);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !publication.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return JournalPublicationConverter.toDTO(publication);
    }

    @Override
    @Transactional
    public List<DocumentPublicationIndex> findMyPublicationsInJournal(Integer journalId,
                                                                      Integer authorId) {
        return documentPublicationIndexRepository.findByTypeAndJournalIdAndAuthorIds(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), journalId, authorId);
    }

    @Override
    @Transactional
    public Page<DocumentPublicationIndex> findPublicationsInJournal(Integer journalId,
                                                                    Pageable pageable) {
        if (!SessionUtil.isUserLoggedIn()) {
            return documentPublicationIndexRepository.findByTypeAndJournalIdAndIsApprovedTrue(
                DocumentPublicationType.JOURNAL_PUBLICATION.name(), journalId, pageable);
        }

        return documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), journalId, pageable);
    }

    @Override
    @Transactional
    public JournalPublication createJournalPublication(JournalPublicationDTO publicationDTO,
                                                       Boolean index) {
        var publication = new JournalPublication();

        setCommonFields(publication, publicationDTO);
        setJournalPublicationRelatedFields(publication, publicationDTO);

        var savedPublication = journalPublicationJPAService.save(publication);

        if (index) {
            indexJournalPublication(savedPublication, new DocumentPublicationIndex());
        }

        sendNotifications(savedPublication);

        return savedPublication;
    }

    @Override
    @Transactional
    public void editJournalPublication(Integer publicationId,
                                       JournalPublicationDTO publicationDTO) {
        var publicationToUpdate = findJournalPublicationById(publicationId);

        clearCommonFields(publicationToUpdate);
        publicationToUpdate.getUris().clear();

        setCommonFields(publicationToUpdate, publicationDTO);
        setJournalPublicationRelatedFields(publicationToUpdate, publicationDTO);

        var indexToUpdate = findDocumentPublicationIndexByDatabaseId(publicationId);

        indexJournalPublication(publicationToUpdate, indexToUpdate);
        journalService.reindexJournalVolatileInformation(publicationToUpdate.getJournal().getId());

        journalPublicationJPAService.save(publicationToUpdate);

        sendNotifications(publicationToUpdate);

        if (Objects.nonNull(publicationDTO.getJournalId()) &&
            !publicationDTO.getJournalId().equals(indexToUpdate.getJournalId())) {
            journalService.reindexJournalVolatileInformation(indexToUpdate.getJournalId());
        }
    }

    @Override
    @Transactional
    public void deleteJournalPublication(Integer journalPublicationId) {
        var publicationToDelete = findJournalPublicationById(journalPublicationId);

        deleteProofsAndFileItems(publicationToDelete);

        journalPublicationJPAService.delete(journalPublicationId);
        this.delete(journalPublicationId);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(journalPublicationId));
    }

    @Override
    @Transactional(readOnly = true)
    public void indexJournalPublication(JournalPublication publication,
                                        DocumentPublicationIndex index) {
        indexCommonFields(publication, index);

        index.setPublicationSeriesId(publication.getJournal().getId());
        index.setType(DocumentPublicationType.JOURNAL_PUBLICATION.name());

        if (Objects.nonNull(publication.getJournalPublicationType())) {
            index.setPublicationType(publication.getJournalPublicationType().name());
        }

        index.setJournalId(publication.getJournal().getId());
        calculateNumberOfPages(publication, index);

        index.setApa(
            citationService.craftCitationInGivenStyle("apa", index, LanguageAbbreviations.ENGLISH));
        documentPublicationIndexRepository.save(index);
    }

    @Override
    @Transactional(readOnly = true)
    public void indexJournalPublication(JournalPublication publication) {
        indexJournalPublication(publication,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                publication.getId()).orElse(new DocumentPublicationIndex()));
    }

    @Override
    @Transactional
    public Integer transferProceedingsPublicationToJournal(Integer proceedingsPublicationId,
                                                           Integer journalId) {
        var proceedingsPublication =
            proceedingsPublicationRepository.findById(proceedingsPublicationId);

        if (proceedingsPublication.isEmpty()) {
            throw new NotFoundException("Proceedings publication with given ID does not exist.");
        }

        var proceedingsId = proceedingsPublication.get().getProceedings().getId();
        var journalPublication = new JournalPublication(proceedingsPublication.get());
        journalPublication.setJournal(journalService.findJournalById(journalId));

        proceedingsPublicationRepository.delete(proceedingsPublication.get());
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            proceedingsPublicationId).ifPresent(documentPublicationIndexRepository::delete);

        var saved = journalPublicationJPAService.save(journalPublication);
        indexJournalPublication(saved, new DocumentPublicationIndex());

        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            proceedingsId).ifPresent(proceedingsIndex -> {
                proceedingsIndex.setHasPublications(
                    (documentPublicationIndexRepository.countByProceedingsId(proceedingsId) > 0)
                );
                documentPublicationIndexRepository.save(proceedingsIndex);
            }
        );

        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexJournalPublications() {
        // Super service does the initial deletion

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            journalPublicationJPAService::findAll,
            journalPublication ->
                indexJournalPublication(journalPublication, new DocumentPublicationIndex())
        );
    }

    private void setJournalPublicationRelatedFields(JournalPublication publication,
                                                    JournalPublicationDTO publicationDTO) {
        if (Objects.nonNull(publicationDTO.getJournalPublicationType())) {
            publication.setJournalPublicationType(publicationDTO.getJournalPublicationType());
        } else {
            publication.setJournalPublicationType(JournalPublicationType.RESEARCH_ARTICLE);
        }

        publication.setStartPage(publicationDTO.getStartPage());
        publication.setEndPage(publicationDTO.getEndPage());
        publication.setNumberOfPages(publicationDTO.getNumberOfPages());
        publication.setArticleNumber(publicationDTO.getArticleNumber());
        publication.setVolume(publicationDTO.getVolume());
        publication.setIssue(publicationDTO.getIssue());

        publication.setJournal(journalService.findJournalById(publicationDTO.getJournalId()));
    }
}
