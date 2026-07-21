package rs.teslaris.core.service.impl.document;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.ReindexExternalIndicatorsEvent;
import rs.teslaris.core.converter.commontypes.FlexibleDateConverter;
import rs.teslaris.core.converter.document.IntellectualPropertyConverter;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.IntellectualProperty;
import rs.teslaris.core.model.document.IntellectualPropertyApplicationStatus;
import rs.teslaris.core.model.document.IntellectualPropertyType;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.IntellectualPropertyRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.IntellectualPropertyJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.IntellectualPropertyService;
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
import rs.teslaris.revisioner.model.RevisionCreateEvent;
import rs.teslaris.revisioner.model.RevisionType;

@Service
@Traceable
@Slf4j
public class IntellectualPropertyServiceImpl extends DocumentPublicationServiceImpl implements
    IntellectualPropertyService {

    private final IntellectualPropertyJPAServiceImpl intellectualPropertyJPAService;

    private final PublisherService publisherService;

    private final IntellectualPropertyRepository intellectualPropertyRepository;


    @Autowired
    public IntellectualPropertyServiceImpl(MultilingualContentService multilingualContentService,
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
                                           DocumentLookupService documentLookupService,
                                           CountryService countryService,
                                           IntellectualPropertyJPAServiceImpl intellectualPropertyJPAService,
                                           PublisherService publisherService,
                                           IntellectualPropertyRepository intellectualPropertyRepository) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService,
            documentLookupService,
            countryService);
        this.intellectualPropertyJPAService = intellectualPropertyJPAService;
        this.publisherService = publisherService;
        this.intellectualPropertyRepository = intellectualPropertyRepository;
    }

    @Override
    @Transactional
    public IntellectualProperty findIntellectualPropertyById(Integer intellectualPropertyId) {
        return intellectualPropertyJPAService.findOne(intellectualPropertyId);
    }

    @Override
    @Transactional(readOnly = true)
    public IntellectualPropertyDTO readIntellectualPropertyById(Integer intellectualPropertyId) {
        IntellectualProperty intellectualProperty;
        try {
            intellectualProperty = intellectualPropertyJPAService.findOne(intellectualPropertyId);
        } catch (NotFoundException e) {
            log.info(
                "Trying to read non-existent INTELLECTUAL_PROPERTY with ID {}. Clearing index.",
                intellectualPropertyId);
            this.clearIndexWhenFailedRead(intellectualPropertyId,
                DocumentPublicationType.INTELLECTUAL_PROPERTY);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !intellectualProperty.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return IntellectualPropertyConverter.toDTO(intellectualProperty);
    }

    @Override
    @Transactional
    public IntellectualPropertyDTO readIntellectualPropertyByOldId(Integer oldId) {
        var intellectualProperty =
            intellectualPropertyRepository.findIntellectualPropertyByOldIdsContains(oldId);
        if (intellectualProperty.isEmpty() || (!SessionUtil.isUserLoggedIn() &&
            !intellectualProperty.get().getApproveStatus().equals(ApproveStatus.APPROVED))) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return IntellectualPropertyConverter.toDTO(intellectualProperty.get());
    }

    @Override
    @Transactional
    public IntellectualProperty createIntellectualProperty(
        IntellectualPropertyDTO intellectualPropertyDTO, Boolean index) {
        var newIntellectualProperty = new IntellectualProperty();

        checkForDocumentDate(intellectualPropertyDTO);
        setCommonFields(newIntellectualProperty, intellectualPropertyDTO, new HashSet<>());
        setIntellectualPropertyRelatedFields(newIntellectualProperty, intellectualPropertyDTO);

        var savedIntellectualProperty =
            intellectualPropertyJPAService.save(newIntellectualProperty);

        applicationEventPublisher.publishEvent(
            new RevisionCreateEvent(
                DocumentPublicationType.INTELLECTUAL_PROPERTY.name(),
                savedIntellectualProperty.getId(),
                null,
                IntellectualPropertyConverter.toDTO(savedIntellectualProperty),
                RevisionType.CREATE
            )
        );

        if (index) {
            indexIntellectualProperty(savedIntellectualProperty, new DocumentPublicationIndex());
        }

        sendNotifications(savedIntellectualProperty, Collections.emptySet());

        return savedIntellectualProperty;
    }

    @Override
    @Transactional
    public void editIntellectualProperty(Integer intellectualPropertyId,
                                         IntellectualPropertyDTO intellectualPropertyDTO) {
        var intellectualPropertyToUpdate =
            intellectualPropertyJPAService.findOne(intellectualPropertyId);

        applicationEventPublisher.publishEvent(
            new RevisionCreateEvent(
                DocumentPublicationType.INTELLECTUAL_PROPERTY.name(),
                intellectualPropertyId,
                IntellectualPropertyConverter.toDTO(intellectualPropertyToUpdate),
                intellectualPropertyDTO,
                RevisionType.UPDATE
            )
        );

        checkForDocumentDate(intellectualPropertyDTO);
        var oldContributorIds = clearCommonFields(intellectualPropertyToUpdate);
        setCommonFields(intellectualPropertyToUpdate, intellectualPropertyDTO, oldContributorIds);
        setIntellectualPropertyRelatedFields(intellectualPropertyToUpdate, intellectualPropertyDTO);

        var updatedIntellectualProperty =
            intellectualPropertyJPAService.save(intellectualPropertyToUpdate);

        indexIntellectualProperty(intellectualPropertyToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                    intellectualPropertyId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(updatedIntellectualProperty, oldContributorIds);
    }

    private void setIntellectualPropertyRelatedFields(IntellectualProperty intellectualProperty,
                                                      IntellectualPropertyDTO intellectualPropertyDTO) {
        intellectualProperty.setNumber(intellectualPropertyDTO.getNumber());
        intellectualProperty.setType(intellectualPropertyDTO.getType());

        validateApplicationStatus(intellectualPropertyDTO.getType(),
            intellectualPropertyDTO.getApplicationStatus());
        intellectualProperty.setApplicationStatus(intellectualPropertyDTO.getApplicationStatus());

        intellectualProperty.setDateRequested(
            FlexibleDateConverter.fromDTO(intellectualPropertyDTO.getDateRequested()));
        intellectualProperty.setDateFilingPriority(
            FlexibleDateConverter.fromDTO(intellectualPropertyDTO.getDateFilingPriority()));
        intellectualProperty.setDateTo(
            FlexibleDateConverter.fromDTO(intellectualPropertyDTO.getDateTo()));

        intellectualProperty.setPublisher(null);
        intellectualProperty.setAuthorReprint(false);

        if (Objects.nonNull(intellectualPropertyDTO.getAuthorReprint()) &&
            intellectualPropertyDTO.getAuthorReprint()) {
            intellectualProperty.setAuthorReprint(true);
        } else if (Objects.nonNull(intellectualPropertyDTO.getPublisherId())) {
            intellectualProperty.setPublisher(
                publisherService.findOne(intellectualPropertyDTO.getPublisherId()));
        }
    }

    @Override
    @Transactional
    public void deleteIntellectualProperty(Integer intellectualPropertyId) {
        var intellectualPropertyToDelete =
            intellectualPropertyJPAService.findOne(intellectualPropertyId);

        updateIndexedPersonContributions(intellectualPropertyToDelete);

        deleteProofsAndFileItems(intellectualPropertyToDelete);

        intellectualPropertyJPAService.delete(intellectualPropertyId);
        documentRepository.deleteDocumentContributions(intellectualPropertyId);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(intellectualPropertyId));
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexIntellectualProperties() {
        // Super service does the initial deletion

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            intellectualPropertyJPAService::findAll,
            intellectualProperty -> {
                var index =
                    indexIntellectualProperty(intellectualProperty, new DocumentPublicationIndex());
                applicationEventPublisher.publishEvent(new ReindexExternalIndicatorsEvent(index));
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void indexIntellectualProperty(IntellectualProperty intellectualProperty) {
        indexIntellectualProperty(intellectualProperty,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                intellectualProperty.getId()).orElse(new DocumentPublicationIndex()));
    }

    private DocumentPublicationIndex indexIntellectualProperty(
        IntellectualProperty intellectualProperty, DocumentPublicationIndex index) {
        indexCommonFields(intellectualProperty, index);

        index.setType(DocumentPublicationType.INTELLECTUAL_PROPERTY.name());
        if (Objects.nonNull(intellectualProperty.getPublisher())) {
            index.setPublisherId(intellectualProperty.getPublisher().getId());
        } else {
            index.setPublisherId(null);
        }
        index.setAuthorReprint(intellectualProperty.getAuthorReprint());

        index.setApa(
            citationService.craftCitationInGivenStyle("apa", index, LanguageAbbreviations.ENGLISH));
        documentPublicationIndexRepository.save(index);

        return index;
    }

    private void validateApplicationStatus(IntellectualPropertyType type,
                                           IntellectualPropertyApplicationStatus status) {
        if (Objects.isNull(type) || Objects.isNull(status)) {
            return;
        }

        if (status.equals(IntellectualPropertyApplicationStatus.DISCLOSED)
            && !type.equals(IntellectualPropertyType.DISCLOSURE)) {
            throw new IllegalArgumentException("DISCLOSED is only valid for DISCLOSURE.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.PENDING)
            && !type.equals(IntellectualPropertyType.PATENT)
            && !type.equals(IntellectualPropertyType.TRADEMARK)) {
            throw new IllegalArgumentException("PENDING is only valid for PATENT and TRADEMARK.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.IN_NEGOTIATION)
            && !type.equals(IntellectualPropertyType.LICENSE)) {
            throw new IllegalArgumentException("IN_NEGOTIATION is only valid for LICENSE.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.ALLOWED)
            && !type.equals(IntellectualPropertyType.PATENT)) {
            throw new IllegalArgumentException("ALLOWED is only valid for PATENT.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.GRANTED_OR_REGISTERED)
            && !type.equals(IntellectualPropertyType.PATENT)
            && !type.equals(IntellectualPropertyType.REGISTERED_COPYRIGHT)
            && !type.equals(IntellectualPropertyType.TRADEMARK)) {
            throw new IllegalArgumentException(
                "GRANTED_OR_REGISTERED is only valid for PATENT, REGISTERED_COPYRIGHT and TRADEMARK.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.PROTECTED)
            && !type.equals(IntellectualPropertyType.DISCLOSURE)) {
            throw new IllegalArgumentException("PROTECTED is only valid for DISCLOSURE.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.ASSIGNED)
            && !type.equals(IntellectualPropertyType.LICENSE)) {
            throw new IllegalArgumentException("ASSIGNED is only valid for LICENSE.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.FIRST_FIXATION)
            && !type.equals(IntellectualPropertyType.REGISTERED_COPYRIGHT)) {
            throw new IllegalArgumentException(
                "FIRST_FIXATION is only valid for REGISTERED_COPYRIGHT.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.EXPIRED)
            && !type.equals(IntellectualPropertyType.PATENT)) {
            throw new IllegalArgumentException("EXPIRED is only valid for PATENT.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.WITHDRAWN)
            && !type.equals(IntellectualPropertyType.PATENT)) {
            throw new IllegalArgumentException("WITHDRAWN is only valid for PATENT.");
        }

        if (status.equals(IntellectualPropertyApplicationStatus.ELIMINATED)
            && !type.equals(IntellectualPropertyType.REGISTERED_COPYRIGHT)) {
            throw new IllegalArgumentException(
                "ELIMINATED is only valid for REGISTERED_COPYRIGHT.");
        }
    }
}
