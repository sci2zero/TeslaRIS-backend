package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.document.ProceedingsConverter;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ProceedingsJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ProceedingsReferenceConstraintViolationException;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
@Transactional
public class ProceedingsServiceImpl extends DocumentPublicationServiceImpl
    implements ProceedingsService {

    private final ProceedingsJPAServiceImpl proceedingsJPAService;

    private final ProceedingsRepository proceedingsRepository;

    private final LanguageTagService languageTagService;

    private final JournalService journalService;

    private final BookSeriesService bookSeriesService;

    private final EventService eventService;

    private final PublisherService publisherService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    @Autowired
    public ProceedingsServiceImpl(MultilingualContentService multilingualContentService,
                                  DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                  SearchService<DocumentPublicationIndex> searchService,
                                  OrganisationUnitService organisationUnitService,
                                  DocumentRepository documentRepository,
                                  DocumentFileService documentFileService,
                                  PersonContributionService personContributionService,
                                  ExpressionTransformer expressionTransformer,
                                  EventService eventService,
                                  ProceedingsJPAServiceImpl proceedingsJPAService,
                                  ProceedingsRepository proceedingsRepository,
                                  LanguageTagService languageTagService,
                                  JournalService journalService,
                                  BookSeriesService bookSeriesService, EventService eventService1,
                                  PublisherService publisherService,
                                  DocumentPublicationIndexRepository documentPublicationIndexRepository1) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService,
            personContributionService,
            expressionTransformer, eventService);
        this.proceedingsJPAService = proceedingsJPAService;
        this.proceedingsRepository = proceedingsRepository;
        this.languageTagService = languageTagService;
        this.journalService = journalService;
        this.bookSeriesService = bookSeriesService;
        this.eventService = eventService1;
        this.publisherService = publisherService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository1;
    }

    @Override
    public ProceedingsResponseDTO readProceedingsById(Integer proceedingsId) {
        var proceedings = findProceedingsById(proceedingsId);
        if (!proceedings.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Proceedings with given ID does not exist.");
        }
        return ProceedingsConverter.toDTO(proceedings);
    }

    @Override
    public List<ProceedingsResponseDTO> readProceedingsForEventId(Integer eventId) {
        return proceedingsRepository.findProceedingsForEventId(eventId).stream()
            .map(ProceedingsConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    public Proceedings findProceedingsById(Integer proceedingsId) {
        return proceedingsJPAService.findOne(proceedingsId);
    }

    @Override
    public Proceedings createProceedings(ProceedingsDTO proceedingsDTO, boolean index) {
        var proceedings = new Proceedings();

        setCommonFields(proceedings, proceedingsDTO);
        setProceedingsRelatedFields(proceedings, proceedingsDTO);

        proceedings.setApproveStatus(ApproveStatus.APPROVED);

        var savedProceedings = proceedingsJPAService.save(proceedings);

        if (proceedings.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexProceedings(savedProceedings, new DocumentPublicationIndex());
        }

        sendNotifications(savedProceedings);

        return savedProceedings;
    }

    @Override
    public void updateProceedings(Integer proceedingsId, ProceedingsDTO proceedingsDTO) {
        var proceedingsToUpdate = findProceedingsById(proceedingsId);

        proceedingsToUpdate.getLanguages().clear();
        clearCommonFields(proceedingsToUpdate);

        setCommonFields(proceedingsToUpdate, proceedingsDTO);
        setProceedingsRelatedFields(proceedingsToUpdate, proceedingsDTO);

        if (proceedingsToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            var proceedingsIndex = findDocumentPublicationIndexByDatabaseId(proceedingsId);
            indexProceedings(proceedingsToUpdate, proceedingsIndex);
        }

        proceedingsJPAService.save(proceedingsToUpdate);

        sendNotifications(proceedingsToUpdate);
    }

    @Override
    public void deleteProceedings(Integer proceedingsId) {
        var proceedingsToDelete = findProceedingsById(proceedingsId);

        if (proceedingsRepository.hasPublications(proceedingsId)) {
            throw new ProceedingsReferenceConstraintViolationException("proceedingsInUse");
        }

//        TODO: Should we delete files if we have soft delete
//        deleteProofsAndFileItems(proceedingsToDelete);
        proceedingsJPAService.delete(proceedingsId);

        if (proceedingsToDelete.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            documentPublicationIndexRepository.delete(
                findDocumentPublicationIndexByDatabaseId(proceedingsId));
        }
    }

    @Override
    public void forceDeleteProceedings(Integer proceedingsId) {
        proceedingsRepository.deleteAllPublicationsInProceedings(proceedingsId);

        proceedingsJPAService.delete(proceedingsId);

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            proceedingsId);
        index.ifPresent(documentPublicationIndexRepository::delete);

        documentPublicationIndexRepository.deleteByProceedingsId(proceedingsId);
    }

    @Override
    public void indexProceedings(Proceedings proceedings, DocumentPublicationIndex index) {
        indexCommonFields(proceedings, index);

        if (proceedings.getPublisher() != null) {
            index.setPublisherId(proceedings.getPublisher().getId());
        }

        index.setType(DocumentPublicationType.PROCEEDINGS.name());

        if (Objects.nonNull(proceedings.getPublicationSeries())) {
            index.setPublicationSeriesId(proceedings.getPublicationSeries().getId());
            if (proceedings.getPublicationSeries() instanceof Journal journal) {
                index.setJournalId(journal.getId());
            }
        }

        documentPublicationIndexRepository.save(index);
    }

    @Override
    public void reindexProceedings() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Proceedings> chunk =
                proceedingsJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach(
                (proceedings) -> indexProceedings(proceedings, new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void setProceedingsRelatedFields(Proceedings proceedings,
                                             ProceedingsDTO proceedingsDTO) {
        setCommonIdentifiers(proceedings, proceedingsDTO);

        proceedings.setNumberOfPages(proceedingsDTO.getNumberOfPages());
        proceedings.setPublicationSeriesVolume(proceedingsDTO.getPublicationSeriesVolume());
        proceedings.setPublicationSeriesIssue(proceedingsDTO.getPublicationSeriesIssue());

        proceedingsDTO.getLanguageTagIds().forEach(id -> {
            proceedings.getLanguages().add(languageTagService.findLanguageTagById(id));
        });

        proceedings.setEvent(eventService.findEventById(proceedingsDTO.getEventId()));

        if (proceedingsDTO.getPublicationSeriesId() != null) {
            var optionalJournal =
                journalService.tryToFindById(proceedingsDTO.getPublicationSeriesId());

            if (optionalJournal.isPresent()) {
                proceedings.setPublicationSeries(optionalJournal.get());
            } else {
                var bookSeries = bookSeriesService.findBookSeriesById(
                    proceedingsDTO.getPublicationSeriesId());
                proceedings.setPublicationSeries(bookSeries);
            }
        }

        if (proceedingsDTO.getPublisherId() != null) {
            proceedings.setPublisher(
                publisherService.findPublisherById(proceedingsDTO.getPublisherId()));
        }
    }

    private void setCommonIdentifiers(Proceedings proceedings, ProceedingsDTO proceedingsDTO) {
        IdentifierUtil.validateAndSetIdentifier(
            proceedingsDTO.getEISBN(),
            proceedings.getId(),
            "^(?:(?:\\d[\\ |-]?){9}[\\dX]|(?:\\d[\\ |-]?){13})$",
            proceedingsRepository::existsByeISBN,
            proceedings::setEISBN,
            "eisbnFormatError",
            "eisbnExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            proceedingsDTO.getPrintISBN(),
            proceedings.getId(),
            "^(?:(?:\\d[\\ |-]?){9}[\\dX]|(?:\\d[\\ |-]?){13})$",
            proceedingsRepository::existsByPrintISBN,
            proceedings::setPrintISBN,
            "printIsbnFormatError",
            "printIsbnExistsError"
        );
    }
}
