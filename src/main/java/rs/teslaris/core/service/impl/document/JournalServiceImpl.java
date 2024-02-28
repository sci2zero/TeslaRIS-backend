package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.BookSeriesConverter;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.JournalJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.JournalReferenceConstraintViolationException;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Transactional
public class JournalServiceImpl extends PublicationSeriesServiceImpl implements JournalService {

    private final JournalJPAServiceImpl journalJPAService;

    private final SearchService<JournalIndex> searchService;

    private final JournalIndexRepository journalIndexRepository;

    private final JournalRepository journalRepository;


    @Autowired
    public JournalServiceImpl(PublicationSeriesRepository publicationSeriesRepository,
                              MultilingualContentService multilingualContentService,
                              LanguageTagService languageTagService,
                              PersonContributionService personContributionService,
                              EmailUtil emailUtil, JournalJPAServiceImpl journalJPAService,
                              SearchService<JournalIndex> searchService,
                              JournalIndexRepository journalIndexRepository,
                              JournalRepository journalRepository) {
        super(publicationSeriesRepository, multilingualContentService, languageTagService,
            personContributionService, emailUtil);
        this.journalJPAService = journalJPAService;
        this.searchService = searchService;
        this.journalIndexRepository = journalIndexRepository;
        this.journalRepository = journalRepository;
    }

    @Override
    public Page<JournalResponseDTO> readAllJournals(Pageable pageable) {
        return journalJPAService.findAll(pageable).map(BookSeriesConverter::toDTO);
    }

    @Override
    public Page<JournalIndex> searchJournals(List<String> tokens, Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens), pageable, JournalIndex.class,
            "journal");
    }

    @Override
    public JournalResponseDTO readJournal(Integer journalId) {
        return BookSeriesConverter.toDTO(journalJPAService.findOne(journalId));
    }

    @Override
    public Journal findJournalById(Integer journalId) {
        return journalJPAService.findOne(journalId);
    }

    @Override
    public Journal createJournal(PublicationSeriesDTO journalDTO) {
        var journal = new Journal();
        journal.setLanguages(new HashSet<>());

        var index = new JournalIndex();

        setPublicationSeriesCommonFields(journal, journalDTO);
        setJournalRelatedFields(journal, journalDTO);
        indexCommonFields(journal, index);

        var savedJournal = journalJPAService.save(journal);
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

        var savedJournal = journalJPAService.save(journal);

        emailUtil.notifyInstitutionalEditor(savedJournal.getId(), "journal");

        return savedJournal;
    }

    @Override
    public void updateJournal(PublicationSeriesDTO journalDTO, Integer journalId) {
        var journalToUpdate = journalJPAService.findOne(journalId);
        journalToUpdate.getLanguages().clear();

        var indexToUpdate = journalIndexRepository.findJournalIndexByDatabaseId(journalId)
            .orElse(new JournalIndex());
        indexToUpdate.setDatabaseId(journalId);

        setPublicationSeriesCommonFields(journalToUpdate, journalDTO);
        setJournalRelatedFields(journalToUpdate, journalDTO);
        indexCommonFields(journalToUpdate, indexToUpdate);

        journalJPAService.save(journalToUpdate);
        journalIndexRepository.save(indexToUpdate);
    }

    @Override
    public void deleteJournal(Integer journalId) {
        if (journalRepository.hasPublication(journalId) ||
            publicationSeriesRepository.hasProceedings(journalId)) {
            throw new JournalReferenceConstraintViolationException(
                "PublicationSeries with given ID is allready in use.");
        }

        journalJPAService.delete(journalId);
        var index = journalIndexRepository.findJournalIndexByDatabaseId(journalId);
        index.ifPresent(journalIndexRepository::delete);
    }

    private void setJournalRelatedFields(Journal journal, PublicationSeriesDTO journalDTO) {
        if (Objects.nonNull(journalDTO.getContributions())) {
            personContributionService.setPersonPublicationSeriesContributionsForJournal(journal,
                journalDTO);
        }
    }

    private void indexCommonFields(Journal journal, JournalIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            journal.getTitle());
        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            journal.getNameAbbreviation());

        StringUtil.removeTrailingPipeDelimiter(srContent, otherContent);
        index.setTitleSr(srContent.length() > 0 ? srContent.toString() : otherContent.toString());
        index.setTitleSrSortable(index.getTitleSr());
        index.setTitleOther(
            otherContent.length() > 0 ? otherContent.toString() : srContent.toString());
        index.setTitleOtherSortable(index.getTitleOther());
        index.setEISSN(journal.getEISSN());
        index.setPrintISSN(journal.getPrintISSN());
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                b.should(sb -> sb.wildcard(
                    m -> m.field("title_sr").value("*" + token + "*").caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("title_sr").query(token)));
                b.should(sb -> sb.wildcard(
                    m -> m.field("title_other").value("*" + token + "*").caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("e_issn").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("print_issn").query(token)));
            });
            return b;
        })))._toQuery();
    }
}
