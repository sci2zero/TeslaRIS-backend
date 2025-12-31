package rs.teslaris.core.service.impl.document;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.SoftwareConverter;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.SoftwareJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.document.SoftwareService;
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
public class SoftwareServiceImpl extends DocumentPublicationServiceImpl implements SoftwareService {

    private final SoftwareJPAServiceImpl softwareJPAService;

    private final PublisherService publisherService;


    @Autowired
    public SoftwareServiceImpl(MultilingualContentService multilingualContentService,
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
                               SoftwareJPAServiceImpl softwareJPAService,
                               PublisherService publisherService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService);
        this.softwareJPAService = softwareJPAService;
        this.publisherService = publisherService;
    }

    @Override
    @Transactional
    public Software findSoftwareById(Integer softwareId) {
        return softwareJPAService.findOne(softwareId);
    }

    @Override
    @Transactional(readOnly = true)
    public SoftwareDTO readSoftwareById(Integer softwareId) {
        Software software;
        try {
            software = softwareJPAService.findOne(softwareId);
        } catch (NotFoundException e) {
            log.info("Trying to read non-existent SOFTWARE with ID {}. Clearing index.",
                softwareId);
            this.clearIndexWhenFailedRead(softwareId, DocumentPublicationType.SOFTWARE);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !software.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return SoftwareConverter.toDTO(software);
    }

    @Override
    @Transactional
    public Software createSoftware(SoftwareDTO softwareDTO, Boolean index) {
        var newSoftware = new Software();

        checkForDocumentDate(softwareDTO);
        setCommonFields(newSoftware, softwareDTO);
        setSoftwareRelatedFields(newSoftware, softwareDTO);

        var savedSoftware = softwareJPAService.save(newSoftware);

        if (index) {
            indexSoftware(savedSoftware, new DocumentPublicationIndex());
        }

        sendNotifications(savedSoftware);

        return savedSoftware;
    }

    @Override
    @Transactional
    public void editSoftware(Integer softwareId, SoftwareDTO softwareDTO) {
        var softwareToUpdate = softwareJPAService.findOne(softwareId);

        checkForDocumentDate(softwareDTO);
        clearCommonFields(softwareToUpdate);
        setCommonFields(softwareToUpdate, softwareDTO);
        setSoftwareRelatedFields(softwareToUpdate, softwareDTO);

        softwareJPAService.save(softwareToUpdate);

        indexSoftware(softwareToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(softwareId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(softwareToUpdate);
    }

    private void setSoftwareRelatedFields(Software software, SoftwareDTO softwareDTO) {
        software.setInternalNumber(softwareDTO.getInternalNumber());

        software.setPublisher(null);
        software.setAuthorReprint(false);

        if (Objects.nonNull(softwareDTO.getAuthorReprint()) && softwareDTO.getAuthorReprint()) {
            software.setAuthorReprint(true);
        } else if (Objects.nonNull(softwareDTO.getPublisherId())) {
            software.setPublisher(
                publisherService.findOne(softwareDTO.getPublisherId()));
        }
    }

    @Override
    @Transactional
    public void deleteSoftware(Integer softwareId) {
        var softwareToDelete = softwareJPAService.findOne(softwareId);

        deleteProofsAndFileItems(softwareToDelete);

        softwareJPAService.delete(softwareId);
        this.delete(softwareId);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(softwareId));
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexSoftware() {
        // Super service does the initial deletion

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            softwareJPAService::findAll,
            software ->
                indexSoftware(software, new DocumentPublicationIndex())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void indexSoftware(Software software) {
        indexSoftware(software,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                software.getId()).orElse(new DocumentPublicationIndex()));
    }

    private void indexSoftware(Software software, DocumentPublicationIndex index) {
        indexCommonFields(software, index);

        index.setType(DocumentPublicationType.SOFTWARE.name());
        if (Objects.nonNull(software.getPublisher())) {
            index.setPublisherId(software.getPublisher().getId());
        }
        index.setAuthorReprint(software.getAuthorReprint());

        index.setApa(
            citationService.craftCitationInGivenStyle("apa", index, LanguageAbbreviations.ENGLISH));
        documentPublicationIndexRepository.save(index);
    }
}
