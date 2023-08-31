package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.JournalConverter;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.JournalReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class JournalServiceImpl extends JPAServiceImpl<PublicationSeries>
    implements JournalService {

    private final JournalRepository journalRepository;

    private final MultilingualContentService multilingualContentService;

    private final LanguageTagService languageTagService;

    private final PersonContributionService personContributionService;

    @Override
    protected JpaRepository<PublicationSeries, Integer> getEntityRepository() {
        return journalRepository;
    }

    @Override
    public Page<JournalResponseDTO> readAllJournals(Pageable pageable) {
        return journalRepository.findAll(pageable).map(JournalConverter::toDTO);
    }

    @Override
    public JournalResponseDTO readJournal(Integer journalId) {
        return JournalConverter.toDTO(findOne(journalId));
    }

    @Override
    @Deprecated(forRemoval = true)
    public PublicationSeries findJournalById(Integer journalId) {
        return journalRepository.findById(journalId)
            .orElseThrow(
                () -> new NotFoundException("PublicationSeries with given id does not exist."));
    }

    @Override
    public PublicationSeries createJournal(JournalDTO journalDTO) {
        var journal = new PublicationSeries();
        journal.setLanguages(new HashSet<>());

        setCommonFields(journal, journalDTO);

        return journalRepository.save(journal);
    }

    @Override
    public void updateJournal(JournalDTO journalDTO, Integer journalId) {
        var journalToUpdate = findOne(journalId);
        journalToUpdate.getLanguages().clear();

        setCommonFields(journalToUpdate, journalDTO);

        journalRepository.save(journalToUpdate);
    }

    @Override
    public void deleteJournal(Integer journalId) {
        if (journalRepository.hasPublication(journalId) ||
            journalRepository.hasProceedings(journalId)) {
            throw new JournalReferenceConstraintViolationException(
                "PublicationSeries with given ID is allready in use.");
        }

        this.delete(journalId);
    }

    private void setCommonFields(PublicationSeries publicationSeries, JournalDTO journalDTO) {
        publicationSeries.setTitle(
            multilingualContentService.getMultilingualContent(journalDTO.getTitle()));
        publicationSeries.setNameAbbreviation(
            multilingualContentService.getMultilingualContent(journalDTO.getNameAbbreviation()));

        publicationSeries.setEISSN(journalDTO.getEISSN());
        publicationSeries.setPrintISSN(journalDTO.getPrintISSN());

        personContributionService.setPersonJournalContributionsForJournal(publicationSeries,
            journalDTO);

        journalDTO.getLanguageTagIds().forEach(languageTagId -> {
            publicationSeries.getLanguages()
                .add(languageTagService.findLanguageTagById(languageTagId));
        });
    }
}
