package rs.teslaris.core.service.impl.document;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.GeneticMaterialConverter;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.GeneticMaterial;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.GeneticMaterialJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.GeneticMaterialService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.session.SessionUtil;

@Service
@Traceable
@Slf4j
public class GeneticMaterialServiceImpl extends DocumentPublicationServiceImpl implements
    GeneticMaterialService {

    private final GeneticMaterialJPAServiceImpl geneticMaterialJPAService;

    private final PublisherService publisherService;


    @Autowired
    public GeneticMaterialServiceImpl(
        MultilingualContentService multilingualContentService,
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
        GeneticMaterialJPAServiceImpl geneticMaterialJPAService,
        PublisherService publisherService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService);
        this.geneticMaterialJPAService = geneticMaterialJPAService;
        this.publisherService = publisherService;
    }

    @Override
    @Transactional
    public GeneticMaterial findGeneticMaterialById(Integer geneticMaterialId) {
        return geneticMaterialJPAService.findOne(geneticMaterialId);
    }

    @Override
    @Transactional(readOnly = true)
    public GeneticMaterialDTO readGeneticMaterialById(Integer geneticMaterialId) {
        GeneticMaterial geneticMaterial;
        try {
            geneticMaterial = geneticMaterialJPAService.findOne(geneticMaterialId);
        } catch (NotFoundException e) {
            log.info("Trying to read non-existent MATERIAL_PRODUCT with ID {}. Clearing index.",
                geneticMaterialId);
            this.clearIndexWhenFailedRead(geneticMaterialId,
                DocumentPublicationType.MATERIAL_PRODUCT);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !geneticMaterial.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return GeneticMaterialConverter.toDTO(geneticMaterial);
    }

    @Override
    @Transactional
    public GeneticMaterial createGeneticMaterial(GeneticMaterialDTO geneticMaterialDTO,
                                                 Boolean index) {
        var newProduct = new GeneticMaterial();

        checkForDocumentDate(geneticMaterialDTO);
        setCommonFields(newProduct, geneticMaterialDTO);
        setGeneticMaterialRelatedFields(newProduct, geneticMaterialDTO);

        var savedProduct = geneticMaterialJPAService.save(newProduct);

        if (index) {
            indexGeneticMaterial(savedProduct, new DocumentPublicationIndex());
        }

        sendNotifications(savedProduct);

        return savedProduct;
    }

    @Override
    @Transactional
    public void editGeneticMaterial(Integer geneticMaterialId,
                                    GeneticMaterialDTO geneticMaterialDTO) {
        var geneticMaterialToUpdate = geneticMaterialJPAService.findOne(geneticMaterialId);

        checkForDocumentDate(geneticMaterialDTO);
        clearCommonFields(geneticMaterialToUpdate);
        setCommonFields(geneticMaterialToUpdate, geneticMaterialDTO);

        setGeneticMaterialRelatedFields(geneticMaterialToUpdate, geneticMaterialDTO);

        geneticMaterialJPAService.save(geneticMaterialToUpdate);

        indexGeneticMaterial(geneticMaterialToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                    geneticMaterialId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(geneticMaterialToUpdate);
    }

    @Override
    @Transactional
    public void deleteGeneticMaterial(Integer geneticMaterialId) {
        var geneticMaterialToDelete = geneticMaterialJPAService.findOne(geneticMaterialId);

        deleteProofsAndFileItems(geneticMaterialToDelete);

        geneticMaterialJPAService.delete(geneticMaterialId);
        save(geneticMaterialToDelete);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(geneticMaterialId));
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexGeneticMaterials() {
        // Super service does the initial deletion

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            geneticMaterialJPAService::findAll,
            geneticMaterial ->
                indexGeneticMaterial(geneticMaterial, new DocumentPublicationIndex())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void indexGeneticMaterial(GeneticMaterial geneticMaterial) {
        indexGeneticMaterial(geneticMaterial,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                geneticMaterial.getId()).orElse(new DocumentPublicationIndex()));
    }

    private void setGeneticMaterialRelatedFields(GeneticMaterial geneticMaterial,
                                                 GeneticMaterialDTO geneticMaterialDTO) {
        geneticMaterial.setInternalNumber(geneticMaterialDTO.getInternalNumber());
        geneticMaterial.setGeneticMaterialType(geneticMaterialDTO.getGeneticMaterialType());

        geneticMaterial.setPublisher(null);
        geneticMaterial.setAuthorReprint(false);

        if (Objects.nonNull(geneticMaterialDTO.getAuthorReprint()) &&
            geneticMaterialDTO.getAuthorReprint()) {
            geneticMaterial.setAuthorReprint(true);
        } else if (Objects.nonNull(geneticMaterialDTO.getPublisherId())) {
            geneticMaterial.setPublisher(
                publisherService.findOne(geneticMaterialDTO.getPublisherId()));
        }
    }

    private void indexGeneticMaterial(GeneticMaterial geneticMaterial,
                                      DocumentPublicationIndex index) {
        indexCommonFields(geneticMaterial, index);

        index.setType(DocumentPublicationType.GENETIC_MATERIAL.name());
        if (Objects.nonNull(geneticMaterial.getPublisher())) {
            index.setPublisherId(geneticMaterial.getPublisher().getId());
        }
        index.setAuthorReprint(geneticMaterial.getAuthorReprint());

        index.setApa(
            citationService.craftCitationInGivenStyle(
                "apa", index, LanguageAbbreviations.ENGLISH)
        );

        documentPublicationIndexRepository.save(index);
    }
}

