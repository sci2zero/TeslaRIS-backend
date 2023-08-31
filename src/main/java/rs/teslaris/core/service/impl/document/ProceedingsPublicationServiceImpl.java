package rs.teslaris.core.service.impl.document;

import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.ProceedingsPublicationConverter;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ProceedingPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@Transactional
public class ProceedingsPublicationServiceImpl extends DocumentPublicationServiceImpl
    implements ProceedingsPublicationService {

    private final ProceedingPublicationJPAServiceImpl proceedingPublicationJPAService;
    private final ProceedingsService proceedingsService;

    private final ProceedingsPublicationRepository proceedingsPublicationRepository;

    @Autowired
    public ProceedingsPublicationServiceImpl(MultilingualContentService multilingualContentService,
                                             DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                             DocumentRepository documentRepository,
                                             DocumentFileService documentFileService,
                                             PersonContributionService personContributionService,
                                             SearchService<DocumentPublicationIndex> searchService,
                                             EventService eventService,
                                             ProceedingPublicationJPAServiceImpl proceedingPublicationJPAService,
                                             ProceedingsService proceedingsService,
                                             ProceedingsPublicationRepository proceedingsPublicationRepository) {
        super(multilingualContentService, documentPublicationIndexRepository, documentRepository,
            documentFileService, personContributionService, searchService, eventService);
        this.proceedingPublicationJPAService = proceedingPublicationJPAService;
        this.proceedingsService = proceedingsService;
        this.proceedingsPublicationRepository = proceedingsPublicationRepository;
    }


    @Override
    public ProceedingsPublicationDTO readProceedingsPublicationById(Integer proceedingsId) {
        var publication = (ProceedingsPublication) this.findOne(proceedingsId);
        if (!publication.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }
        return ProceedingsPublicationConverter.toDTO(publication);
    }

    @Override
    public ProceedingsPublication createProceedingsPublication(
        ProceedingsPublicationDTO proceedingsPublicationDTO) {
        var publication = new ProceedingsPublication();

        setCommonFields(publication, proceedingsPublicationDTO);
        setProceedingsPublicationRelatedFields(publication, proceedingsPublicationDTO);

        publication.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        var savedPublication = proceedingPublicationJPAService.save(publication);

        if (publication.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexProceedingsPublication(savedPublication, new DocumentPublicationIndex());
        }

        return savedPublication;
    }

    @Override
    public void editProceedingsPublication(Integer publicationId,
                                           ProceedingsPublicationDTO publicationDTO) {
        var publicationToUpdate = (ProceedingsPublication) this.findOne(publicationId);

        clearCommonFields(publicationToUpdate);
        publicationToUpdate.getUris().clear();

        setCommonFields(publicationToUpdate, publicationDTO);
        setProceedingsPublicationRelatedFields(publicationToUpdate, publicationDTO);

        if (publicationToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            var indexToUpdate = findDocumentPublicationIndexByDatabaseId(publicationId);
            indexProceedingsPublication(publicationToUpdate, indexToUpdate);
        }

        proceedingPublicationJPAService.save(publicationToUpdate);
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

        documentPublicationIndexRepository.save(index);
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
    }
}
