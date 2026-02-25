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
import rs.teslaris.core.converter.document.IntangibleProductConverter;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.IntangibleProduct;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.IntangibleProductJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.IntangibleProductService;
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
public class IntangibleProductServiceImpl extends DocumentPublicationServiceImpl implements
    IntangibleProductService {

    private final IntangibleProductJPAServiceImpl intangibleProductJPAService;

    private final PublisherService publisherService;

    private final ResearchAreaService researchAreaService;


    @Autowired
    public IntangibleProductServiceImpl(MultilingualContentService multilingualContentService,
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
                                        IntangibleProductJPAServiceImpl intangibleProductJPAService,
                                        PublisherService publisherService,
                                        ResearchAreaService researchAreaService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService);
        this.intangibleProductJPAService = intangibleProductJPAService;
        this.publisherService = publisherService;
        this.researchAreaService = researchAreaService;
    }

    @Override
    @Transactional
    public IntangibleProduct findIntangibleProductById(Integer intangibleProductId) {
        return intangibleProductJPAService.findOne(intangibleProductId);
    }

    @Override
    @Transactional(readOnly = true)
    public IntangibleProductDTO readIntangibleProductById(Integer intangibleProductId) {
        IntangibleProduct intangibleProduct;
        try {
            intangibleProduct = intangibleProductJPAService.findOne(intangibleProductId);
        } catch (NotFoundException e) {
            log.info("Trying to read non-existent INTANGIBLE_PRODUCT with ID {}. Clearing index.",
                intangibleProductId);
            this.clearIndexWhenFailedRead(intangibleProductId,
                DocumentPublicationType.INTANGIBLE_PRODUCT);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !intangibleProduct.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return IntangibleProductConverter.toDTO(intangibleProduct);
    }

    @Override
    @Transactional
    public IntangibleProduct createIntangibleProduct(IntangibleProductDTO intangibleProductDTO,
                                                     Boolean index) {
        var newIntangibleProduct = new IntangibleProduct();

        checkForDocumentDate(intangibleProductDTO);
        setCommonFields(newIntangibleProduct, intangibleProductDTO);
        setIntangibleProductRelatedFields(newIntangibleProduct, intangibleProductDTO);

        var savedIntangibleProduct = intangibleProductJPAService.save(newIntangibleProduct);

        if (index) {
            indexIntangibleProduct(savedIntangibleProduct, new DocumentPublicationIndex());
        }

        sendNotifications(savedIntangibleProduct);

        return savedIntangibleProduct;
    }

    @Override
    @Transactional
    public void editIntangibleProduct(Integer intangibleProductId,
                                      IntangibleProductDTO intangibleProductDTO) {
        var intangibleProductToUpdate = intangibleProductJPAService.findOne(intangibleProductId);

        checkForDocumentDate(intangibleProductDTO);
        clearCommonFields(intangibleProductToUpdate);
        setCommonFields(intangibleProductToUpdate, intangibleProductDTO);
        setIntangibleProductRelatedFields(intangibleProductToUpdate, intangibleProductDTO);

        intangibleProductJPAService.save(intangibleProductToUpdate);

        indexIntangibleProduct(intangibleProductToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                    intangibleProductId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(intangibleProductToUpdate);
    }

    private void setIntangibleProductRelatedFields(IntangibleProduct intangibleProduct,
                                                   IntangibleProductDTO intangibleProductDTO) {
        intangibleProduct.setInternalNumber(intangibleProductDTO.getInternalNumber());
        intangibleProduct.setIntangibleProductType(intangibleProductDTO.getIntangibleProductType());
        intangibleProduct.setProductUsers(multilingualContentService.getMultilingualContent(
            intangibleProductDTO.getProductUsers()));

        intangibleProduct.setPublisher(null);
        intangibleProduct.setAuthorReprint(false);

        if (Objects.nonNull(intangibleProductDTO.getAuthorReprint()) &&
            intangibleProductDTO.getAuthorReprint()) {
            intangibleProduct.setAuthorReprint(true);
        } else if (Objects.nonNull(intangibleProductDTO.getPublisherId())) {
            intangibleProduct.setPublisher(
                publisherService.findOne(intangibleProductDTO.getPublisherId()));
        }

        var researchAreas = researchAreaService.getResearchAreasByIds(
            intangibleProductDTO.getResearchAreasId().stream().toList());
        intangibleProduct.setResearchAreas(new HashSet<>(researchAreas));
    }

    @Override
    @Transactional
    public void deleteIntangibleProduct(Integer intangibleProductId) {
        var intangibleProductToDelete = intangibleProductJPAService.findOne(intangibleProductId);

        deleteProofsAndFileItems(intangibleProductToDelete);

        intangibleProductJPAService.delete(intangibleProductId);
        this.delete(intangibleProductId);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(intangibleProductId));
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexIntangibleProduct() {
        // Super service does the initial deletion

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            intangibleProductJPAService::findAll,
            intangibleProduct ->
                indexIntangibleProduct(intangibleProduct, new DocumentPublicationIndex())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void indexIntangibleProduct(IntangibleProduct intangibleProduct) {
        indexIntangibleProduct(intangibleProduct,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                intangibleProduct.getId()).orElse(new DocumentPublicationIndex()));
    }

    private void indexIntangibleProduct(IntangibleProduct intangibleProduct,
                                        DocumentPublicationIndex index) {
        indexCommonFields(intangibleProduct, index);

        index.setType(DocumentPublicationType.INTANGIBLE_PRODUCT.name());
        if (Objects.nonNull(intangibleProduct.getPublisher())) {
            index.setPublisherId(intangibleProduct.getPublisher().getId());
        }
        index.setAuthorReprint(intangibleProduct.getAuthorReprint());

        index.setApa(
            citationService.craftCitationInGivenStyle("apa", index, LanguageAbbreviations.ENGLISH));
        documentPublicationIndexRepository.save(index);
    }
}
