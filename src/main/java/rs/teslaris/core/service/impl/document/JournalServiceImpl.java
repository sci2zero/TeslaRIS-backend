package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.document.PublicationSeriesConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.JournalJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
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

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    @Autowired
    public JournalServiceImpl(PublicationSeriesRepository publicationSeriesRepository,
                              MultilingualContentService multilingualContentService,
                              LanguageTagService languageTagService,
                              PersonContributionService personContributionService,
                              EmailUtil emailUtil,
                              IndexBulkUpdateService indexBulkUpdateService,
                              JournalJPAServiceImpl journalJPAService,
                              SearchService<JournalIndex> searchService,
                              JournalIndexRepository journalIndexRepository,
                              JournalRepository journalRepository,
                              DocumentPublicationIndexRepository documentPublicationIndexRepository) {
        super(publicationSeriesRepository, multilingualContentService, languageTagService,
            personContributionService, emailUtil, indexBulkUpdateService);
        this.journalJPAService = journalJPAService;
        this.searchService = searchService;
        this.journalIndexRepository = journalIndexRepository;
        this.journalRepository = journalRepository;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
    }

    @Override
    public Page<JournalResponseDTO> readAllJournals(Pageable pageable) {
        return journalJPAService.findAll(pageable).map(PublicationSeriesConverter::toDTO);
    }

    @Override
    public Page<JournalIndex> searchJournals(List<String> tokens, Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens), pageable, JournalIndex.class,
            "journal");
    }

    @Override
    public JournalResponseDTO readJournal(Integer journalId) {
        return PublicationSeriesConverter.toDTO(journalJPAService.findOne(journalId));
    }

    @Override
    public JournalIndex readJournalByIssn(String eIssn, String printIssn) {
        return journalIndexRepository.findJournalIndexByeISSNOrPrintISSN(eIssn, printIssn)
            .orElse(null);
    }

    @Override
    public Journal findJournalById(Integer journalId) {
        return journalJPAService.findOne(journalId);
    }

    @Override
    public Optional<Journal> tryToFindById(Integer journalId) {
        return journalRepository.findById(journalId);
    }

    @Override
    public Journal findJournalByOldId(Integer journalId) {
        return journalRepository.findJournalByOldId(journalId).orElse(null);
    }

    @Override
    public Journal createJournal(PublicationSeriesDTO journalDTO, Boolean index) {
        var journal = new Journal();

        clearPublicationSeriesCommonFields(journal);
        setPublicationSeriesCommonFields(journal, journalDTO);
        setJournalRelatedFields(journal, journalDTO);

        var savedJournal = journalJPAService.save(journal);

        if (index) {
            indexJournal(journal, new JournalIndex());
        }

        return savedJournal;
    }

    @Override
    public Journal createJournal(JournalBasicAdditionDTO journalDTO) {
        var journal = new Journal();

        journal.setTitle(multilingualContentService.getMultilingualContent(journalDTO.getTitle()));
        journal.setEISSN(journalDTO.getEISSN());
        journal.setPrintISSN(journalDTO.getPrintISSN());

        var savedJournal = journalJPAService.save(journal);
        indexJournal(journal,
            journalIndexRepository.findJournalIndexByDatabaseId(journal.getId())
                .orElse(new JournalIndex()));

        emailUtil.notifyInstitutionalEditor(savedJournal.getId(), "journal");

        return savedJournal;
    }

    @Override
    public void updateJournal(Integer journalId, PublicationSeriesDTO journalDTO) {
        var journalToUpdate = journalJPAService.findOne(journalId);
        journalToUpdate.getLanguages().clear();

        clearPublicationSeriesCommonFields(journalToUpdate);
        setPublicationSeriesCommonFields(journalToUpdate, journalDTO);
        setJournalRelatedFields(journalToUpdate, journalDTO);

        var indexToUpdate = journalIndexRepository.findJournalIndexByDatabaseId(journalId)
            .orElse(new JournalIndex());
        indexJournal(journalToUpdate, indexToUpdate);

        journalJPAService.save(journalToUpdate);
    }

    @Override
    public void deleteJournal(Integer journalId) {
        if (journalRepository.hasPublication(journalId) ||
            publicationSeriesRepository.hasProceedings(journalId)) {
            throw new JournalReferenceConstraintViolationException(
                "PublicationSeries with given ID is already in use.");
        }

        journalJPAService.delete(journalId);
        var index = journalIndexRepository.findJournalIndexByDatabaseId(journalId);
        index.ifPresent(journalIndexRepository::delete);
    }

    @Override
    public void forceDeleteJournal(Integer journalId) {
        journalRepository.deleteAllPublicationsInJournal(journalId);
        publicationSeriesRepository.unbindProceedings(journalId);

        journalJPAService.delete(journalId);

        var index = journalIndexRepository.findJournalIndexByDatabaseId(journalId);
        index.ifPresent(journalIndexRepository::delete);

        documentPublicationIndexRepository.deleteByJournalIdAndType(journalId,
            DocumentPublicationType.JOURNAL_PUBLICATION.name());

        indexBulkUpdateService.removeIdFromRecord("document_publication", "publication_series_id",
            journalId);
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexJournals() {
        journalIndexRepository.deleteAll();
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Journal> chunk =
                journalJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((journal) -> indexJournal(journal, new JournalIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void setJournalRelatedFields(Journal journal, PublicationSeriesDTO journalDTO) {
        if (Objects.nonNull(journalDTO.getContributions())) {
            personContributionService.setPersonPublicationSeriesContributionsForJournal(journal,
                journalDTO);
        }
    }

    @Override
    public void indexJournal(Journal journal, JournalIndex index) {
        index.setDatabaseId(journal.getId());

        indexCommonFields(journal, index);
        journalIndexRepository.save(index);
    }

    @Override
    public PublicationSeries findOrCreatePublicationSeries(String[] line,
                                                           String defaultLanguageTag,
                                                           String journalName,
                                                           String eIssn, String printIssn,
                                                           boolean issnSpecified) {
        var publicationSeries =
            issnSpecified ? findPublicationSeriesByIssn(eIssn, printIssn) : null;

        if (Objects.isNull(publicationSeries)) {
            var defaultLanguage =
                languageTagService.findLanguageTagByValue(defaultLanguageTag);
            publicationSeries =
                findJournalByJournalName(journalName, defaultLanguage, eIssn, printIssn);
        }

        if (issnSpecified &&
            publicationSeries.getPrintISSN().equals(publicationSeries.getEISSN()) &&
            !eIssn.equals(printIssn)) {
            publicationSeries.setEISSN(eIssn);
            publicationSeries.setPrintISSN(printIssn);
            save(publicationSeries);
        }

        return publicationSeries;
    }

    @Override
    public Journal findJournalByJournalName(String journalName,
                                            LanguageTag defaultLanguage,
                                            String eIssn, String printIssn) {
        var potentialHits = searchJournals(
            Arrays.stream(journalName.split(" ")).toList(), PageRequest.of(0, 2)).getContent();

        for (var potentialHit : potentialHits) {
            for (var title : potentialHit.getTitleOther().split("\\|")) {
                if (title.equalsIgnoreCase(journalName)) { // is equalsIgnoreCase ok here?
                    var publicationSeries = findJournalById(potentialHit.getDatabaseId());

                    // TODO: is this ok?
                    if (Objects.isNull(publicationSeries.getEISSN()) ||
                        publicationSeries.getEISSN().isEmpty() ||
                        publicationSeries.getEISSN().equals(publicationSeries.getPrintISSN())) {
                        publicationSeries.setEISSN(eIssn);
                    }

                    if (Objects.isNull(publicationSeries.getPrintISSN()) ||
                        publicationSeries.getPrintISSN().isEmpty() ||
                        publicationSeries.getPrintISSN().equals(publicationSeries.getEISSN())) {
                        publicationSeries.setPrintISSN(printIssn);
                    }

                    indexJournal(publicationSeries, potentialHit);
                    return journalRepository.save(publicationSeries);
                }
            }
        }

        return createNewJournal(journalName, defaultLanguage, eIssn, printIssn);
    }

    private Journal createNewJournal(String journalName, LanguageTag defaultLanguage,
                                     String eIssn, String printIssn) {
        var newJournal = new JournalDTO();
        newJournal.setTitle(List.of(new MultilingualContentDTO(defaultLanguage.getId(),
            defaultLanguage.getLanguageTag(), StringEscapeUtils.unescapeHtml4(journalName), 1)));
        newJournal.setNameAbbreviation(new ArrayList<>());
        newJournal.setContributions(new ArrayList<>());
        newJournal.setEissn(eIssn);
        newJournal.setPrintISSN(printIssn);
        newJournal.setLanguageTagIds(List.of(defaultLanguage.getId()));

        return createJournal(newJournal, true);
    }

    private void indexCommonFields(Journal journal, JournalIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            journal.getTitle(), true);
        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            journal.getNameAbbreviation(), false);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        index.setTitleSr(!srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setTitleSrSortable(index.getTitleSr());
        index.setTitleOther(
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setTitleOtherSortable(index.getTitleOther());
        index.setEISSN(journal.getEISSN());
        index.setPrintISSN(journal.getPrintISSN());
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        var minShouldMatch = (int) Math.ceil(tokens.size() * 0.8);

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                    b.must(mp ->
                        mp.bool(m -> {
                            {
                                m.should(sb -> sb.matchPhrase(
                                    mq -> mq.field("title_sr")
                                        .query(token.replace("\\\"", ""))));
                                m.should(sb -> sb.matchPhrase(
                                    mq -> mq.field("title_other")
                                        .query(token.replace("\\\"", ""))));
                            }
                            return m;
                        }));
                }

                b.should(sb -> sb.wildcard(
                    m -> m.field("title_sr").value("*" + token + "*").caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("title_sr").query(token)));
                b.should(sb -> sb.wildcard(
                    m -> m.field("title_other").value("*" + token + "*").caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("e_issn").query(token.replace("\\-", "-"))));
                b.should(sb -> sb.match(
                    m -> m.field("print_issn").query(token.replace("\\-", "-"))));
            });
            return b.minimumShouldMatch(Integer.toString(minShouldMatch));
        })))._toQuery();
    }
}
