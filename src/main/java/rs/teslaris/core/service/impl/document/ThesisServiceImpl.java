package rs.teslaris.core.service.impl.document;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.repository.CommissionRepository;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.document.ThesisConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisAttachmentType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ThesisJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
@Transactional
public class ThesisServiceImpl extends DocumentPublicationServiceImpl implements ThesisService {

    private final ThesisJPAServiceImpl thesisJPAService;

    private final PublisherService publisherService;

    private final ResearchAreaService researchAreaService;

    private final LanguageService languageService;

    private final LanguageTagService languageTagService;

    private final ThesisRepository thesisRepository;

    @Value("${thesis.public-review.duration-days}")
    private Integer daysOnPublicReview;


    @Autowired
    public ThesisServiceImpl(MultilingualContentService multilingualContentService,
                             DocumentPublicationIndexRepository documentPublicationIndexRepository,
                             SearchService<DocumentPublicationIndex> searchService,
                             OrganisationUnitService organisationUnitService,
                             DocumentRepository documentRepository,
                             DocumentFileService documentFileService,
                             PersonContributionService personContributionService,
                             ExpressionTransformer expressionTransformer, EventService eventService,
                             CommissionRepository commissionRepository,
                             ThesisJPAServiceImpl thesisJPAService,
                             PublisherService publisherService,
                             ResearchAreaService researchAreaService,
                             LanguageService languageService,
                             LanguageTagService languageTagService,
                             ThesisRepository thesisRepository) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService,
            personContributionService,
            expressionTransformer, eventService, commissionRepository);
        this.thesisJPAService = thesisJPAService;
        this.publisherService = publisherService;
        this.researchAreaService = researchAreaService;
        this.languageService = languageService;
        this.languageTagService = languageTagService;
        this.thesisRepository = thesisRepository;
    }

    @Override
    public ThesisResponseDTO readThesisById(Integer thesisId) {
        var thesis = thesisJPAService.findOne(thesisId);
        if (!thesis.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return ThesisConverter.toDTO(thesis);
    }

    @Override
    public Thesis createThesis(ThesisDTO thesisDTO, Boolean index) {
        var newThesis = new Thesis();

        if (Objects.nonNull(thesisDTO.getContributions()) &&
            !thesisDTO.getContributions().isEmpty()) {
            thesisDTO.setContributions(thesisDTO.getContributions().subList(0, 1));
        }

        setCommonFields(newThesis, thesisDTO);
        setThesisRelatedFields(newThesis, thesisDTO);

        newThesis.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        var savedThesis = thesisJPAService.save(newThesis);

        if (newThesis.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexThesis(savedThesis, new DocumentPublicationIndex());
        }

        sendNotifications(savedThesis);

        return newThesis;
    }

    @Override
    public void editThesis(Integer thesisId, ThesisDTO thesisDTO) {
        var thesisToUpdate = thesisJPAService.findOne(thesisId);

        checkIfThesisIsOnPublicReview(thesisToUpdate);

        clearCommonFields(thesisToUpdate);
        thesisToUpdate.setOrganisationUnit(null);

        if (Objects.nonNull(thesisDTO.getContributions()) &&
            !thesisDTO.getContributions().isEmpty()) {
            thesisDTO.setContributions(thesisDTO.getContributions().subList(0, 1));
        }

        setCommonFields(thesisToUpdate, thesisDTO);
        setThesisRelatedFields(thesisToUpdate, thesisDTO);

        thesisJPAService.save(thesisToUpdate);

        if (thesisToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexThesis(thesisToUpdate,
                documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        thesisId)
                    .orElse(new DocumentPublicationIndex()));
        }

        sendNotifications(thesisToUpdate);
    }

    @Override
    public void deleteThesis(Integer thesisId) {
        var thesisToDelete = thesisJPAService.findOne(thesisId);
        checkIfThesisIsOnPublicReview(thesisToDelete);

        thesisJPAService.delete(thesisId);
    }

    @Override
    public void reindexTheses() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Thesis> chunk =
                thesisJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((thesis) -> indexThesis(thesis, new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    @Override
    public DocumentFileResponseDTO addThesisAttachment(Integer thesisId, DocumentFileDTO document,
                                                       ThesisAttachmentType attachmentType) {
        var thesis = thesisJPAService.findOne(thesisId);

        checkIfThesisIsOnPublicReview(thesis);

        document.setResourceType(attachmentType.getResourceType());
        var documentFile = documentFileService.saveNewPreliminaryDocument(document);

        switch (attachmentType) {
            case FILE -> {
                thesis.getPreliminaryFiles().forEach(file -> file.setLatest(false));
                thesis.getPreliminaryFiles().add(documentFile);
            }
            case SUPPLEMENT -> {
                thesis.getPreliminarySupplements().forEach(file -> file.setLatest(false));
                thesis.getPreliminarySupplements().add(documentFile);
            }
            case COMMISSION_REPORT -> {
                thesis.getCommissionReports().forEach(file -> file.setLatest(false));
                thesis.getCommissionReports().add(documentFile);
            }
        }

        thesisJPAService.save(thesis);
        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    public void deleteThesisAttachment(Integer thesisId, Integer documentFileId,
                                       ThesisAttachmentType attachmentType) {
        var thesis = thesisJPAService.findOne(thesisId);

        checkIfThesisIsOnPublicReview(thesis);

        var documentFile = documentFileService.findDocumentFileById(documentFileId);

        removeAttachment(thesis, documentFile, attachmentType);

        thesisJPAService.save(thesis);
        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    private void removeAttachment(Thesis thesis, DocumentFile documentFile,
                                  ThesisAttachmentType attachmentType) {
        Set<DocumentFile> attachments = switch (attachmentType) {
            case FILE -> thesis.getPreliminaryFiles();
            case SUPPLEMENT -> thesis.getPreliminarySupplements();
            case COMMISSION_REPORT -> thesis.getCommissionReports();
        };

        attachments.remove(documentFile);
        updateLatestFlag(attachments);
    }

    private void updateLatestFlag(Set<DocumentFile> attachments) {
        attachments.forEach(file -> file.setLatest(false));
        attachments.stream()
            .max(Comparator.comparing(DocumentFile::getTimestamp))
            .ifPresent(file -> file.setLatest(true));
    }

    @Override
    public void putOnPublicReview(Integer thesisId) {
        var thesis = thesisJPAService.findOne(thesisId);

        if (!thesis.getThesisType().equals(ThesisType.PHD) &&
            !thesis.getThesisType().equals(ThesisType.PHD_ART_PROJECT)) {
            throw new ThesisException("Only PHD theses can be put on public reviews.");
        }

        if (thesis.getIsOnPublicReview()) {
            throw new ThesisException("Already on public review.");
        }

        if (thesis.getPreliminaryFiles().isEmpty() ||
            thesis.getCommissionReports().isEmpty()) {
            throw new ThesisException("noAttachmentsMessage");
        }

        if (thesis.getPreliminaryFiles().size() != thesis.getCommissionReports().size()) {
            throw new ThesisException("missingAttachmentsMessage");
        }

        thesis.setIsOnPublicReview(true);
        thesis.getPublicReviewStartDates().add(LocalDate.now());
        thesisJPAService.save(thesis);
    }

    @Override
    public void removeFromPublicReview(Integer thesisId) {
        var thesis = thesisJPAService.findOne(thesisId);

        if (!thesis.getIsOnPublicReview()) {
            throw new ThesisException("Thesis is not on public review.");
        }

        var lastPublicReviewDate = thesis.getPublicReviewStartDates().stream()
            .max(Comparator.naturalOrder()).stream().findFirst();

        if (lastPublicReviewDate.isEmpty()) {
            throw new ThesisException("Never been on public review.");
        }

        thesis.setIsOnPublicReview(false);
        thesis.getPublicReviewStartDates().remove(lastPublicReviewDate.get());
    }

    private void setThesisRelatedFields(Thesis thesis, ThesisDTO thesisDTO) {
        thesis.setThesisType(thesisDTO.getThesisType());
        thesis.setNumberOfPages(thesisDTO.getNumberOfPages());

        if (Objects.nonNull(thesisDTO.getPublisherId())) {
            thesis.setPublisher(publisherService.findOne(thesisDTO.getPublisherId()));
        }

        if (Objects.nonNull(thesisDTO.getResearchAreaId())) {
            thesis.setResearchArea(researchAreaService.findOne(thesisDTO.getResearchAreaId()));
        }

        if (Objects.nonNull(thesisDTO.getLanguageId())) {
            thesis.setLanguage(languageService.findOne(thesisDTO.getLanguageId()));
        }

        if (Objects.nonNull(thesisDTO.getWritingLanguageTagId())) {
            thesis.setWritingLanguage(
                languageTagService.findOne(thesisDTO.getWritingLanguageTagId()));
        }

        if (Objects.nonNull(thesisDTO.getOrganisationUnitId())) {
            thesis.setOrganisationUnit(
                organisationUnitService.findOrganisationUnitById(
                    thesisDTO.getOrganisationUnitId()));
        } else {
            if (Objects.isNull(thesisDTO.getExternalOrganisationUnitName())) {
                throw new NotFoundException(
                    "No organisation unit ID provided without external OU name reference.");
            }

            thesis.setExternalOrganisationUnitName(
                multilingualContentService.getMultilingualContent(
                    thesisDTO.getExternalOrganisationUnitName()));
        }
    }

    private void indexThesis(Thesis thesis, DocumentPublicationIndex index) {
        indexCommonFields(thesis, index);

        index.setType(DocumentPublicationType.THESIS.name());
        if (Objects.nonNull(thesis.getPublisher())) {
            index.setPublisherId(thesis.getPublisher().getId());
        }

        documentPublicationIndexRepository.save(index);
    }

    private void checkIfThesisIsOnPublicReview(Thesis thesis) {
        if (thesis.getIsOnPublicReview()) {
            throw new ThesisException("Public review is in progress, can't edit.");
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // every day at midnight
    public void removeFromPublicReview() {
        var thesesOnPublicReview = thesisRepository.findAllOnPublicReview();

        var now = new Date();
        var thirtyDaysAgo =
            (new Date(now.getTime() - (daysOnPublicReview * 24 * 60 * 60 * 1000))).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();

        thesesOnPublicReview.stream()
            .filter(thesis -> thesis.getPublicReviewStartDates().stream()
                .max(Comparator.naturalOrder())
                .filter(publicReviewStartDate -> publicReviewStartDate.isBefore(thirtyDaysAgo))
                .isPresent())
            .forEach(thesis -> {
                thesis.setIsOnPublicReview(false);
                thesisJPAService.save(thesis);
            });
    }
}
