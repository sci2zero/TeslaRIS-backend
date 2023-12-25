package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.HashSet;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.BookSeriesConverter;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.JournalReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@Service
@RequiredArgsConstructor
@Transactional
public class JournalServiceImpl extends JPAServiceImpl<Journal>
    implements JournalService {

    private final JournalRepository journalRepository;

    private final MultilingualContentService multilingualContentService;

    private final LanguageTagService languageTagService;

    private final PersonContributionService personContributionService;

    private final SearchService<JournalIndex> searchService;

    private final JournalIndexRepository journalIndexRepository;

    private final EmailUtil emailUtil;


    @Override
    protected JpaRepository<Journal, Integer> getEntityRepository() {
        return journalRepository;
    }

    @Override
    public Page<JournalResponseDTO> readAllJournals(Pageable pageable) {
        return journalRepository.findAll(pageable).map(BookSeriesConverter::toDTO);
    }

    @Override
    public Page<JournalIndex> searchJournals(List<String> tokens, Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens), pageable, JournalIndex.class,
            "journal");
    }

    @Override
    public JournalResponseDTO readJournal(Integer journalId) {
        return BookSeriesConverter.toDTO(findOne(journalId));
    }

    @Override
    @Deprecated(forRemoval = true)
    public Journal findJournalById(Integer journalId) {
        return journalRepository.findById(journalId)
            .orElseThrow(
                () -> new NotFoundException("PublicationSeries with given id does not exist."));
    }

    @Override
    public Journal createJournal(JournalDTO journalDTO) {
        var journal = new Journal();
        journal.setLanguages(new HashSet<>());

        var index = new JournalIndex();

        setCommonFields(journal, journalDTO);
        indexCommonFields(journal, index);

        var savedJournal = journalRepository.save(journal);
        index.setDatabaseId(savedJournal.getId());
        journalIndexRepository.save(index);

        return savedJournal;
    }

    @Override
    public Journal createJournal(JournalBasicAdditionDTO journalDTO) {
        var journal = new Journal();

        journal.setLanguages(new HashSet<>());
        journal.setContributions(new HashSet<>());
        journal.setNameAbbreviation(new HashSet<>());

        journal.setTitle(multilingualContentService.getMultilingualContent(journalDTO.getTitle()));
        journal.setEISSN(journalDTO.getEISSN());
        journal.setPrintISSN(journalDTO.getPrintISSN());

        var savedJournal = journalRepository.save(journal);

        emailUtil.notifyInstitutionalEditor(savedJournal.getId(), "journal");

        return savedJournal;
    }

    @Override
    public void updateJournal(JournalDTO journalDTO, Integer journalId) {
        var journalToUpdate = findOne(journalId);
        journalToUpdate.getLanguages().clear();

        var indexToUpdate = journalIndexRepository.findJournalIndexByDatabaseId(journalId)
            .orElse(new JournalIndex());
        indexToUpdate.setDatabaseId(journalId);

        setCommonFields(journalToUpdate, journalDTO);
        indexCommonFields(journalToUpdate, indexToUpdate);

        journalRepository.save(journalToUpdate);
        journalIndexRepository.save(indexToUpdate);
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

    private void setCommonFields(Journal journal, JournalDTO journalDTO) {
        journal.setTitle(
            multilingualContentService.getMultilingualContent(journalDTO.getTitle()));
        journal.setNameAbbreviation(
            multilingualContentService.getMultilingualContent(journalDTO.getNameAbbreviation()));

        journal.setEISSN(journalDTO.getEISSN());
        journal.setPrintISSN(journalDTO.getPrintISSN());

        personContributionService.setPersonPublicationSeriesContributionsForJournal(journal,
            journalDTO);

        journalDTO.getLanguageTagIds().forEach(languageTagId -> {
            journal.getLanguages()
                .add(languageTagService.findLanguageTagById(languageTagId));
        });
    }

    private void indexCommonFields(Journal journal, JournalIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        journal.getTitle().forEach(content -> {
            if (content.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                srContent.append(content.getContent()).append(" | ");
            } else {
                otherContent.append(content.getContent()).append(" | ");
            }
        });

        journal.getNameAbbreviation().forEach(content -> {
            if (content.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                srContent.append(content.getContent()).append(" | ");
            } else {
                otherContent.append(content.getContent()).append(" | ");
            }
        });

        index.setTitleSr(srContent.toString());
        index.setTitleOther(otherContent.toString());
        index.setEISSN(journal.getEISSN());
        index.setPrintISSN(journal.getPrintISSN());
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                b.should(sb -> sb.match(
                    m -> m.field("title_sr").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("title_other").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("e_issn").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("print_issn").query(token)));
            });
            return b;
        })))._toQuery();
    }
}
