package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.MonographPublicationConverter;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.MonographPublicationType;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.MonographPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.session.SessionUtil;

@Service
@Traceable
@Slf4j
public class MonographPublicationServiceImpl extends DocumentPublicationServiceImpl implements
    MonographPublicationService {

    private final MonographPublicationJPAServiceImpl monographPublicationJPAService;

    private final MonographService monographService;


    @Autowired
    public MonographPublicationServiceImpl(MultilingualContentService multilingualContentService,
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
                                           MonographPublicationJPAServiceImpl monographPublicationJPAService,
                                           MonographService monographService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService);
        this.monographPublicationJPAService = monographPublicationJPAService;
        this.monographService = monographService;
    }

    @Override
    @Transactional
    public MonographPublication findMonographPublicationById(Integer monographPublicationId) {
        return monographPublicationJPAService.findOne(monographPublicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public MonographPublicationDTO readMonographPublicationById(Integer monographPublicationId) {
        MonographPublication monographPublication;
        try {
            monographPublication = monographPublicationJPAService.findOne(monographPublicationId);
        } catch (NotFoundException e) {
            log.info(
                "Trying to read non-existent MONOGRAPH_PUBLICATION with ID {}. Clearing index.",
                monographPublicationId);
            this.clearIndexWhenFailedRead(monographPublicationId,
                DocumentPublicationType.MONOGRAPH_PUBLICATION);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !monographPublication.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException(
                "Monograph with ID " + monographPublicationId + " does not exist.");
        }

        return MonographPublicationConverter.toDTO(monographPublication);
    }

    @Override
    @Transactional
    public MonographPublication createMonographPublication(
        MonographPublicationDTO monographPublicationDTO, Boolean index) {
        var newMonographPublication = new MonographPublication();

        setCommonFields(newMonographPublication, monographPublicationDTO);
        setMonographPublicationRelatedFields(newMonographPublication, monographPublicationDTO);

        newMonographPublication.setApproveStatus((newMonographPublication.getIsMetadataValid() &&
            newMonographPublication.getAreFilesValid()) ? ApproveStatus.APPROVED :
            ApproveStatus.REQUESTED);

        var savedMonographPublication =
            monographPublicationJPAService.save(newMonographPublication);

        if (index) {
            indexMonographPublication(savedMonographPublication, new DocumentPublicationIndex());
        }

        sendNotifications(savedMonographPublication);

        return savedMonographPublication;
    }

    @Override
    @Transactional
    public List<DocumentPublicationIndex> findAuthorsPublicationsForMonograph(Integer monographId,
                                                                              Integer authorId) {
        return documentPublicationIndexRepository.findByTypeAndMonographIdAndAuthorIds(
            DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), monographId, authorId);
    }

    @Override
    @Transactional
    public Page<DocumentPublicationIndex> findAllPublicationsForMonograph(Integer monographId,
                                                                          Pageable pageable) {
        if (!SessionUtil.isUserLoggedIn()) {
            return documentPublicationIndexRepository.findByTypeAndMonographIdAndIsApprovedTrue(
                DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), monographId, pageable);
        }

        return documentPublicationIndexRepository.findByTypeAndMonographId(
            DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), monographId, pageable);
    }

    @Override
    @Transactional
    public void editMonographPublication(Integer monographPublicationId,
                                         MonographPublicationDTO monographPublicationDTO) {
        var monographPublicationToUpdate =
            monographPublicationJPAService.findOne(monographPublicationId);

        clearCommonFields(monographPublicationToUpdate);
        setCommonFields(monographPublicationToUpdate, monographPublicationDTO);
        setMonographPublicationRelatedFields(monographPublicationToUpdate, monographPublicationDTO);

        var monographPublicationIndex =
            findDocumentPublicationIndexByDatabaseId(monographPublicationId);
        indexMonographPublication(monographPublicationToUpdate, monographPublicationIndex);

        monographPublicationJPAService.save(monographPublicationToUpdate);

        sendNotifications(monographPublicationToUpdate);
    }

    @Override
    @Transactional
    public void deleteMonographPublication(Integer monographPublicationId) {
        var publicationToDelete = monographPublicationJPAService.findOne(monographPublicationId);

        monographPublicationJPAService.delete(publicationToDelete.getId());

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(monographPublicationId));
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexMonographPublications() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<MonographPublication> chunk =
                monographPublicationJPAService.findAll(
                        PageRequest.of(pageNumber, chunkSize, Sort.by(Sort.Direction.ASC, "id")))
                    .getContent();

            chunk.forEach((monographPublication) -> indexMonographPublication(monographPublication,
                new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void setMonographPublicationRelatedFields(MonographPublication monographPublication,
                                                      MonographPublicationDTO monographPublicationDTO) {
        monographPublication.setMonographPublicationType(
            monographPublicationDTO.getMonographPublicationType());
        monographPublication.setStartPage(monographPublicationDTO.getStartPage());
        monographPublication.setEndPage(monographPublicationDTO.getEndPage());
        monographPublication.setNumberOfPages(monographPublicationDTO.getNumberOfPages());
        monographPublication.setArticleNumber(monographPublicationDTO.getArticleNumber());

        var monograph =
            monographService.findMonographById(monographPublicationDTO.getMonographId());

        if (!monograph.getMonographType().equals(MonographType.BOOK)) {
            throw new NotFoundException("Book monograph with given ID does not exist.");
        }

        monographPublication.setMonograph(monograph);
        if (monographPublication.getMonographPublicationType()
            .equals(MonographPublicationType.CHAPTER)) {
            monographPublication.setDocumentDate(monograph.getDocumentDate());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void indexMonographPublication(MonographPublication monographPublication,
                                          DocumentPublicationIndex index) {
        indexCommonFields(monographPublication, index);

        index.setType(DocumentPublicationType.MONOGRAPH_PUBLICATION.name());

        if (Objects.nonNull(monographPublication.getMonographPublicationType())) {
            index.setPublicationType(monographPublication.getMonographPublicationType().name());
        }

        index.setMonographId(monographPublication.getMonograph().getId());

        index.setApa(
            citationService.craftCitationInGivenStyle("apa", index, LanguageAbbreviations.ENGLISH));
        documentPublicationIndexRepository.save(index);
    }

    @Override
    @Transactional
    public void indexMonographPublication(MonographPublication monographPublication) {
        indexMonographPublication(monographPublication,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                monographPublication.getId()).orElse(new DocumentPublicationIndex()));
    }
}
