package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.DatasetConverter;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.DatasetJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DatasetService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.session.SessionUtil;

@Service
@Traceable
@Transactional
public class DatasetServiceImpl extends DocumentPublicationServiceImpl implements DatasetService {

    private final DatasetJPAServiceImpl datasetJPAService;

    private final PublisherService publisherService;


    @Autowired
    public DatasetServiceImpl(MultilingualContentService multilingualContentService,
                              DocumentPublicationIndexRepository documentPublicationIndexRepository,
                              SearchService<DocumentPublicationIndex> searchService,
                              OrganisationUnitService organisationUnitService,
                              DocumentRepository documentRepository,
                              DocumentFileService documentFileService,
                              CitationService citationService,
                              ApplicationEventPublisher applicationEventPublisher,
                              PersonContributionService personContributionService,
                              ExpressionTransformer expressionTransformer,
                              EventService eventService,
                              CommissionRepository commissionRepository,
                              SearchFieldsLoader searchFieldsLoader,
                              OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService,
                              InvolvementRepository involvementRepository,
                              OrganisationUnitOutputConfigurationService organisationUnitOutputConfigurationService,
                              DatasetJPAServiceImpl datasetJPAService,
                              PublisherService publisherService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService);
        this.datasetJPAService = datasetJPAService;
        this.publisherService = publisherService;
    }

    @Override
    public Dataset findDatasetById(Integer datasetId) {
        return datasetJPAService.findOne(datasetId);
    }

    @Override
    public DatasetDTO readDatasetById(Integer datasetId) {
        Dataset dataset;
        try {
            dataset = datasetJPAService.findOne(datasetId);
        } catch (NotFoundException e) {
            this.clearIndexWhenFailedRead(datasetId, DocumentPublicationType.DATASET);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !dataset.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return DatasetConverter.toDTO(dataset);
    }

    @Override
    public Dataset createDataset(DatasetDTO datasetDTO, Boolean index) {
        var newDataset = new Dataset();

        checkForDocumentDate(datasetDTO);
        setCommonFields(newDataset, datasetDTO);
        setDatasetRelatedFields(newDataset, datasetDTO);

        var savedDataset = datasetJPAService.save(newDataset);

        if (index) {
            indexDataset(savedDataset, new DocumentPublicationIndex());
        }

        sendNotifications(savedDataset);

        return savedDataset;
    }

    @Override
    public void editDataset(Integer datasetId, DatasetDTO datasetDTO) {
        var datasetToUpdate = datasetJPAService.findOne(datasetId);

        checkForDocumentDate(datasetDTO);
        clearCommonFields(datasetToUpdate);
        setCommonFields(datasetToUpdate, datasetDTO);
        setDatasetRelatedFields(datasetToUpdate, datasetDTO);

        datasetJPAService.save(datasetToUpdate);

        indexDataset(datasetToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(datasetId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(datasetToUpdate);
    }

    private void setDatasetRelatedFields(Dataset dataset, DatasetDTO datasetDTO) {
        dataset.setInternalNumber(datasetDTO.getInternalNumber());

        dataset.setPublisher(null);
        dataset.setAuthorReprint(false);

        if (Objects.nonNull(datasetDTO.getAuthorReprint()) && datasetDTO.getAuthorReprint()) {
            dataset.setAuthorReprint(true);
        } else if (Objects.nonNull(datasetDTO.getPublisherId())) {
            dataset.setPublisher(publisherService.findOne(datasetDTO.getPublisherId()));
        }
    }

    @Override
    public void deleteDataset(Integer datasetId) {
        var datasetToDelete = datasetJPAService.findOne(datasetId);

        deleteProofsAndFileItems(datasetToDelete);

        datasetJPAService.delete(datasetId);
        this.delete(datasetId);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(datasetId));
    }

    @Override
    public void reindexDatasets() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Dataset> chunk =
                datasetJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((dataset) -> indexDataset(dataset, new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    @Override
    public void indexDataset(Dataset dataset) {
        indexDataset(dataset,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                dataset.getId()).orElse(new DocumentPublicationIndex()));
    }

    private void indexDataset(Dataset dataset, DocumentPublicationIndex index) {
        indexCommonFields(dataset, index);

        index.setType(DocumentPublicationType.DATASET.name());
        if (Objects.nonNull(dataset.getPublisher())) {
            index.setPublisherId(dataset.getPublisher().getId());
        }
        index.setAuthorReprint(dataset.getAuthorReprint());

        index.setApa(
            citationService.craftCitationInGivenStyle("apa", index, LanguageAbbreviations.ENGLISH));
        documentPublicationIndexRepository.save(index);
    }
}
