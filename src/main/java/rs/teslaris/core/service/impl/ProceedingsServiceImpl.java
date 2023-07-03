package rs.teslaris.core.service.impl;

import java.util.HashSet;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.ProceedingsConverter;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.EventService;
import rs.teslaris.core.service.JournalService;
import rs.teslaris.core.service.LanguageTagService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.PersonContributionService;
import rs.teslaris.core.service.ProceedingsService;
import rs.teslaris.core.service.PublisherService;

@Service
@Transactional
public class ProceedingsServiceImpl extends DocumentPublicationServiceImpl
    implements ProceedingsService {

    private final ProceedingsRepository proceedingsRepository;

    private final LanguageTagService languageTagService;

    private final JournalService journalService;

    private final EventService eventService;

    private final PublisherService publisherService;


    public ProceedingsServiceImpl(MultilingualContentService multilingualContentService,
                                  DocumentRepository documentRepository,
                                  DocumentFileService documentFileService,
                                  PersonContributionService personContributionService,
                                  ProceedingsRepository proceedingsRepository,
                                  LanguageTagService languageTagService,
                                  JournalService journalService, EventService eventService,
                                  PublisherService publisherService) {
        super(multilingualContentService, documentRepository, documentFileService,
            personContributionService);
        this.proceedingsRepository = proceedingsRepository;
        this.languageTagService = languageTagService;
        this.journalService = journalService;
        this.eventService = eventService;
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
    public Proceedings findProceedingsById(Integer proceedingsId) {
        return proceedingsRepository.findById(proceedingsId)
            .orElseThrow(() -> new NotFoundException("Proceedings with given ID does not exist."));
    }

    @Override
    public Proceedings createProceedings(ProceedingsDTO proceedingsDTO) {
        var proceedings = new Proceedings();
        proceedings.setLanguages(new HashSet<>());

        setCommonFields(proceedings, proceedingsDTO);
        setProceedingsRelatedFields(proceedings, proceedingsDTO);

        return proceedingsRepository.save(proceedings);
    }

    @Override
    public void updateProceedings(Integer proceedingsId, ProceedingsDTO proceedingsDTO) {
        var proceedingsToUpdate = findProceedingsById(proceedingsId);

        proceedingsToUpdate.getLanguages().clear();
        clearCommonFields(proceedingsToUpdate);

        setCommonFields(proceedingsToUpdate, proceedingsDTO);
        setProceedingsRelatedFields(proceedingsToUpdate, proceedingsDTO);

        proceedingsRepository.save(proceedingsToUpdate);
    }

    @Override
    public void deleteProceedings(Integer proceedingsId) {
        var proceedingsToDelete = findProceedingsById(proceedingsId);

        deleteProofsAndFileItems(proceedingsToDelete);

        proceedingsRepository.delete(proceedingsToDelete);
    }

    private void setProceedingsRelatedFields(Proceedings proceedings,
                                             ProceedingsDTO proceedingsDTO) {
        proceedings.setEISBN(proceedingsDTO.getEISBN());
        proceedings.setPrintISBN(proceedingsDTO.getPrintISBN());
        proceedings.setNumberOfPages(proceedingsDTO.getNumberOfPages());
        proceedings.setEditionTitle(proceedingsDTO.getEditionTitle());
        proceedings.setEditionNumber(proceedingsDTO.getEditionNumber());
        proceedings.setEditionISSN(proceedingsDTO.getEditionISSN());

        proceedingsDTO.getLanguageTagIds().forEach(id -> {
            proceedings.getLanguages().add(languageTagService.findLanguageTagById(id));
        });

        proceedings.setJournal(journalService.findJournalById(proceedingsDTO.getJournalId()));
        proceedings.setJournalVolume(proceedingsDTO.getJournalVolume());
        proceedings.setJournalIssue(proceedingsDTO.getJournalIssue());

        proceedings.setEvent(eventService.findEventById(proceedingsDTO.getEventId()));
        proceedings.setPublisher(
            publisherService.findPublisherById(proceedingsDTO.getPublisherId()));
    }
}
