package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.MaterialProductConverter;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.MaterialProductJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.MaterialProductService;
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
public class MaterialProductServiceImpl extends DocumentPublicationServiceImpl implements
    MaterialProductService {

    private final MaterialProductJPAServiceImpl materialProductJPAService;

    private final PublisherService publisherService;

    private final ResearchAreaService researchAreaService;


    @Autowired
    public MaterialProductServiceImpl(
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
        MaterialProductJPAServiceImpl materialProductJPAService,
        PublisherService publisherService, ResearchAreaService researchAreaService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService);
        this.materialProductJPAService = materialProductJPAService;
        this.publisherService = publisherService;
        this.researchAreaService = researchAreaService;
    }

    @Override
    @Transactional
    public MaterialProduct findMaterialProductById(Integer materialProductId) {
        return materialProductJPAService.findOne(materialProductId);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialProductDTO readMaterialProductById(Integer materialProductId) {
        MaterialProduct materialProduct;
        try {
            materialProduct = materialProductJPAService.findOne(materialProductId);
        } catch (NotFoundException e) {
            log.info("Trying to read non-existent MATERIAL_PRODUCT with ID {}. Clearing index.",
                materialProductId);
            this.clearIndexWhenFailedRead(materialProductId,
                DocumentPublicationType.MATERIAL_PRODUCT);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !materialProduct.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return MaterialProductConverter.toDTO(materialProduct);
    }

    @Override
    @Transactional
    public MaterialProduct createMaterialProduct(MaterialProductDTO materialProductDTO,
                                                 Boolean index) {
        var newProduct = new MaterialProduct();

        checkForDocumentDate(materialProductDTO);
        setCommonFields(newProduct, materialProductDTO);
        setMaterialProductRelatedFields(newProduct, materialProductDTO);

        var savedProduct = materialProductJPAService.save(newProduct);

        if (index) {
            indexMaterialProduct(savedProduct, new DocumentPublicationIndex());
        }

        sendNotifications(savedProduct);

        return savedProduct;
    }

    @Override
    @Transactional
    public void editMaterialProduct(Integer materialProductId,
                                    MaterialProductDTO materialProductDTO) {
        var materialProductToUpdate = materialProductJPAService.findOne(materialProductId);

        checkForDocumentDate(materialProductDTO);
        clearCommonFields(materialProductToUpdate);
        setCommonFields(materialProductToUpdate, materialProductDTO);

        materialProductToUpdate.getResearchAreas().clear();
        setMaterialProductRelatedFields(materialProductToUpdate, materialProductDTO);

        materialProductJPAService.save(materialProductToUpdate);

        indexMaterialProduct(materialProductToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                    materialProductId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(materialProductToUpdate);
    }

    @Override
    @Transactional
    public void deleteMaterialProduct(Integer materialProductId) {
        var materialProductToDelete = materialProductJPAService.findOne(materialProductId);

        deleteProofsAndFileItems(materialProductToDelete);

        materialProductJPAService.delete(materialProductId);
        save(materialProductToDelete);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(materialProductId));
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexMaterialProducts() {
        // Super service does the initial deletion

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            materialProductJPAService::findAll,
            materialProduct ->
                indexMaterialProduct(materialProduct, new DocumentPublicationIndex())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void indexMaterialProduct(MaterialProduct materialProduct) {
        indexMaterialProduct(materialProduct,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                materialProduct.getId()).orElse(new DocumentPublicationIndex()));
    }

    private void setMaterialProductRelatedFields(MaterialProduct materialProduct,
                                                 MaterialProductDTO materialProductDTO) {
        materialProduct.setInternalNumber(materialProductDTO.getInternalNumber());
        materialProduct.setNumberProduced(materialProductDTO.getNumberProduced());
        materialProduct.setMaterialProductType(materialProductDTO.getMaterialProductType());
        materialProduct.setProductUsers(multilingualContentService.getMultilingualContent(
            materialProductDTO.getProductUsers()));

        materialProduct.setPublisher(null);
        materialProduct.setAuthorReprint(false);

        if (Objects.nonNull(materialProductDTO.getAuthorReprint()) &&
            materialProductDTO.getAuthorReprint()) {
            materialProduct.setAuthorReprint(true);
        } else if (Objects.nonNull(materialProductDTO.getPublisherId())) {
            materialProduct.setPublisher(
                publisherService.findOne(materialProductDTO.getPublisherId()));
        }

        var researchAreas = researchAreaService.getResearchAreasByIds(
            materialProductDTO.getResearchAreasId().stream().toList());
        materialProduct.setResearchAreas(new HashSet<>(researchAreas));
    }

    private void indexMaterialProduct(MaterialProduct materialProduct,
                                      DocumentPublicationIndex index) {
        indexCommonFields(materialProduct, index);

        index.setType(DocumentPublicationType.MATERIAL_PRODUCT.name());
        if (Objects.nonNull(materialProduct.getPublisher())) {
            index.setPublisherId(materialProduct.getPublisher().getId());
        }
        index.setAuthorReprint(materialProduct.getAuthorReprint());

        index.setApa(
            citationService.craftCitationInGivenStyle("apa", index, LanguageAbbreviations.ENGLISH));
        documentPublicationIndexRepository.save(index);
    }
}
