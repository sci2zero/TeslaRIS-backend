package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.document.ProceedingsConverter;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ProceedingJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
@Transactional
public class ProceedingsServiceImpl extends DocumentPublicationServiceImpl
    implements ProceedingsService {

    private final ProceedingJPAServiceImpl proceedingJPAService;

    private final ProceedingsRepository proceedingsRepository;

    private final LanguageTagService languageTagService;

    private final JournalService journalService;

    private final EventService eventService;

    private final PublisherService publisherService;


    @Autowired
    public ProceedingsServiceImpl(MultilingualContentService multilingualContentService,
                                  DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                  DocumentRepository documentRepository,
                                  DocumentFileService documentFileService,
                                  PersonContributionService personContributionService,
                                  SearchService<DocumentPublicationIndex> searchService,
                                  ExpressionTransformer expressionTransformer,
                                  EventService eventService,
                                  ProceedingJPAServiceImpl proceedingJPAService,
                                  ProceedingsRepository proceedingsRepository,
                                  LanguageTagService languageTagService,
                                  JournalService journalService, EventService eventService1,
                                  PublisherService publisherService) {
        super(multilingualContentService, documentPublicationIndexRepository, documentRepository,
            documentFileService,
            personContributionService, searchService, expressionTransformer, eventService);
        this.proceedingJPAService = proceedingJPAService;
        this.proceedingsRepository = proceedingsRepository;
        this.languageTagService = languageTagService;
        this.journalService = journalService;
        this.eventService = eventService1;
        this.publisherService = publisherService;
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
        return proceedingJPAService.findOne(proceedingsId);
    }

    @Override
    public Proceedings createProceedings(ProceedingsDTO proceedingsDTO) {
        var proceedings = new Proceedings();
        proceedings.setLanguages(new HashSet<>());

        setCommonFields(proceedings, proceedingsDTO);
        setProceedingsRelatedFields(proceedings, proceedingsDTO);

        proceedings.setApproveStatus(ApproveStatus.APPROVED);

        var savedProceedings = proceedingJPAService.save(proceedings);

        if (proceedings.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexProceedings(savedProceedings, new DocumentPublicationIndex());
        }

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

        proceedingJPAService.save(proceedingsToUpdate);
    }

    @Override
    public void deleteProceedings(Integer proceedingsId) {
        var proceedingsToDelete = findProceedingsById(proceedingsId);

//        TODO: Should we delete files if we have soft delete
//        deleteProofsAndFileItems(proceedingsToDelete);
        proceedingJPAService.delete(proceedingsId);

        if (proceedingsToDelete.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            documentPublicationIndexRepository.delete(
                findDocumentPublicationIndexByDatabaseId(proceedingsId));
        }
    }

    @Override
    public void indexProceedings(Proceedings proceedings, DocumentPublicationIndex index) {
        indexCommonFields(proceedings, index);

        if (proceedings.getPublicationSeries() != null) {
            index.setPublicationSeriesId(proceedings.getPublicationSeries().getId());
        }

        if (proceedings.getPublisher() != null) {
            index.setPublisherId(proceedings.getPublisher().getId());
        }

        index.setEventId(proceedings.getEvent().getId());
        index.setType(DocumentPublicationType.PROCEEDINGS.name());

        documentPublicationIndexRepository.save(index);
    }

    private void setProceedingsRelatedFields(Proceedings proceedings,
                                             ProceedingsDTO proceedingsDTO) {
        proceedings.setEISBN(proceedingsDTO.getEISBN());
        proceedings.setPrintISBN(proceedingsDTO.getPrintISBN());
        proceedings.setNumberOfPages(proceedingsDTO.getNumberOfPages());
        proceedings.setPublicationSeriesVolume(proceedingsDTO.getPublicationSeriesVolume());
        proceedings.setPublicationSeriesIssue(proceedingsDTO.getPublicationSeriesIssue());

        proceedingsDTO.getLanguageTagIds().forEach(id -> {
            proceedings.getLanguages().add(languageTagService.findLanguageTagById(id));
        });

        proceedings.setEvent(eventService.findEventById(proceedingsDTO.getEventId()));

        if (proceedingsDTO.getPublicationSeriesId() != null) {
            proceedings.setPublicationSeries(
                journalService.findJournalById(proceedingsDTO.getPublicationSeriesId()));
        }

        if (proceedingsDTO.getPublisherId() != null) {
            proceedings.setPublisher(
                publisherService.findPublisherById(proceedingsDTO.getPublisherId()));
        }

    }
}
