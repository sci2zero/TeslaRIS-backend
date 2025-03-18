package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.repository.CommissionRepository;
import rs.teslaris.core.converter.document.PatentConverter;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.PatentJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PatentService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
@Transactional
public class PatentServiceImpl extends DocumentPublicationServiceImpl implements PatentService {

    private final PatentJPAServiceImpl patentJPAService;

    private final PublisherService publisherService;


    @Autowired
    public PatentServiceImpl(MultilingualContentService multilingualContentService,
                             DocumentPublicationIndexRepository documentPublicationIndexRepository,
                             SearchService<DocumentPublicationIndex> searchService,
                             OrganisationUnitService organisationUnitService,
                             DocumentRepository documentRepository,
                             DocumentFileService documentFileService,
                             PersonContributionService personContributionService,
                             ExpressionTransformer expressionTransformer, EventService eventService,
                             CommissionRepository commissionRepository,
                             PatentJPAServiceImpl patentJPAService,
                             PublisherService publisherService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService,
            personContributionService,
            expressionTransformer, eventService, commissionRepository);
        this.patentJPAService = patentJPAService;
        this.publisherService = publisherService;
    }

    @Override
    public PatentDTO readPatentById(Integer patentId) {
        var patent = patentJPAService.findOne(patentId);
        if (!patent.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return PatentConverter.toDTO(patent);
    }

    @Override
    public Patent createPatent(PatentDTO patentDTO, Boolean index) {
        var newPatent = new Patent();

        setCommonFields(newPatent, patentDTO);

        newPatent.setNumber(patentDTO.getNumber());
        if (Objects.nonNull(patentDTO.getPublisherId())) {
            newPatent.setPublisher(publisherService.findOne(patentDTO.getPublisherId()));
        }

        newPatent.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        var savedPatent = patentJPAService.save(newPatent);

        if (newPatent.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexPatent(savedPatent, new DocumentPublicationIndex());
        }

        sendNotifications(savedPatent);

        return savedPatent;
    }

    @Override
    public void editPatent(Integer patentId, PatentDTO patentDTO) {
        var patentToUpdate = patentJPAService.findOne(patentId);

        clearCommonFields(patentToUpdate);
        setCommonFields(patentToUpdate, patentDTO);

        patentToUpdate.setNumber(patentDTO.getNumber());
        if (Objects.nonNull(patentDTO.getPublisherId())) {
            patentToUpdate.setPublisher(
                publisherService.findOne(patentDTO.getPublisherId()));
        }

        var updatedPatent = patentJPAService.save(patentToUpdate);

        if (patentToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexPatent(patentToUpdate,
                documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        patentId)
                    .orElse(new DocumentPublicationIndex()));
        }

        sendNotifications(updatedPatent);
    }

    @Override
    public void deletePatent(Integer patentId) {
        var patentToDelete = patentJPAService.findOne(patentId);

        deleteProofsAndFileItems(patentToDelete);

        patentJPAService.delete(patentId);
        this.delete(patentId);
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexPatents() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Patent> chunk =
                patentJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((patent) -> indexPatent(patent, new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void indexPatent(Patent patent, DocumentPublicationIndex index) {
        indexCommonFields(patent, index);

        index.setType(DocumentPublicationType.PATENT.name());
        if (Objects.nonNull(patent.getPublisher())) {
            index.setPublisherId(patent.getPublisher().getId());
        }

        documentPublicationIndexRepository.save(index);
    }
}
