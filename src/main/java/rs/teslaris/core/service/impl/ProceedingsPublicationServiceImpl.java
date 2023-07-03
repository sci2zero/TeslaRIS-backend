package rs.teslaris.core.service.impl;

import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.ProceedingsPublicationConverter;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.PersonContributionService;
import rs.teslaris.core.service.ProceedingsPublicationService;
import rs.teslaris.core.service.ProceedingsService;

@Service
@Transactional
public class ProceedingsPublicationServiceImpl extends DocumentPublicationServiceImpl
    implements ProceedingsPublicationService {

    private final ProceedingsService proceedingsService;

    private final ProceedingsPublicationRepository proceedingsPublicationRepository;


    @Autowired
    public ProceedingsPublicationServiceImpl(DocumentRepository documentRepository,
                                             DocumentFileService documentFileService,
                                             MultilingualContentService multilingualContentService,
                                             PersonContributionService personContributionService,
                                             ProceedingsService proceedingsService,
                                             ProceedingsPublicationRepository proceedingsPublicationRepository) {
        super(multilingualContentService, documentRepository, documentFileService,
            personContributionService);
        this.proceedingsService = proceedingsService;
        this.proceedingsPublicationRepository = proceedingsPublicationRepository;
    }

    @Override
    public ProceedingsPublicationDTO readProceedingsPublicationById(Integer proceedingsId) {
        var publication = (ProceedingsPublication) findDocumentById(proceedingsId);
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

        return proceedingsPublicationRepository.save(publication);
    }

    @Override
    public void editProceedingsPublication(Integer publicationId,
                                           ProceedingsPublicationDTO publicationDTO) {
        var publicationToUpdate = (ProceedingsPublication) findDocumentById(publicationId);

        clearCommonFields(publicationToUpdate);
        publicationToUpdate.getUris().clear();

        setCommonFields(publicationToUpdate, publicationDTO);
        setProceedingsPublicationRelatedFields(publicationToUpdate, publicationDTO);

        proceedingsPublicationRepository.save(publicationToUpdate);
    }

    @Override
    public void deleteProceedingsPublication(Integer proceedingsPublicationId) {
        var publicationToDelete =
            (ProceedingsPublication) findDocumentById(proceedingsPublicationId);

        deleteProofsAndFileItems(publicationToDelete);
        proceedingsPublicationRepository.delete(publicationToDelete);
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
