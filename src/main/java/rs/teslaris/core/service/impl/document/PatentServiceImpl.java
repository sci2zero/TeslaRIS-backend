package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.PatentConverter;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.PatentJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PatentService;
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
@Transactional
@Traceable
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
                             CitationService citationService,
                             ApplicationEventPublisher applicationEventPublisher,
                             PersonContributionService personContributionService,
                             ExpressionTransformer expressionTransformer, EventService eventService,
                             CommissionRepository commissionRepository,
                             SearchFieldsLoader searchFieldsLoader,
                             OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService,
                             InvolvementRepository involvementRepository,
                             OrganisationUnitOutputConfigurationService organisationUnitOutputConfigurationService,
                             PatentJPAServiceImpl patentJPAService,
                             PublisherService publisherService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService);
        this.patentJPAService = patentJPAService;
        this.publisherService = publisherService;
    }

    @Override
    public Patent findPatentById(Integer patentId) {
        return patentJPAService.findOne(patentId);
    }

    @Override
    public PatentDTO readPatentById(Integer patentId) {
        Patent patent;
        try {
            patent = patentJPAService.findOne(patentId);
        } catch (NotFoundException e) {
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
    public Patent createPatent(PatentDTO patentDTO, Boolean index) {
        var newPatent = new Patent();

        checkForDocumentDate(patentDTO);
        setCommonFields(newPatent, patentDTO);
        setPatentRelatedFields(newPatent, patentDTO);

        var savedPatent = patentJPAService.save(newPatent);

        if (index) {
            indexPatent(savedPatent, new DocumentPublicationIndex());
        }

        sendNotifications(savedPatent);

        return savedPatent;
    }

    @Override
    public void editPatent(Integer patentId, PatentDTO patentDTO) {
        var patentToUpdate = patentJPAService.findOne(patentId);

        checkForDocumentDate(patentDTO);
        clearCommonFields(patentToUpdate);
        setCommonFields(patentToUpdate, patentDTO);
        setPatentRelatedFields(patentToUpdate, patentDTO);

        var updatedPatent = patentJPAService.save(patentToUpdate);

        indexPatent(patentToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(patentId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(updatedPatent);
    }

    private void setPatentRelatedFields(Patent patent, PatentDTO patentDTO) {
        patent.setNumber(patentDTO.getNumber());

        patent.setPublisher(null);
        patent.setAuthorReprint(false);

        if (Objects.nonNull(patentDTO.getAuthorReprint()) && patentDTO.getAuthorReprint()) {
            patent.setAuthorReprint(true);
        } else if (Objects.nonNull(patentDTO.getPublisherId())) {
            patent.setPublisher(publisherService.findOne(patentDTO.getPublisherId()));
        }
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
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Patent> chunk =
                patentJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((patent) -> indexPatent(patent, new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    @Override
    public void indexPatent(Patent patent) {
        indexPatent(patent,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                patent.getId()).orElse(new DocumentPublicationIndex()));
    }

    private void indexPatent(Patent patent, DocumentPublicationIndex index) {
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
    }
}
