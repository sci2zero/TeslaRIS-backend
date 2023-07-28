package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.JournalConverter;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.JournalInUseException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class JournalServiceImpl implements JournalService {

    private final JournalRepository journalRepository;

    private final MultilingualContentService multilingualContentService;

    private final LanguageTagService languageTagService;

    private final PersonContributionService personContributionService;


    @Override
    public Page<JournalResponseDTO> readAllJournals(Pageable pageable) {
        return journalRepository.findAll(pageable).map(JournalConverter::toDTO);
    }

    @Override
    public JournalResponseDTO readJournal(Integer journalId) {
        return JournalConverter.toDTO(findJournalById(journalId));
    }

    @Override
    public Journal findJournalById(Integer journalId) {
        return journalRepository.findById(journalId)
            .orElseThrow(() -> new NotFoundException("Journal with given id does not exist."));
    }

    @Override
    public Journal createJournal(JournalDTO journalDTO) {
        var journal = new Journal();
        journal.setLanguages(new HashSet<>());

        setCommonFields(journal, journalDTO);

        return journalRepository.save(journal);
    }

    @Override
    public void updateJournal(JournalDTO journalDTO, Integer journalId) {
        var journalToUpdate = findJournalById(journalId);
        journalToUpdate.getLanguages().clear();

        setCommonFields(journalToUpdate, journalDTO);

        journalRepository.save(journalToUpdate);
    }

    @Override
    public void deleteJournal(Integer journalId) {
        var journalToDelete = findJournalById(journalId);

        if (journalRepository.hasPublication(journalId) ||
            journalRepository.hasProceedings(journalId)) {
            throw new JournalInUseException("Journal with given ID is allready in use.");
        }

        journalRepository.delete(journalToDelete);
    }

    private void setCommonFields(Journal journal, JournalDTO journalDTO) {
        journal.setTitle(multilingualContentService.getMultilingualContent(journalDTO.getTitle()));
        journal.setNameAbbreviation(
            multilingualContentService.getMultilingualContent(journalDTO.getNameAbbreviation()));

        journal.setEISSN(journalDTO.getEISSN());
        journal.setPrintISSN(journalDTO.getPrintISSN());

        personContributionService.setPersonJournalContributionsForJournal(journal, journalDTO);

        journalDTO.getLanguageTagIds().forEach(languageTagId -> {
            journal.getLanguages().add(languageTagService.findLanguageTagById(languageTagId));
        });
    }
}
