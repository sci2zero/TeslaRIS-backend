package rs.teslaris.core.service.impl.document;

import javax.transaction.Transactional;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.JournalPublicationConverter;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;

@Service
@Transactional
public class JournalPublicationServiceImpl extends DocumentPublicationServiceImpl
    implements JournalPublicationService {

    private final JournalService journalService;

    private final JournalPublicationRepository journalPublicationRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    public JournalPublicationServiceImpl(MultilingualContentService multilingualContentService,
                                         DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                         DocumentRepository documentRepository,
                                         DocumentFileService documentFileService,
                                         PersonContributionService personContributionService,
                                         JournalService journalService,
                                         JournalPublicationRepository journalPublicationRepository,
                                         DocumentPublicationIndexRepository documentPublicationIndexRepository1) {
        super(multilingualContentService, documentPublicationIndexRepository, documentRepository,
            documentFileService, personContributionService);
        this.journalService = journalService;
        this.journalPublicationRepository = journalPublicationRepository;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository1;
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

        var savedPublication = journalPublicationRepository.save(publication);

        if (publication.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexJournalPublication(savedPublication, new DocumentPublicationIndex());
        }

        return savedPublication;
    }

    @Override
    public void editJournalPublication(Integer publicationId,
                                       JournalPublicationDTO publicationDTO) {
        var publicationToUpdate = (JournalPublication) findDocumentById(publicationId);

        clearCommonFields(publicationToUpdate);
        publicationToUpdate.getUris().clear();

        setCommonFields(publicationToUpdate, publicationDTO);
        setJournalPublicationRelatedFields(publicationToUpdate, publicationDTO);

        if (publicationToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            var indexToUpdate = findDocumentPublicationIndexByDatabaseId(publicationId);
            indexJournalPublication(publicationToUpdate, indexToUpdate);
        }

        journalPublicationRepository.save(publicationToUpdate);
    }

    @Override
    public void deleteJournalPublication(Integer journalPublicationId) {
        var publicationToDelete = (JournalPublication) findDocumentById(journalPublicationId);

        deleteProofsAndFileItems(publicationToDelete);
        journalPublicationRepository.delete(publicationToDelete);
        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(journalPublicationId));
    }

    @Override
    public void indexJournalPublication(JournalPublication publication,
                                        DocumentPublicationIndex index) {
        indexCommonFields(publication, index);

        index.setJournalId(publication.getJournal().getId());

        documentPublicationIndexRepository.save(index);
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
