package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.SoftwareConverter;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.SoftwareJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.document.SoftwareService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
public class SoftwareServiceImpl extends DocumentPublicationServiceImpl implements SoftwareService {

    private final SoftwareJPAServiceImpl softwareJPAService;

    private final PublisherService publisherService;


    @Autowired
    public SoftwareServiceImpl(MultilingualContentService multilingualContentService,
                               DocumentPublicationIndexRepository documentPublicationIndexRepository,
                               DocumentRepository documentRepository,
                               DocumentFileService documentFileService,
                               PersonContributionService personContributionService,
                               SearchService<DocumentPublicationIndex> searchService,
                               ExpressionTransformer expressionTransformer,
                               EventService eventService,
                               OrganisationUnitService organisationUnitService,
                               SoftwareJPAServiceImpl softwareJPAService,
                               PublisherService publisherService) {
        super(multilingualContentService, documentPublicationIndexRepository, documentRepository,
            documentFileService, personContributionService, searchService, expressionTransformer,
            eventService, organisationUnitService);
        this.softwareJPAService = softwareJPAService;
        this.publisherService = publisherService;
    }

    @Override
    public SoftwareDTO readSoftwareById(Integer softwareId) {
        return SoftwareConverter.toDTO(softwareJPAService.findOne(softwareId));
    }

    @Override
    public Software createSoftware(SoftwareDTO softwareDTO, Boolean index) {
        var newSoftware = new Software();

        setCommonFields(newSoftware, softwareDTO);

        newSoftware.setInternalNumber(softwareDTO.getInternalNumber());
        if (Objects.nonNull(softwareDTO.getPublisherId())) {
            newSoftware.setPublisher(
                publisherService.findPublisherById(softwareDTO.getPublisherId()));
        }

        newSoftware.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        var savedSoftware = softwareJPAService.save(newSoftware);

        if (newSoftware.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexSoftware(savedSoftware, new DocumentPublicationIndex());
        }

        sendNotifications(savedSoftware);

        return savedSoftware;
    }

    @Override
    public void editSoftware(Integer softwareId, SoftwareDTO softwareDTO) {
        var softwareToUpdate = softwareJPAService.findOne(softwareId);

        clearCommonFields(softwareToUpdate);
        setCommonFields(softwareToUpdate, softwareDTO);

        softwareToUpdate.setInternalNumber(softwareDTO.getInternalNumber());
        if (Objects.nonNull(softwareDTO.getPublisherId())) {
            softwareToUpdate.setPublisher(
                publisherService.findPublisherById(softwareDTO.getPublisherId()));
        }

        softwareJPAService.save(softwareToUpdate);

        if (softwareToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexSoftware(softwareToUpdate,
                documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        softwareId)
                    .orElse(new DocumentPublicationIndex()));
        }

        sendNotifications(softwareToUpdate);
    }

    @Override
    public void deleteSoftware(Integer softwareId) {
        var softwareToDelete = softwareJPAService.findOne(softwareId);

        deleteProofsAndFileItems(softwareToDelete);

        softwareJPAService.delete(softwareId);
        this.delete(softwareId);
    }

    @Override
    public void reindexSoftware() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Software> chunk =
                softwareJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((software) -> indexSoftware(software, new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void indexSoftware(Software software, DocumentPublicationIndex index) {
        indexCommonFields(software, index);

        index.setType(DocumentPublicationType.SOFTWARE.name());
        if (Objects.nonNull(software.getPublisher())) {
            index.setPublisherId(software.getPublisher().getId());
        }

        documentPublicationIndexRepository.save(index);
    }
}
