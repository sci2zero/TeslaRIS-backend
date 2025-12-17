package rs.teslaris.core.service.impl.document;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.document.ThesisConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisLibraryFormatsResponseDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.LibraryFormat;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisAttachmentType;
import rs.teslaris.core.model.document.ThesisPhysicalDescription;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.repository.document.ThesisResearchOutputRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ThesisJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.BrandingInformationService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.OrganisationUnitReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.SessionUtil;
import rs.teslaris.core.util.xmlutil.XMLUtil;

@Service
@Slf4j
@Traceable
public class ThesisServiceImpl extends DocumentPublicationServiceImpl implements ThesisService {

    private final Pattern udcPattern =
        Pattern.compile("^\\d{1,3}([.:/]\\d{1,5})*(\\(\\d{1,5}(\\.\\d{1,5})?\\))?$",
            Pattern.CASE_INSENSITIVE);

    private final ThesisJPAServiceImpl thesisJPAService;

    private final PublisherService publisherService;

    private final LanguageService languageService;

    private final LanguageTagService languageTagService;

    private final ThesisRepository thesisRepository;

    private final ThesisResearchOutputRepository thesisResearchOutputRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final UserService userService;

    private final MessageSource messageSource;

    private final BrandingInformationService brandingInformationService;

    private final EmailUtil emailUtil;

    private final DocumentFileService documentFileService;

    private final TaskManagerService taskManagerService;

    private final FileService fileService;

    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    @Value("${thesis.public-review.duration-days}")
    private Integer daysOnPublicReview;

    @Value("${feedback.email}")
    private String feedbackEmail;

    @Value("${thesis.check-public-review-end.enable-fallback}")
    private Boolean fallbackPublicReviewCheckEnabled;

    @Value("${migration-mode.enabled}")
    private Boolean migrationModeEnabled;


    @Autowired
    public ThesisServiceImpl(MultilingualContentService multilingualContentService,
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
                             ThesisJPAServiceImpl thesisJPAService,
                             PublisherService publisherService,
                             LanguageService languageService, LanguageTagService languageTagService,
                             ThesisRepository thesisRepository,
                             ThesisResearchOutputRepository thesisResearchOutputRepository,
                             OrganisationUnitIndexRepository organisationUnitIndexRepository,
                             UserService userService, MessageSource messageSource,
                             BrandingInformationService brandingInformationService,
                             EmailUtil emailUtil, DocumentFileService documentFileService1,
                             TaskManagerService taskManagerService, FileService fileService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService, citationService,
            applicationEventPublisher, personContributionService, expressionTransformer,
            eventService,
            commissionRepository, searchFieldsLoader, organisationUnitTrustConfigurationService,
            involvementRepository, organisationUnitOutputConfigurationService);
        this.thesisJPAService = thesisJPAService;
        this.publisherService = publisherService;
        this.languageService = languageService;
        this.languageTagService = languageTagService;
        this.thesisRepository = thesisRepository;
        this.thesisResearchOutputRepository = thesisResearchOutputRepository;
        this.organisationUnitIndexRepository = organisationUnitIndexRepository;
        this.userService = userService;
        this.messageSource = messageSource;
        this.brandingInformationService = brandingInformationService;
        this.emailUtil = emailUtil;
        this.documentFileService = documentFileService1;
        this.taskManagerService = taskManagerService;
        this.fileService = fileService;
    }

    @Override
    @Transactional
    public Thesis getThesisById(Integer thesisId) {
        return thesisJPAService.findOne(thesisId);
    }

    @Override
    @Transactional(readOnly = true)
    public ThesisResponseDTO readThesisById(Integer thesisId) {
        Thesis thesis;
        try {
            thesis = thesisJPAService.findOne(thesisId);
        } catch (NotFoundException e) {
            log.info("Trying to read non-existent THESIS with ID {}. Clearing index.", thesisId);
            this.clearIndexWhenFailedRead(thesisId, DocumentPublicationType.THESIS);
            throw e;
        }

        if (!SessionUtil.isUserLoggedIn() &&
            !thesis.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return ThesisConverter.toDTO(thesis);
    }

    @Override
    @Transactional
    public ThesisResponseDTO readThesisByOldId(Integer oldId) {
        var thesis = thesisRepository.findThesisByOldIdsContains(oldId);
        if (thesis.isEmpty() || (!SessionUtil.isUserLoggedIn() &&
            !thesis.get().getApproveStatus().equals(ApproveStatus.APPROVED))) {
            throw new NotFoundException("Document with given id does not exist.");
        }

        return ThesisConverter.toDTO(thesis.get());
    }

    @Override
    @Transactional
    public Thesis createThesis(ThesisDTO thesisDTO, Boolean index) {
        var newThesis = new Thesis();

        if (Objects.nonNull(thesisDTO.getContributions()) &&
            !thesisDTO.getContributions().isEmpty()) {
            var contributions = thesisDTO.getContributions();

            var firstAuthor = contributions.stream()
                .filter(c -> DocumentContributionType.AUTHOR.equals(c.getContributionType()))
                .findFirst();

            var others = contributions.stream()
                .filter(c -> !DocumentContributionType.AUTHOR.equals(c.getContributionType()))
                .toList();

            var result = new ArrayList<PersonDocumentContributionDTO>();
            firstAuthor.ifPresent(result::add);
            result.addAll(others);

            thesisDTO.setContributions(result);
        }

        setCommonFields(newThesis, thesisDTO);
        setThesisRelatedFields(newThesis, thesisDTO);

        var savedThesis = thesisJPAService.save(newThesis);

        if (index) {
            indexThesis(savedThesis, new DocumentPublicationIndex());
        }

        sendNotifications(savedThesis);

        return newThesis;
    }

    @Override
    @Transactional
    public void editThesis(Integer thesisId, ThesisDTO thesisDTO) {
        var thesisToUpdate = thesisJPAService.findOne(thesisId);

        checkIfAvailableForEditing(thesisToUpdate);

        clearCommonFields(thesisToUpdate);
        thesisToUpdate.setOrganisationUnit(null);

        if (Objects.nonNull(thesisDTO.getContributions()) &&
            !thesisDTO.getContributions().isEmpty()) {
            AtomicBoolean firstAuthorFound = new AtomicBoolean(false);

            var filteredContributions = thesisDTO.getContributions().stream()
                .filter(contribution ->
                    !contribution.getContributionType().equals(DocumentContributionType.AUTHOR) ||
                        firstAuthorFound.compareAndSet(false, true)
                )
                .toList();

            thesisDTO.setContributions(filteredContributions);
        }

        setCommonFields(thesisToUpdate, thesisDTO);
        setThesisRelatedFields(thesisToUpdate, thesisDTO);

        thesisJPAService.save(thesisToUpdate);

        indexThesis(thesisToUpdate,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId)
                .orElse(new DocumentPublicationIndex()));

        sendNotifications(thesisToUpdate);
    }

    @Override
    @Transactional
    public void deleteThesis(Integer thesisId) {
        var thesisToDelete = thesisJPAService.findOne(thesisId);
        checkIfAvailableForEditing(thesisToDelete);

        thesisJPAService.delete(thesisId);
        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(thesisId));
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexTheses() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Thesis> chunk =
                thesisJPAService.findAll(PageRequest.of(pageNumber, chunkSize,
                    Sort.by(Sort.Direction.ASC, "id"))).getContent();

            chunk.forEach(thesis -> {
                try {
                    indexThesis(thesis, new DocumentPublicationIndex());
                } catch (Exception e) {
                    log.warn("Skipping THESIS {} due to indexing error: {}", thesis.getId(),
                        e.getMessage());
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void indexThesis(Thesis thesis) {
        indexThesis(thesis,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                thesis.getId()).orElse(new DocumentPublicationIndex()));
    }

    @Override
    @Transactional
    public DocumentFileResponseDTO addThesisAttachment(Integer thesisId, DocumentFileDTO document,
                                                       ThesisAttachmentType attachmentType) {
        var thesis = thesisJPAService.findOne(thesisId);

        if (!(migrationModeEnabled && SessionUtil.isUserLoggedInAndAdmin())) {
            checkIfAvailableForEditing(thesis);
        }

        document.setResourceType(attachmentType.getResourceType());
        var documentFile = documentFileService.saveNewPreliminaryDocument(document);
        documentFile.setDocument(thesis);
        documentFileService.save(documentFile);

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

        var index = findDocumentPublicationIndexByDatabaseId(thesisId);
        index.setContainsFiles(
            !thesis.getFileItems().isEmpty() || !thesis.getProofs().isEmpty() ||
                !thesis.getPreliminaryFiles().isEmpty() ||
                !thesis.getPreliminarySupplements().isEmpty() ||
                !thesis.getCommissionReports().isEmpty());
        documentPublicationIndexRepository.save(index);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    @Transactional
    public void transferPreliminaryFileToOfficial(Integer thesisId, Integer documentFileId) {
        var thesis = thesisJPAService.findOne(thesisId);

        var documentFile = thesis.getPreliminaryFiles().stream().filter(
            file -> file.getId().equals(documentFileId) &&
                file.getResourceType().equals(ResourceType.PREPRINT)).findFirst();

        if (documentFile.isEmpty()) {
            documentFile = thesis.getPreliminarySupplements().stream().filter(
                file -> file.getId().equals(documentFileId) &&
                    file.getResourceType().equals(ResourceType.SUPPLEMENT)).findFirst();
        }

        var finalDocumentFile = documentFile;
        if (
            documentFile.isPresent() &&
                ((documentFile.get().getResourceType().equals(ResourceType.PREPRINT) &&
                    thesis.getFileItems().stream()
                        .anyMatch(
                            f -> f.getResourceType().equals(ResourceType.OFFICIAL_PUBLICATION))) ||
                    thesis.getFileItems().stream().anyMatch(f -> f.getFilename().equals(
                        finalDocumentFile.get().getFilename())))
        ) {
            throw new ThesisException("Thesis already has an official file version uploaded.");
        }

        finalDocumentFile.ifPresent(file -> {
            var officialPublication = new DocumentFile();
            officialPublication.setDocument(file.getDocument());
            officialPublication.setFilename(file.getFilename());
            officialPublication.setMimeType(file.getMimeType());
            officialPublication.setFileSize(file.getFileSize());
            officialPublication.setAccessRights(file.getAccessRights());
            officialPublication.setLicense(file.getLicense());
            officialPublication.setApproveStatus(file.getApproveStatus());

            if (file.getResourceType().equals(ResourceType.PREPRINT)) {
                officialPublication.setResourceType(ResourceType.OFFICIAL_PUBLICATION);
            } else {
                officialPublication.setResourceType(ResourceType.SUPPLEMENT);
            }

            var description = new HashSet<>();
            file.getDescription().forEach(multiLingualContent -> description.add(
                new MultiLingualContent(multiLingualContent)));
            officialPublication.setDescription((Set) description);

            var copiedFileResource = fileService.duplicateFile(file.getServerFilename());
            officialPublication.setServerFilename(copiedFileResource.a);
            documentFileService.save(officialPublication);

            documentFileService.parseAndIndexPdfDocument(officialPublication, copiedFileResource.b,
                officialPublication.getFilename(), officialPublication.getServerFilename(),
                new DocumentFileIndex());

            thesis.getFileItems().add(officialPublication);
            thesisJPAService.save(thesis);

            try {
                copiedFileResource.b.close();
            } catch (IOException e) {
                log.error("Unable to close stream of duplicated file {}.", copiedFileResource.a);
            }
        });
    }

    @Override
    @Transactional
    public void schedulePublicReviewEndCheck(LocalDateTime timestamp, List<ThesisType> types,
                                             Integer publicReviewLengthDays, Integer userId,
                                             RecurrenceType recurrence) {
        var taskId = taskManagerService.scheduleTask("PublicReviewEndCheck-" +
                StringUtils.join(types.stream().map(ThesisType::name).toList(), "-") + "-" +
                publicReviewLengthDays + "_DAYS-" +
                UUID.randomUUID(),
            timestamp,
            () -> removeFromPublicReview(types, publicReviewLengthDays),
            userId, recurrence);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timestamp,
                ScheduledTaskType.PUBLIC_REVIEW_END_DATE_CHECK, new HashMap<>() {{
                put("types", types);
                put("publicReviewLengthDays", publicReviewLengthDays);
                put("userId", userId);
            }}, recurrence));
    }

    @Override
    @Transactional
    public void deleteThesisAttachment(Integer thesisId, Integer documentFileId,
                                       ThesisAttachmentType attachmentType) {
        var thesis = thesisJPAService.findOne(thesisId);

        checkIfAvailableForEditing(thesis);

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
    @Transactional
    public void putOnPublicReview(Integer thesisId, Boolean continueLastReview) {
        var thesis = thesisJPAService.findOne(thesisId);
        validateThesisForPublicReview(thesis);

        thesis.setIsOnPublicReview(true);
        updatePublicReviewDates(thesis, continueLastReview, false);

        thesis.setIsOnPublicReviewPause(false);
        thesisJPAService.save(thesis);
        indexThesis(thesis,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId)
                .orElse(new DocumentPublicationIndex()));
    }

    private void validateThesisForPublicReview(Thesis thesis) {
        if (Objects.isNull(thesis.getOrganisationUnit()) ||
            !thesis.getOrganisationUnit().getIsClientInstitutionDl()) {
            throw new ThesisException("Institution is not a digital library client.");
        }

        if (!isPhdThesis(thesis)) {
            throw new ThesisException("Only PHD theses can be put on public reviews.");
        }

        if (thesis.getIsOnPublicReview()) {
            throw new ThesisException("Already on public review.");
        }

        if (thesis.getTitle().isEmpty() || Objects.isNull(thesis.getOrganisationUnit()) ||
            Objects.isNull(thesis.getScientificArea()) || thesis.getScientificArea().isEmpty() ||
            thesis.getPreliminaryFiles().isEmpty() || thesis.getCommissionReports().isEmpty()) {
            throw new ThesisException("noAttachmentsMessage");
        }

        if (thesis.getPreliminaryFiles().size() != thesis.getCommissionReports().size()) {
            throw new ThesisException("missingAttachmentsMessage");
        }
    }

    private boolean isPhdThesis(Thesis thesis) {
        return thesis.getThesisType() == ThesisType.PHD ||
            thesis.getThesisType() == ThesisType.PHD_ART_PROJECT;
    }

    private void updatePublicReviewDates(Thesis thesis, Boolean continueLastReview,
                                         Boolean removeLastPublicReviewStartDate) {
        if ((!thesis.getIsOnPublicReviewPause() && !thesis.getPublicReviewStartDates().isEmpty()) ||
            continueLastReview) {
            return;
        }

        if (removeLastPublicReviewStartDate) {
            thesis.getPublicReviewStartDates()
                .stream()
                .max(Comparator.naturalOrder())
                .ifPresent(thesis.getPublicReviewStartDates()::remove);
        }

        thesis.getPublicReviewStartDates().add(LocalDate.now());
    }

    @Override
    @Transactional
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
        thesis.setIsOnPublicReviewPause(true);

        thesisJPAService.save(thesis);
        indexThesis(thesis,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId)
                .orElse(new DocumentPublicationIndex()));
    }

    @Override
    @Transactional
    public void archiveThesis(Integer thesisId) {
        var thesis = thesisJPAService.findOne(thesisId);

        if (thesis.getTitle().isEmpty() || Objects.isNull(thesis.getThesisDefenceDate()) ||
            Objects.isNull(thesis.getDocumentDate()) || thesis.getDocumentDate().isBlank()) {
            throw new ThesisException("missingDataToArchiveMessage");
        }

        thesis.setIsArchived(true);
        thesisJPAService.save(thesis);

        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId)
            .ifPresent(index -> {
                index.setIsArchived(thesis.getIsArchived());
                documentPublicationIndexRepository.save(index);
            });
    }

    @Override
    @Transactional
    public void unarchiveThesis(Integer thesisId) {
        var thesis = thesisJPAService.findOne(thesisId);
        thesis.setIsArchived(false);

        thesisJPAService.save(thesis);

        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId)
            .ifPresent(index -> {
                index.setIsArchived(thesis.getIsArchived());
                documentPublicationIndexRepository.save(index);
            });
    }

    @Override
    @Transactional
    public ThesisLibraryFormatsResponseDTO getLibraryReferenceFormat(Integer thesisId) {
        var thesis = thesisJPAService.findOne(thesisId);
        try {
            return new ThesisLibraryFormatsResponseDTO(
                XMLUtil.convertToXml(ThesisConverter.toETDMSModel(thesis)),
                XMLUtil.convertToXml(ThesisConverter.toDCModel(thesis)),
                XMLUtil.convertToXml(ThesisConverter.convertToMarc21(thesis)));
        } catch (JAXBException e) {
            log.error("Unable to create library references. Reason: {}", e.getMessage());
            throw new ThesisException(
                "Unable to create library references."); // Should never happen
        }
    }

    @Override
    @Transactional
    public String getSingleLibraryReferenceFormat(Integer thesisId, LibraryFormat libraryFormat) {
        var thesis = thesisJPAService.findOne(thesisId);
        try {
            return switch (libraryFormat) {
                case ETD_MS -> XMLUtil.convertToXml(ThesisConverter.toETDMSModel(thesis));
                case MARC21 -> XMLUtil.convertToXml(ThesisConverter.toDCModel(thesis));
                case DUBLIN_CORE -> XMLUtil.convertToXml(ThesisConverter.convertToMarc21(thesis));
            };
        } catch (JAXBException e) {
            log.error("Unable to create library reference. Reason: {}", e.getMessage());
            return ""; // should never happen
        }
    }

    private void setThesisRelatedFields(Thesis thesis, ThesisDTO thesisDTO) {
        if (Objects.nonNull(thesisDTO.getOrganisationUnitId())) {
            var institution = organisationUnitService.findOne(thesisDTO.getOrganisationUnitId());

            if (Objects.nonNull(thesisDTO.getThesisType()) &&
                !institution.getAllowedThesisTypes().contains(thesisDTO.getThesisType().name())) {
                throw new OrganisationUnitReferenceConstraintViolationException(
                    "thesisTypeNotAllowedMessage");
            }

            thesis.setOrganisationUnit(institution);
        } else {
            if (Objects.isNull(thesisDTO.getExternalOrganisationUnitName())) {
                throw new NotFoundException(
                    "No organisation unit ID provided without external OU name reference.");
            }

            thesis.setExternalOrganisationUnitName(
                multilingualContentService.getMultilingualContent(
                    thesisDTO.getExternalOrganisationUnitName()));
        }

        var isAdmin = SessionUtil.isUserLoggedInAndAdmin();

        var isHeadOfLibrary = SessionUtil.isUserLoggedIn() &&
            Objects.requireNonNull(SessionUtil.getLoggedInUser()).getAuthority().getName()
                .equals(UserRole.HEAD_OF_LIBRARY.name());

        thesis.setThesisType(thesisDTO.getThesisType());
        thesis.setTopicAcceptanceDate(thesisDTO.getTopicAcceptanceDate());

        thesis.setAlternateTitle(
            multilingualContentService.getMultilingualContent(thesisDTO.getAlternateTitle()));
        thesis.setExtendedAbstract(
            multilingualContentService.getMultilingualContent(thesisDTO.getExtendedAbstract()));

        thesis.setPhysicalDescription(new ThesisPhysicalDescription() {{
            setNumberOfPages(thesisDTO.getNumberOfPages());
            setNumberOfChapters(thesisDTO.getNumberOfChapters());
            setNumberOfReferences(thesisDTO.getNumberOfReferences());
            setNumberOfGraphs(thesisDTO.getNumberOfGraphs());
            setNumberOfIllustrations(thesisDTO.getNumberOfIllustrations());
            setNumberOfTables(thesisDTO.getNumberOfTables());
            setNumberOfAppendices(thesisDTO.getNumberOfAppendices());
        }});

        if (Objects.nonNull(thesisDTO.getThesisDefenceDate())) {
            thesis.setDocumentDate(String.valueOf(thesisDTO.getThesisDefenceDate().getYear()));

            if (thesis.getPublicReviewCompleted() || isAdmin || isHeadOfLibrary ||
                Objects.isNull(thesis.getOrganisationUnit()) ||
                (Objects.nonNull(thesis.getOrganisationUnit()) &&
                    !thesis.getOrganisationUnit().getIsClientInstitutionDl())) {
                thesis.setThesisDefenceDate(thesisDTO.getThesisDefenceDate());
            }
        } else {
            thesis.setThesisDefenceDate(null);

            if (Objects.nonNull(thesis.getPublicReviewStartDates()) &&
                !thesis.getPublicReviewStartDates().isEmpty()) {
                thesis.getPublicReviewStartDates().stream().max(LocalDate::compareTo)
                    .ifPresent(latestPublicReviewDate -> {
                        thesis.setDocumentDate(String.valueOf(latestPublicReviewDate.getYear()));
                    });
            }
        }

        thesis.setPublisher(null);
        thesis.setAuthorReprint(false);

        if (Objects.nonNull(thesisDTO.getAuthorReprint()) && thesisDTO.getAuthorReprint()) {
            thesis.setAuthorReprint(true);
        } else if (Objects.nonNull(thesisDTO.getPublisherId())) {
            thesis.setPublisher(publisherService.findOne(thesisDTO.getPublisherId()));
        }

        thesis.setScientificArea(
            multilingualContentService.getMultilingualContent(thesisDTO.getScientificArea()));
        thesis.setScientificSubArea(
            multilingualContentService.getMultilingualContent(thesisDTO.getScientificSubArea()));
        thesis.setPlaceOfKeeping(
            multilingualContentService.getMultilingualContent(thesisDTO.getPlaceOfKeep()));
        thesis.setTypeOfTitle(
            multilingualContentService.getMultilingualContent(thesisDTO.getTypeOfTitle()));

        if (Objects.nonNull(thesisDTO.getUdc())) {
            // udcPattern.matcher(thesisDTO.getUdc()).matches()
            thesis.setUdc(thesisDTO.getUdc());
        }

        if (Objects.nonNull(thesisDTO.getLanguageId())) {
            thesis.setLanguage(languageService.findOne(thesisDTO.getLanguageId()));
        }

        if (Objects.nonNull(thesisDTO.getWritingLanguageTagId())) {
            thesis.setWritingLanguage(
                languageTagService.findOne(thesisDTO.getWritingLanguageTagId()));
        } else {
            thesis.setWritingLanguage(null);
        }

        setCommonIdentifiers(thesis, thesisDTO);

        if (isAdmin) {
            if (Objects.nonNull(thesisDTO.getPublicReviewStartDate())) {
                thesis.getPublicReviewStartDates().clear();
                thesis.getPublicReviewStartDates().add(thesisDTO.getPublicReviewStartDate());

                var startDate = thesisDTO.getPublicReviewStartDate();
                var isOnReviewFlag = Boolean.TRUE.equals(thesisDTO.getIsOnPublicReview());
                var cutoffDate = LocalDate.now().minusDays(daysOnPublicReview);

                boolean withinWindow = Objects.nonNull(startDate) &&
                    (startDate.isAfter(cutoffDate) || startDate.isEqual(cutoffDate));

                if (withinWindow || isOnReviewFlag) {
                    thesis.setIsOnPublicReview(true);
                    thesis.setPublicReviewCompleted(false);
                } else {
                    thesis.setPublicReviewCompleted(true);
                    thesis.setIsOnPublicReview(false);
                }
            }

            if (Objects.nonNull(thesisDTO.getIsArchived()) && thesisDTO.getIsArchived()) {
                thesis.setIsArchived(true);
            }
        }
    }

    private void setCommonIdentifiers(Thesis thesis, ThesisDTO thesisDTO) {
        IdentifierUtil.validateAndSetIdentifier(
            thesisDTO.getEisbn(),
            thesis.getId(),
            "^(?:(?:\\d[\\ |-]?){9}[\\dX]|(?:\\d[\\ |-]?){13})$",
            thesisRepository::existsByeISBN,
            thesis::setEISBN,
            "eisbnFormatError",
            "eisbnExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            thesisDTO.getPrintISBN(),
            thesis.getId(),
            "^(?:(?:\\d[\\ |-]?){9}[\\dX]|(?:\\d[\\ |-]?){13})$",
            thesisRepository::existsByPrintISBN,
            thesis::setPrintISBN,
            "printIsbnFormatError",
            "printIsbnExistsError"
        );
    }

    private void indexThesis(Thesis thesis, DocumentPublicationIndex index) {
        indexCommonFields(thesis, index);

        index.setType(DocumentPublicationType.THESIS.name());
        index.setPublicationType(thesis.getThesisType().name());
        if (Objects.nonNull(thesis.getPublisher())) {
            index.setPublisherId(thesis.getPublisher().getId());
        }
        index.setAuthorReprint(thesis.getAuthorReprint());

        if (Objects.nonNull(thesis.getOrganisationUnit())) {
            if (!index.getOrganisationUnitIds().contains(thesis.getOrganisationUnit().getId())) {
                index.getOrganisationUnitIds().add(thesis.getOrganisationUnit().getId());
            }
        }

        index.setResearchOutputIds(
            thesisResearchOutputRepository.findResearchOutputIdsForThesis(thesis.getId()));
        index.setTopicAcceptanceDate(thesis.getTopicAcceptanceDate());
        index.setThesisDefenceDate(thesis.getThesisDefenceDate());
        index.setPublicReviewStartDates(thesis.getPublicReviewStartDates().stream().toList());
        if (!index.getPublicReviewStartDates().isEmpty()) {
            index.setLatestPublicReviewStartDate(index.getPublicReviewStartDates().getLast());
        }
        index.setIsOnPublicReview(thesis.getIsOnPublicReview());
        index.setIsPublicReviewCompleted(thesis.getPublicReviewCompleted());

        if (Objects.nonNull(thesis.getOrganisationUnit())) {
            index.setThesisInstitutionId(thesis.getOrganisationUnit().getId());
            organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                thesis.getOrganisationUnit().getId()).ifPresent(institutionIndex -> {
                index.setThesisInstitutionNameSr(institutionIndex.getNameSr());
                index.setThesisInstitutionNameOther(institutionIndex.getNameOther());
            });
        }

        indexScientificArea(thesis, index);

        thesis.getContributors().stream().filter(contribution -> contribution.getContributionType()
            .equals(DocumentContributionType.AUTHOR)).findFirst().ifPresent(authorship -> {
            index.setThesisAuthorName(
                authorship.getAffiliationStatement().getDisplayPersonName().getLastname() + " " +
                    authorship.getAffiliationStatement().getDisplayPersonName().getFirstname());
        });

        index.setApa(
            citationService.craftCitationInGivenStyle("apa", index, LanguageAbbreviations.ENGLISH));
        documentPublicationIndexRepository.save(index);
    }

    private void checkIfAvailableForEditing(Thesis thesis) {
        if (thesis.getIsOnPublicReview()) {
            throw new ThesisException("Public review is in progress, can't edit.");
        }

        if (thesis.getIsArchived()) {
            throw new ThesisException("Thesis is archived, can't edit.");
        }
    }

    private void indexScientificArea(Thesis thesis, DocumentPublicationIndex index) {
        var contentSr = new StringBuilder();
        var contentOther = new StringBuilder();

        multilingualContentService.buildLanguageStrings(contentSr, contentOther,
            thesis.getScientificArea(), true);

        StringUtil.removeTrailingDelimiters(contentSr, contentOther);
        index.setScientificFieldSr(
            !contentSr.isEmpty() ? contentSr.toString() : contentOther.toString());
        index.setScientificFieldOther(
            !contentOther.isEmpty() ? contentOther.toString() : contentSr.toString());
    }

    @Scheduled(cron = "${thesis.check-public-review-end.period}")
    @Transactional
    protected void removeFromPublicReviewScheduledFallback() {
        if (Objects.nonNull(fallbackPublicReviewCheckEnabled) && fallbackPublicReviewCheckEnabled) {
            removeFromPublicReview(List.of(ThesisType.PHD, ThesisType.PHD_ART_PROJECT),
                daysOnPublicReview);
        }
    }

    private void removeFromPublicReview(List<ThesisType> thesisTypes,
                                        Integer publicReviewLengthDays) {
        var thesesOnPublicReview = thesisRepository.findAllOnPublicReviewOfGivenTypes(thesisTypes);

        var latestPossibleStartDate = LocalDate.now().minusDays(publicReviewLengthDays);
        var thesesByInstitution =
            new ConcurrentHashMap<Integer, List<Triple<String, Set<MultiLingualContent>, LocalDate>>>();

        thesesOnPublicReview.stream()
            .filter(thesis -> isPublicReviewExpired(thesis, latestPossibleStartDate))
            .forEach(thesis -> {
                updateThesisAndIndex(thesis);

                var institutionId = thesis.getOrganisationUnit().getId();
                thesesByInstitution.putIfAbsent(institutionId, new ArrayList<>());

                var authorName = getAuthorName(thesis);
                if (Objects.isNull(authorName)) {
                    return; // should never happen
                }

                var latestReviewDate = thesis.getPublicReviewStartDates().stream()
                    .max(Comparator.naturalOrder())
                    .orElse(LocalDate.now());

                thesesByInstitution.get(institutionId).add(
                    new Triple<>(authorName, thesis.getTitle(), latestReviewDate));
            });

        notifyLibrarians(thesesByInstitution, publicReviewLengthDays);
    }

    private boolean isPublicReviewExpired(Thesis thesis, LocalDate cutoffDate) {
        return thesis.getPublicReviewStartDates().stream()
            .max(Comparator.naturalOrder())
            .map(date -> date.isBefore(cutoffDate))
            .orElse(false);
    }

    private void updateThesisAndIndex(Thesis thesis) {
        thesis.setIsOnPublicReview(false);
        thesis.setPublicReviewCompleted(true);
        thesisJPAService.save(thesis);

        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesis.getId())
            .ifPresent(index -> {
                index.setIsOnPublicReview(false);
                index.setIsPublicReviewCompleted(true);
                documentPublicationIndexRepository.save(index);
            });
    }

    private String getAuthorName(Thesis thesis) {
        return thesis.getContributors().stream()
            .filter(c -> c.getContributionType().equals(DocumentContributionType.AUTHOR))
            .findFirst()
            .map(c -> c.getAffiliationStatement().getDisplayPersonName().toString())
            .orElse(null);
    }

    private void notifyLibrarians(
        Map<Integer, List<Triple<String, Set<MultiLingualContent>, LocalDate>>> thesesByInstitution,
        Integer publicReviewLengthDays) {
        userService.findAllLibrarianUsers().forEach(librarianUser -> {
            var thesesList = new StringBuilder();
            var preferredLocale = librarianUser.getPreferredUILanguage().getLanguageTag();

            if (Objects.isNull(librarianUser.getOrganisationUnit())) {
                log.error("{} user with ID {} and email {} does not have an OU bound to him!",
                    librarianUser.getAuthority().getName(), librarianUser.getId(),
                    librarianUser.getEmail());
                return;
            }

            for (var institutionId : organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                librarianUser.getOrganisationUnit().getId())) {

                if (thesesByInstitution.containsKey(institutionId)) {
                    var institutionName = organisationUnitService.findOne(institutionId).getName();

                    thesesByInstitution.get(institutionId).forEach(content ->
                        thesesList.append(StringEscapeUtils.escapeHtml4(content.a)).append(" - ")
                            .append(StringEscapeUtils.escapeHtml4(
                                getLocalisedContentString(institutionName, preferredLocale)))
                            .append(" - ")
                            .append(StringEscapeUtils.escapeHtml4(
                                getLocalisedContentString(content.b, preferredLocale)))
                            .append(" (").append(dtFormatter.format(content.c)).append(" - ")
                            .append(dtFormatter.format(content.c.plusDays(publicReviewLengthDays)))
                            .append(")<br/>"));
                }
            }

            if (thesesList.isEmpty()) {
                return;
            }

            var subject = getMessage("public-review-end.email.subject", new Object[] {},
                preferredLocale.toLowerCase());

            var applicationTitle = brandingInformationService.readBrandingInformation().title();
            var body = getMessage("public-review-end.email.body",
                new Object[] {
                    thesesList.toString(),
                    getLocalisedContentString(applicationTitle, preferredLocale),
                    feedbackEmail
                },
                preferredLocale.toLowerCase());

            emailUtil.sendHTMLSupportedEmail(librarianUser.getEmail(), subject, body);
        });
    }

    private String getLocalisedContentString(Set<MultiLingualContent> contentList,
                                             String languageTag) {
        return getLocalisedContentStringGeneric(
            contentList,
            languageTag,
            content -> content.getLanguage().getLanguageTag(),
            MultiLingualContent::getContent
        );
    }

    private String getLocalisedContentString(List<MultilingualContentDTO> contentList,
                                             String languageTag) {
        return getLocalisedContentStringGeneric(
            contentList,
            languageTag,
            MultilingualContentDTO::getLanguageTag,
            MultilingualContentDTO::getContent
        );
    }

    private <T> String getLocalisedContentStringGeneric(Collection<T> contentList,
                                                        String languageTag,
                                                        Function<T, String> languageExtractor,
                                                        Function<T, String> contentExtractor) {
        for (T content : contentList) {
            if (languageExtractor.apply(content).equalsIgnoreCase(languageTag)) {
                return contentExtractor.apply(content);
            }
        }

        return contentList.stream()
            .findFirst()
            .map(contentExtractor)
            .orElse(""); // should never happen, just a failsafe
    }

    private String getMessage(String key, Object[] args, String locale) {
        try {
            return messageSource.getMessage(key, args, Locale.forLanguageTag(locale));
        } catch (NoSuchMessageException e) {
            return messageSource.getMessage(key, args, Locale.ENGLISH);
        }
    }
}
