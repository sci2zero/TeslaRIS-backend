package rs.teslaris.core.service.impl;

import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.JournalPublicationConverter;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.JournalPublicationService;
import rs.teslaris.core.service.JournalService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.PersonContributionService;

@Service
@Transactional
public class JournalPublicationServiceImpl extends DocumentPublicationServiceImpl
    implements JournalPublicationService {

    private final JournalService journalService;

    private final JournalPublicationRepository journalPublicationRepository;


    @Autowired
    public JournalPublicationServiceImpl(DocumentRepository documentRepository,
                                         DocumentFileService documentFileService,
                                         PersonContributionService personContributionService,
                                         MultilingualContentService multilingualContentService,
                                         JournalService journalService,
                                         JournalPublicationRepository journalPublicationRepository) {
        super(multilingualContentService, documentRepository, documentFileService,
            personContributionService);
        this.journalService = journalService;
        this.journalPublicationRepository = journalPublicationRepository;
    }

    @Override
    public JournalPublicationResponseDTO readJournalPublicationById(Integer publicationId) {
        var publication = (JournalPublication) findDocumentById(publicationId);
        if (!publication.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }
        return JournalPublicationConverter.toDTO(publication);
    }

    @Override
    public JournalPublication createJournalPublication(JournalPublicationDTO publicationDTO) {
        var publication = new JournalPublication();

        setCommonFields(publication, publicationDTO);
        setJournalPublicationRelatedFields(publication, publicationDTO);

        publication.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        return journalPublicationRepository.save(publication);
    }

    @Override
    public void editJournalPublication(Integer publicationId,
                                       JournalPublicationDTO publicationDTO) {
        var publicationToUpdate = (JournalPublication) findDocumentById(publicationId);

        clearCommonFields(publicationToUpdate);
        publicationToUpdate.getUris().clear();

        setCommonFields(publicationToUpdate, publicationDTO);
        setJournalPublicationRelatedFields(publicationToUpdate, publicationDTO);

        journalPublicationRepository.save(publicationToUpdate);
    }

    @Override
    public void deleteJournalPublication(Integer journalPublicationId) {
        var publicationToDelete = (JournalPublication) findDocumentById(journalPublicationId);

        deleteProofsAndFileItems(publicationToDelete);
        journalPublicationRepository.delete(publicationToDelete);
    }

    private void setJournalPublicationRelatedFields(JournalPublication publication,
                                                    JournalPublicationDTO publicationDTO) {
        publication.setJournalPublicationType(publicationDTO.getJournalPublicationType());
        publication.setStartPage(publicationDTO.getStartPage());
        publication.setEndPage(publicationDTO.getEndPage());
        publication.setNumberOfPages(publicationDTO.getNumberOfPages());
        publication.setArticleNumber(publicationDTO.getArticleNumber());
        publication.setVolume(publicationDTO.getVolume());
        publication.setIssue(publicationDTO.getIssue());

        publication.setJournal(journalService.findJournalById(publicationDTO.getJournalId()));
    }
}
