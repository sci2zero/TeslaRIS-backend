package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.repository.CommissionRepository;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.ProceedingsPublicationConverter;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ProceedingPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;

@Service
@Transactional
public class ProceedingsPublicationServiceImpl extends DocumentPublicationServiceImpl
    implements ProceedingsPublicationService {

    private final ProceedingPublicationJPAServiceImpl proceedingPublicationJPAService;

    private final ProceedingsService proceedingsService;

    private final ProceedingsPublicationRepository proceedingsPublicationRepository;

    private final ConferenceService conferenceService;


    @Autowired
    public ProceedingsPublicationServiceImpl(MultilingualContentService multilingualContentService,
                                             DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                             SearchService<DocumentPublicationIndex> searchService,
                                             OrganisationUnitService organisationUnitService,
                                             DocumentRepository documentRepository,
                                             DocumentFileService documentFileService,
                                             PersonContributionService personContributionService,
                                             ExpressionTransformer expressionTransformer,
                                             EventService eventService,
                                             CommissionRepository commissionRepository,
                                             SearchFieldsLoader searchFieldsLoader,
                                             ProceedingPublicationJPAServiceImpl proceedingPublicationJPAService,
                                             ProceedingsService proceedingsService,
                                             ProceedingsPublicationRepository proceedingsPublicationRepository,
                                             ConferenceService conferenceService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService,
            personContributionService,
            expressionTransformer, eventService, commissionRepository, searchFieldsLoader);
        this.proceedingPublicationJPAService = proceedingPublicationJPAService;
        this.proceedingsService = proceedingsService;
        this.proceedingsPublicationRepository = proceedingsPublicationRepository;
        this.conferenceService = conferenceService;
    }

    @Override
    public ProceedingsPublicationDTO readProceedingsPublicationById(Integer publicationId) {
        ProceedingsPublication publication;
        try {
            publication = proceedingPublicationJPAService.findOne(publicationId);
        } catch (NotFoundException e) {
            this.clearIndexWhenFailedRead(publicationId);
            throw e;
        }

        if (!publication.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return ProceedingsPublicationConverter.toDTO(publication);
    }

    @Override
    public List<ProceedingsPublicationResponseDTO> findAuthorsProceedingsForEvent(Integer eventId,
                                                                                  Integer authorId) {
        var proceedingsPublications =
            proceedingsPublicationRepository.findProceedingsPublicationsForEventId(eventId,
                authorId);
        return proceedingsPublications.stream().map(publication -> {
            var responseDTO = new ProceedingsPublicationResponseDTO();

            responseDTO.setPublicationId(publication.getId());
            responseDTO.setTitle(
                MultilingualContentConverter.getMultilingualContentDTO(publication.getTitle()));
            responseDTO.setProceedingsTitle(MultilingualContentConverter.getMultilingualContentDTO(
                publication.getProceedings().getTitle()));
            responseDTO.setDocumentDate(publication.getDocumentDate());

            return responseDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<DocumentPublicationIndex> findPublicationsInProceedings(Integer proceedingsId,
                                                                        Pageable pageable) {
        return documentPublicationIndexRepository.findByTypeAndProceedingsId(
            DocumentPublicationType.PROCEEDINGS_PUBLICATION.name(), proceedingsId, pageable);
    }

    @Override
    public ProceedingsPublication createProceedingsPublication(
        ProceedingsPublicationDTO proceedingsPublicationDTO, Boolean index) {
        var publication = new ProceedingsPublication();

        setCommonFields(publication, proceedingsPublicationDTO);
        setProceedingsPublicationRelatedFields(publication, proceedingsPublicationDTO);

        publication.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        var savedPublication = proceedingPublicationJPAService.save(publication);

        if (publication.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexProceedingsPublication(savedPublication, new DocumentPublicationIndex());
        }

        conferenceService.reindexConference(savedPublication.getEvent().getId());

        sendNotifications(savedPublication);

        return savedPublication;
    }

    @Override
    public void editProceedingsPublication(Integer publicationId,
                                           ProceedingsPublicationDTO publicationDTO) {
        var publicationToUpdate =
            (ProceedingsPublication) proceedingPublicationJPAService.findOne(publicationId);

        var oldConferenceId = publicationToUpdate.getEvent().getId();

        clearCommonFields(publicationToUpdate);
        publicationToUpdate.getUris().clear();

        setCommonFields(publicationToUpdate, publicationDTO);
        setProceedingsPublicationRelatedFields(publicationToUpdate, publicationDTO);

        if (publicationToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            var indexToUpdate = findDocumentPublicationIndexByDatabaseId(publicationId);
            indexProceedingsPublication(publicationToUpdate, indexToUpdate);
        }

        proceedingPublicationJPAService.save(publicationToUpdate);

        conferenceService.reindexVolatileConferenceInformation(
            publicationToUpdate.getEvent().getId());
        if (!publicationToUpdate.getEvent().getId().equals(oldConferenceId)) {
            conferenceService.reindexVolatileConferenceInformation(oldConferenceId);
        }

        sendNotifications(publicationToUpdate);
    }

    @Override
    public void deleteProceedingsPublication(Integer proceedingsPublicationId) {
//        var publicationToDelete =
//            (ProceedingsPublication) findDocumentById(proceedingsPublicationId);
//        TODO: check if this is needed because of soft delete
//        deleteProofsAndFileItems(publicationToDelete);

        proceedingPublicationJPAService.delete(proceedingsPublicationId);
        this.delete(proceedingsPublicationId);
    }

    @Override
    public void indexProceedingsPublication(ProceedingsPublication publication,
                                            DocumentPublicationIndex index) {
        indexCommonFields(publication, index);

        index.setEventId(publication.getProceedings().getEvent().getId());
        index.setType(DocumentPublicationType.PROCEEDINGS_PUBLICATION.name());

        if (Objects.nonNull(publication.getProceedingsPublicationType())) {
            index.setPublicationType(publication.getProceedingsPublicationType().name());
        }

        if (Objects.nonNull(publication.getProceedings())) {
            index.setProceedingsId(publication.getProceedings().getId());
        }

        documentPublicationIndexRepository.save(index);
    }

    @Override
    public Page<DocumentPublicationIndex> findProceedingsForEvent(Integer eventId,
                                                                  Pageable pageable) {
        return documentPublicationIndexRepository.findByTypeAndEventId(
            DocumentPublicationType.PROCEEDINGS_PUBLICATION.name(), eventId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexProceedingsPublications() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<ProceedingsPublication> chunk =
                proceedingPublicationJPAService.findAll(PageRequest.of(pageNumber, chunkSize))
                    .getContent();

            chunk.forEach((journalPublication) -> indexProceedingsPublication(journalPublication,
                new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void setProceedingsPublicationRelatedFields(ProceedingsPublication publication,
                                                        ProceedingsPublicationDTO publicationDTO) {
        publication.setProceedingsPublicationType(publicationDTO.getProceedingsPublicationType());
        publication.setStartPage(publicationDTO.getStartPage());
        publication.setEndPage(publicationDTO.getEndPage());
        publication.setNumberOfPages(publicationDTO.getNumberOfPages());
        publication.setArticleNumber(publicationDTO.getArticleNumber());
        publication.setProceedings(
            proceedingsService.findProceedingsById(publicationDTO.getProceedingsId()));

        if (Objects.isNull(publication.getDocumentDate()) ||
            publication.getDocumentDate().isEmpty()) {
            publication.setDocumentDate(publication.getProceedings().getDocumentDate());
        }
    }
}
