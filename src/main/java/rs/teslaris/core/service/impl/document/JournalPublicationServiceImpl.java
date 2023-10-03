package rs.teslaris.core.service.impl.document;

import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.JournalPublicationConverter;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.JournalPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
@Transactional
public class JournalPublicationServiceImpl extends DocumentPublicationServiceImpl
    implements JournalPublicationService {

    private final JournalPublicationJPAServiceImpl journalPublicationJPAService;

    private final JournalService journalService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    @Autowired
    public JournalPublicationServiceImpl(MultilingualContentService multilingualContentService,
                                         DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                         DocumentRepository documentRepository,
                                         DocumentFileService documentFileService,
                                         PersonContributionService personContributionService,
                                         SearchService<DocumentPublicationIndex> searchService,
                                         ExpressionTransformer expressionTransformer,
                                         EventService eventService,
                                         JournalPublicationJPAServiceImpl journalPublicationJPAService,
                                         JournalService journalService,
                                         DocumentPublicationIndexRepository documentPublicationIndexRepository1) {
        super(multilingualContentService, documentPublicationIndexRepository, documentRepository,
            documentFileService,
            personContributionService, searchService, expressionTransformer, eventService);
        this.journalPublicationJPAService = journalPublicationJPAService;
        this.journalService = journalService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository1;
    }

    @Override
    public JournalPublicationResponseDTO readJournalPublicationById(Integer publicationId) {
        var publication = (JournalPublication) findOne(publicationId);
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

        var savedPublication = journalPublicationJPAService.save(publication);

        if (publication.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexJournalPublication(savedPublication, new DocumentPublicationIndex());
        }

        return savedPublication;
    }

    @Override
    public void editJournalPublication(Integer publicationId,
                                       JournalPublicationDTO publicationDTO) {
        var publicationToUpdate = (JournalPublication) findOne(publicationId);

        clearCommonFields(publicationToUpdate);
        publicationToUpdate.getUris().clear();

        setCommonFields(publicationToUpdate, publicationDTO);
        setJournalPublicationRelatedFields(publicationToUpdate, publicationDTO);

        if (publicationToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            var indexToUpdate = findDocumentPublicationIndexByDatabaseId(publicationId);
            indexJournalPublication(publicationToUpdate, indexToUpdate);
        }

        journalPublicationJPAService.save(publicationToUpdate);
    }

    @Override
    public void deleteJournalPublication(Integer journalPublicationId) {
        var publicationToDelete = (JournalPublication) findOne(journalPublicationId);

        deleteProofsAndFileItems(publicationToDelete);
        delete(journalPublicationId);
        this.delete(journalPublicationId);
    }

    @Override
    public void indexJournalPublication(JournalPublication publication,
                                        DocumentPublicationIndex index) {
        indexCommonFields(publication, index);

        index.setPublicationSeriesId(publication.getJournal().getId());

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

        publication.setJournal(journalService.findOne(publicationDTO.getJournalId()));
    }
}
