package rs.teslaris.core.service.impl.document;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.document.MonographPublicationConverter;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.MonographPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
@Transactional
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
                                           PersonContributionService personContributionService,
                                           ExpressionTransformer expressionTransformer,
                                           EventService eventService,
                                           MonographPublicationJPAServiceImpl monographPublicationJPAService,
                                           MonographService monographService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService,
            personContributionService,
            expressionTransformer, eventService);
        this.monographPublicationJPAService = monographPublicationJPAService;
        this.monographService = monographService;
    }

    @Override
    public MonographPublicationDTO readMonographPublicationById(Integer monographPublicationId) {
        var monographPublication = monographPublicationJPAService.findOne(monographPublicationId);

        if (monographPublication.getApproveStatus().equals(ApproveStatus.DECLINED)) {
            throw new NotFoundException(
                "Monograph with ID " + monographPublicationId + " does not exist.");
        }

        return MonographPublicationConverter.toDTO(monographPublication);
    }

    @Override
    public MonographPublication createMonographPublication(
        MonographPublicationDTO monographPublicationDTO, Boolean index) {
        var newMonographPublication = new MonographPublication();

        setCommonFields(newMonographPublication, monographPublicationDTO);
        setMonographPublicationRelatedFields(newMonographPublication, monographPublicationDTO);

        newMonographPublication.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        var savedMonographPublication =
            monographPublicationJPAService.save(newMonographPublication);

        if (newMonographPublication.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexMonographPublication(savedMonographPublication, new DocumentPublicationIndex());
        }

        sendNotifications(savedMonographPublication);

        return savedMonographPublication;
    }

    @Override
    public List<DocumentPublicationIndex> findAuthorsPublicationsForMonograph(Integer monographId,
                                                                              Integer authorId) {
        return documentPublicationIndexRepository.findByTypeAndMonographIdAndAuthorIds(
            DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), monographId, authorId);
    }

    @Override
    public Page<DocumentPublicationIndex> findAllPublicationsForMonograph(Integer monographId,
                                                                          Pageable pageable) {
        return documentPublicationIndexRepository.findByTypeAndMonographId(
            DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), monographId, pageable);
    }

    @Override
    public void updateMonographPublication(Integer monographPublicationId,
                                           MonographPublicationDTO monographPublicationDTO) {
        var monographPublicationToUpdate =
            monographPublicationJPAService.findOne(monographPublicationId);

        setCommonFields(monographPublicationToUpdate, monographPublicationDTO);
        setMonographPublicationRelatedFields(monographPublicationToUpdate, monographPublicationDTO);

        if (monographPublicationToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            var monographPublicationIndex =
                findDocumentPublicationIndexByDatabaseId(monographPublicationId);
            indexMonographPublication(monographPublicationToUpdate, monographPublicationIndex);
        }

        monographPublicationJPAService.save(monographPublicationToUpdate);

        sendNotifications(monographPublicationToUpdate);
    }

    @Override
    public void deleteMonographPublication(Integer monographPublicationId) {
        var publicationToDelete = monographPublicationJPAService.findOne(monographPublicationId);

        monographPublicationJPAService.delete(monographPublicationId);

        if (publicationToDelete.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            documentPublicationIndexRepository.delete(
                findDocumentPublicationIndexByDatabaseId(monographPublicationId));
        }
    }

    @Override
    public void reindexMonographPublications() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<MonographPublication> chunk =
                monographPublicationJPAService.findAll(PageRequest.of(pageNumber, chunkSize))
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

        monographPublication.setMonograph(
            monographService.findMonographById(monographPublicationDTO.getMonographId()));
    }

    private void indexMonographPublication(MonographPublication monographPublication,
                                           DocumentPublicationIndex index) {
        indexCommonFields(monographPublication, index);

        index.setType(DocumentPublicationType.MONOGRAPH_PUBLICATION.name());
        index.setMonographId(monographPublication.getMonograph().getId());

        documentPublicationIndexRepository.save(index);
    }
}
