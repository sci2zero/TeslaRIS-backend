package rs.teslaris.core.service.impl.document;

import jakarta.xml.bind.JAXBException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.document.ThesisConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisLibraryFormatsResponseDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisAttachmentType;
import rs.teslaris.core.model.document.ThesisPhysicalDescription;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.repository.document.ThesisResearchOutputRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ThesisJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.xmlutil.XMLUtil;

@Service
@Transactional
@Slf4j
@Traceable
public class ThesisServiceImpl extends DocumentPublicationServiceImpl implements ThesisService {

    private final Pattern udcPattern =
        Pattern.compile("^\\d{1,3}([.:/]\\d{1,5})*(\\(\\d{1,5}(\\.\\d{1,5})?\\))?$\n",
            Pattern.CASE_INSENSITIVE);

    private final ThesisJPAServiceImpl thesisJPAService;

    private final PublisherService publisherService;

    private final LanguageService languageService;

    private final LanguageTagService languageTagService;

    private final ThesisRepository thesisRepository;

    private final ThesisResearchOutputRepository thesisResearchOutputRepository;

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
                             SearchFieldsLoader searchFieldsLoader,
                             ThesisJPAServiceImpl thesisJPAService,
                             PublisherService publisherService,
                             LanguageService languageService,
                             LanguageTagService languageTagService,
                             ThesisRepository thesisRepository,
                             ThesisResearchOutputRepository thesisResearchOutputRepository) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService,
            personContributionService,
            expressionTransformer, eventService, commissionRepository, searchFieldsLoader);
        this.thesisJPAService = thesisJPAService;
        this.publisherService = publisherService;
        this.languageService = languageService;
        this.languageTagService = languageTagService;
        this.thesisRepository = thesisRepository;
        this.thesisResearchOutputRepository = thesisResearchOutputRepository;
    }

    @Override
    public Thesis getThesisById(Integer thesisId) {
        return thesisJPAService.findOne(thesisId);
    }

    @Override
    public ThesisResponseDTO readThesisById(Integer thesisId) {
        Thesis thesis;
        try {
            thesis = thesisJPAService.findOne(thesisId);
        } catch (NotFoundException e) {
            this.clearIndexWhenFailedRead(thesisId);
            throw e;
        }

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
        checkIfAvailableForEditing(thesisToDelete);

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
    public void indexThesis(Thesis thesis) {
        indexThesis(thesis,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                thesis.getId()).orElse(new DocumentPublicationIndex()));
    }

    @Override
    public DocumentFileResponseDTO addThesisAttachment(Integer thesisId, DocumentFileDTO document,
                                                       ThesisAttachmentType attachmentType) {
        var thesis = thesisJPAService.findOne(thesisId);

        checkIfAvailableForEditing(thesis);

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
    public void putOnPublicReview(Integer thesisId, Boolean continueLastReview) {
        var thesis = thesisJPAService.findOne(thesisId);
        validateThesisForPublicReview(thesis);

        thesis.setIsOnPublicReview(true);
        updatePublicReviewDates(thesis, continueLastReview);

        thesis.setIsOnPublicReviewPause(false);
        thesisJPAService.save(thesis);
        indexThesis(thesis,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId)
                .orElse(new DocumentPublicationIndex()));
    }

    private void validateThesisForPublicReview(Thesis thesis) {
        if (!isPhdThesis(thesis)) {
            throw new ThesisException("Only PHD theses can be put on public reviews.");
        }

        if (thesis.getIsOnPublicReview()) {
            throw new ThesisException("Already on public review.");
        }

        if (thesis.getPreliminaryFiles().isEmpty() || thesis.getCommissionReports().isEmpty()) {
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

    private void updatePublicReviewDates(Thesis thesis, Boolean continueLastReview) {
        if (thesis.getIsOnPublicReviewPause() && !continueLastReview) {
            thesis.getPublicReviewStartDates()
                .stream()
                .max(Comparator.naturalOrder())
                .ifPresent(thesis.getPublicReviewStartDates()::remove);
        }
        thesis.getPublicReviewStartDates().add(LocalDate.now());
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
        thesis.setIsOnPublicReviewPause(true);

        thesisJPAService.save(thesis);
        indexThesis(thesis,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId)
                .orElse(new DocumentPublicationIndex()));
    }

    @Override
    public void archiveThesis(Integer thesisId) {
        var thesis = thesisJPAService.findOne(thesisId);

        if (thesis.getTitle().isEmpty() || Objects.isNull(thesis.getThesisDefenceDate()) ||
            Objects.isNull(thesis.getDocumentDate())) {
            throw new ThesisException("missingDataToArchiveMessage");
        }

        thesis.setIsArchived(true);
        thesisJPAService.save(thesis);
    }

    @Override
    public void unarchiveThesis(Integer thesisId) {
        var thesis = thesisJPAService.findOne(thesisId);
        thesis.setIsArchived(false);

        thesisJPAService.save(thesis);
    }

    @Override
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

    private void setThesisRelatedFields(Thesis thesis, ThesisDTO thesisDTO) {
        thesis.setThesisType(thesisDTO.getThesisType());
        thesis.setTopicAcceptanceDate(thesisDTO.getTopicAcceptanceDate());

        thesis.setAlternateTitle(
            multilingualContentService.getMultilingualContent(thesisDTO.getAlternateTitle()));
        thesis.setExtendedAbstract(
            multilingualContentService.getMultilingualContent(thesisDTO.getExtendedAbstract()));
        thesis.setRemark(multilingualContentService.getMultilingualContent(thesisDTO.getRemark()));

        thesis.setPhysicalDescription(new ThesisPhysicalDescription() {{
            setNumberOfPages(thesisDTO.getNumberOfPages());
            setNumberOfChapters(thesisDTO.getNumberOfChapters());
            setNumberOfReferences(thesisDTO.getNumberOfReferences());
            setNumberOfGraphs(thesisDTO.getNumberOfGraphs());
            setNumberOfIllustrations(thesisDTO.getNumberOfIllustrations());
            setNumberOfTables(thesisDTO.getNumberOfTables());
            setNumberOfAppendices(thesisDTO.getNumberOfAppendices());
        }});

        thesis.setThesisDefenceDate(thesisDTO.getThesisDefenceDate());
        if (Objects.nonNull(thesisDTO.getThesisDefenceDate())) {
            thesis.setDocumentDate(String.valueOf(thesisDTO.getThesisDefenceDate().getYear()));
        }

        if (Objects.nonNull(thesisDTO.getPublisherId())) {
            thesis.setPublisher(publisherService.findOne(thesisDTO.getPublisherId()));
        }

        thesis.setScientificArea(thesisDTO.getScientificArea());
        thesis.setScientificSubArea(thesisDTO.getScientificSubArea());
        thesis.setPlaceOfKeeping(thesisDTO.getPlaceOfKeep());

        if (Objects.nonNull(thesisDTO.getUdc()) &&
            udcPattern.matcher(thesisDTO.getUdc()).matches()) {
            thesis.setUdc(thesisDTO.getUdc());
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

        setCommonIdentifiers(thesis, thesisDTO);
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
