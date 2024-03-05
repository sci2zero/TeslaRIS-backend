package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.DatasetConverter;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.DatasetJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DatasetService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
public class DatasetServiceImpl extends DocumentPublicationServiceImpl implements DatasetService {

    private final DatasetJPAServiceImpl datasetJPAService;

    private final PublisherService publisherService;


    @Autowired
    public DatasetServiceImpl(
        MultilingualContentService multilingualContentService,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        DocumentRepository documentRepository,
        DocumentFileService documentFileService,
        PersonContributionService personContributionService,
        SearchService<DocumentPublicationIndex> searchService,
        ExpressionTransformer expressionTransformer,
        EventService eventService, DatasetJPAServiceImpl datasetJPAService,
        PublisherService publisherService) {
        super(multilingualContentService, documentPublicationIndexRepository, documentRepository,
            documentFileService, personContributionService, searchService, expressionTransformer,
            eventService);
        this.datasetJPAService = datasetJPAService;
        this.publisherService = publisherService;
    }

    @Override
    public DatasetDTO readDatasetById(Integer datasetId) {
        return DatasetConverter.toDTO(datasetJPAService.findOne(datasetId));
    }

    @Override
    public Dataset createDataset(DatasetDTO datasetDTO, Boolean index) {
        var newDataset = new Dataset();

        setCommonFields(newDataset, datasetDTO);

        newDataset.setInternalNumber(datasetDTO.getInternalNumber());
        if (Objects.nonNull(datasetDTO.getPublisherId())) {
            newDataset.setPublisher(
                publisherService.findPublisherById(datasetDTO.getPublisherId()));
        }

        newDataset.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        var savedDataset = datasetJPAService.save(newDataset);

        if (newDataset.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexDataset(savedDataset, new DocumentPublicationIndex());
        }

        return savedDataset;
    }

    @Override
    public void editDataset(Integer datasetId, DatasetDTO datasetDTO) {
        var datasetToUpdate = datasetJPAService.findOne(datasetId);

        clearCommonFields(datasetToUpdate);
        setCommonFields(datasetToUpdate, datasetDTO);

        datasetToUpdate.setInternalNumber(datasetDTO.getInternalNumber());
        if (Objects.nonNull(datasetDTO.getPublisherId())) {
            datasetToUpdate.setPublisher(
                publisherService.findPublisherById(datasetDTO.getPublisherId()));
        }

        datasetJPAService.save(datasetToUpdate);

        if (datasetToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexDataset(datasetToUpdate,
                documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        datasetId)
                    .orElse(new DocumentPublicationIndex()));
        }
    }

    @Override
    public void deleteDataset(Integer datasetId) {
        var datasetToDelete = datasetJPAService.findOne(datasetId);

        deleteProofsAndFileItems(datasetToDelete);

        datasetJPAService.delete(datasetId);
        this.delete(datasetId);
    }

    @Override
    public void reindexDatasets() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Dataset> chunk =
                datasetJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((dataset) -> indexDataset(dataset, new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void indexDataset(Dataset dataset, DocumentPublicationIndex index) {
        indexCommonFields(dataset, index);

        index.setType(DocumentPublicationType.DATASET.name());

        documentPublicationIndexRepository.save(index);
    }
}
