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
import rs.teslaris.core.converter.document.PatentConverter;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.IntellectualPropertyApplicationStatus;
import rs.teslaris.core.model.document.IntellectualPropertyType;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.PatentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.PatentJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PatentService;
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
public class PatentServiceImpl extends DocumentPublicationServiceImpl implements PatentService {

    private final PatentJPAServiceImpl patentJPAService;

    private final PublisherService publisherService;

    private final PatentRepository patentRepository;


    @Autowired
    public PatentServiceImpl(MultilingualContentService multilingualContentService,
                             DocumentPublicationIndexRepository documentPublicationIndexRepository,
                             SearchService<DocumentPublicationIndex> searchService,
                             OrganisationUnitService organisationUnitService,
                             DocumentRepository documentRepository,
                             DocumentFileService documentFileService,
                             CitationService citationService,
                             ApplicationEventPublisher applicationEventPublisher,
                             PersonContributionService personContributionService,
                             ExpressionTransformer expressionTransformer, EventService eventService,
                             CommissionRepository commissionRepository,
                             SearchFieldsLoader searchFieldsLoader,
                             OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService,
                             InvolvementRepository involvementRepository,
                             OrganisationUnitOutputConfigurationService organisationUnitOutputConfigurationService,
                             DocumentLookupService documentLookupService,
                             CountryService countryService,
                             PatentJPAServiceImpl patentJPAService,
                             PublisherService publisherService,
                             PatentRepository patentRepository) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService,
            documentLookupService,
            countryService);
        this.patentJPAService = patentJPAService;
        this.publisherService = publisherService;
        this.patentRepository = patentRepository;
    }

    @Override
    @Transactional
    public Patent findPatentById(Integer patentId) {
        return patentJPAService.findOne(patentId);
    }

    @Override
    @Transactional(readOnly = true)
    public PatentDTO readPatentById(Integer patentId) {
        Patent patent;
        try {
            patent = patentJPAService.findOne(patentId);
        } catch (NotFoundException e) {
            log.info("Trying to read non-existent PATENT with ID {}. Clearing index.", patentId);
            this.clearIndexWhenFailedRead(patentId, DocumentPublicationType.PATENT);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !patent.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return PatentConverter.toDTO(patent);
    }

    @Override
    @Transactional
    public PatentDTO readPatentByOldId(Integer oldId) {
        var patent = patentRepository.findPatentByOldIdsContains(oldId);
        if (patent.isEmpty() || (!SessionUtil.isUserLoggedIn() &&
            !patent.get().getApproveStatus().equals(ApproveStatus.APPROVED))) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return PatentConverter.toDTO(patent.get());
    }

    @Override
    @Transactional
    public Patent createPatent(PatentDTO patentDTO, Boolean index) {
        var newPatent = new Patent();

        checkForDocumentDate(patentDTO);
        setCommonFields(newPatent, patentDTO, new HashSet<>());
        setPatentRelatedFields(newPatent, patentDTO);

        var savedPatent = patentJPAService.save(newPatent);

        applicationEventPublisher.publishEvent(
            new RevisionCreateEvent(
                DocumentPublicationType.PATENT.name(),
                savedPatent.getId(),
                null,
                PatentConverter.toDTO(savedPatent),
                RevisionType.CREATE
            )
        );

        if (index) {
            indexPatent(savedPatent, new DocumentPublicationIndex());
        }

        sendNotifications(savedPatent, Collections.emptySet());

        return savedPatent;
    }

    @Override
    @Transactional
    public void editPatent(Integer patentId, PatentDTO patentDTO) {
        var patentToUpdate = patentJPAService.findOne(patentId);

        applicationEventPublisher.publishEvent(
            new RevisionCreateEvent(
                DocumentPublicationType.PATENT.name(),
                patentId,
                PatentConverter.toDTO(patentToUpdate),
                patentDTO,
                RevisionType.UPDATE
            )
        );

        checkForDocumentDate(patentDTO);
        var oldContributorIds = clearCommonFields(patentToUpdate);
        setCommonFields(patentToUpdate, patentDTO, oldContributorIds);
        setPatentRelatedFields(patentToUpdate, patentDTO);

        var updatedPatent = patentJPAService.save(patentToUpdate);

        indexPatent(patentToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(patentId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(updatedPatent, oldContributorIds);
    }

    private void setPatentRelatedFields(Patent patent, PatentDTO patentDTO) {
        patent.setNumber(patentDTO.getNumber());
        patent.setType(patentDTO.getType());

        validateApplicationStatus(patentDTO.getType(), patentDTO.getApplicationStatus());
        patent.setApplicationStatus(patentDTO.getApplicationStatus());

        patent.setDateRequested(FlexibleDateConverter.fromDTO(patentDTO.getDateRequested()));
        patent.setDateFilingPriority(
            FlexibleDateConverter.fromDTO(patentDTO.getDateFilingPriority()));
        patent.setDateTo(FlexibleDateConverter.fromDTO(patentDTO.getDateTo()));

        patent.setPublisher(null);
        patent.setAuthorReprint(false);

        if (Objects.nonNull(patentDTO.getAuthorReprint()) && patentDTO.getAuthorReprint()) {
            patent.setAuthorReprint(true);
        } else if (Objects.nonNull(patentDTO.getPublisherId())) {
            patent.setPublisher(publisherService.findOne(patentDTO.getPublisherId()));
        }
    }

    @Override
    @Transactional
    public void deletePatent(Integer patentId) {
        var patentToDelete = patentJPAService.findOne(patentId);

        updateIndexedPersonContributions(patentToDelete);

        deleteProofsAndFileItems(patentToDelete);

        patentJPAService.delete(patentId);
        documentRepository.deleteDocumentContributions(patentId);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(patentId));
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexPatents() {
        // Super service does the initial deletion

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            patentJPAService::findAll,
            patent -> {
                var index = indexPatent(patent, new DocumentPublicationIndex());
                applicationEventPublisher.publishEvent(new ReindexExternalIndicatorsEvent(index));
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void indexPatent(Patent patent) {
        indexPatent(patent,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                patent.getId()).orElse(new DocumentPublicationIndex()));
    }

    private DocumentPublicationIndex indexPatent(Patent patent, DocumentPublicationIndex index) {
        indexCommonFields(patent, index);

        index.setType(DocumentPublicationType.PATENT.name());
        if (Objects.nonNull(patent.getPublisher())) {
            index.setPublisherId(patent.getPublisher().getId());
        } else {
            index.setPublisherId(null);
        }
        index.setAuthorReprint(patent.getAuthorReprint());

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
