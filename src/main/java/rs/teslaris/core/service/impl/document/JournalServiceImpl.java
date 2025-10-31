package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
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
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.JournalJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.JournalReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Traceable
public class JournalServiceImpl extends PublicationSeriesServiceImpl implements JournalService {

    private final JournalJPAServiceImpl journalJPAService;

    private final SearchService<JournalIndex> searchService;

    private final JournalIndexRepository journalIndexRepository;

    private final JournalRepository journalRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final CommissionRepository commissionRepository;


    @Autowired
    public JournalServiceImpl(PublicationSeriesRepository publicationSeriesRepository,
                              MultilingualContentService multilingualContentService,
                              LanguageTagService languageTagService,
                              PersonContributionService personContributionService,
                              IndexBulkUpdateService indexBulkUpdateService,
                              JournalJPAServiceImpl journalJPAService,
                              SearchService<JournalIndex> searchService,
                              JournalIndexRepository journalIndexRepository,
                              JournalRepository journalRepository,
                              DocumentPublicationIndexRepository documentPublicationIndexRepository,
                              CommissionRepository commissionRepository) {
        super(publicationSeriesRepository, multilingualContentService, languageTagService,
            personContributionService, indexBulkUpdateService);
        this.journalJPAService = journalJPAService;
        this.searchService = searchService;
        this.journalIndexRepository = journalIndexRepository;
        this.journalRepository = journalRepository;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.commissionRepository = commissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalResponseDTO> readAllJournals(Pageable pageable) {
        return journalJPAService.findAll(pageable).map(PublicationSeriesConverter::toDTO);
    }

    @Override
    public Page<JournalIndex> searchJournals(List<String> tokens, Pageable pageable,
                                             Integer institutionId, Integer commissionId) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens, institutionId, commissionId),
            pageable, JournalIndex.class, "journal");
    }

    @Override
    @Transactional(readOnly = true)
    public JournalResponseDTO readJournal(Integer journalId) {
        Journal journal;
        try {
            journal = journalJPAService.findOne(journalId);
        } catch (NotFoundException e) {
            journalIndexRepository.findJournalIndexByDatabaseId(journalId)
                .ifPresent(journalIndexRepository::delete);
            throw e;
        }

        return PublicationSeriesConverter.toDTO(journal);
    }

    @Override
    @Nullable
    public JournalIndex readJournalByIssn(String eIssn, String printIssn) {
        boolean isEissnBlank = (Objects.isNull(eIssn) || eIssn.isBlank());
        boolean isPrintIssnBlank = (Objects.isNull(printIssn) || printIssn.isBlank());

        if (isEissnBlank && isPrintIssnBlank) {
            return null;
        }

        if (isEissnBlank) {
            eIssn = printIssn;
        } else if (isPrintIssnBlank) {
            printIssn = eIssn;
        }

        return journalIndexRepository.findJournalIndexByeISSNOrPrintISSN(eIssn, printIssn)
            .orElse(null);
    }

    @Override
    public JournalIndex readJournalByIdentifiers(String eIssn, String printIssn,
                                                 String openAlexId) {
        return journalIndexRepository.findByAnyIdentifiers(eIssn, printIssn, openAlexId)
            .orElse(null);
    }

    @Override
    @Transactional
    public Journal findJournalById(Integer journalId) {
        return journalJPAService.findOne(journalId);
    }

    @Override
    @Transactional
    public Journal findRaw(Integer journalId) {
        return journalRepository.findRaw(journalId)
            .orElseThrow(() -> new NotFoundException("Journal with given ID does not exist."));
    }

    @Override
    @Transactional
    public Optional<Journal> tryToFindById(Integer journalId) {
        return journalRepository.findById(journalId);
    }

    @Override
    @Transactional
    public Journal findJournalByOldId(Integer journalId) {
        return journalRepository.findByOldIdsContains(journalId).orElse(null);
    }

    @Override
    @Transactional
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
    @Transactional
    public Journal createJournal(JournalBasicAdditionDTO journalDTO) {
        var journal = new Journal();

        journal.setTitle(multilingualContentService.getMultilingualContent(journalDTO.getTitle()));
        journal.setEISSN(journalDTO.getEISSN());
        journal.setPrintISSN(journalDTO.getPrintISSN());

        var savedJournal = journalJPAService.save(journal);
        indexJournal(journal,
            journalIndexRepository.findJournalIndexByDatabaseId(journal.getId())
                .orElse(new JournalIndex()));

        return savedJournal;
    }

    @Override
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexJournals() {
        journalIndexRepository.deleteAll();

        performBulkReindex();

        return null;
    }

    public void performBulkReindex() {
        int pageNumber = 0;
        int chunkSize = 100;
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
    @Transactional
    public void indexJournal(Journal journal) {
        journalIndexRepository.findJournalIndexByDatabaseId(journal.getId()).ifPresent(index -> {
            indexJournal(journal, index);
        });
    }

    @Override
    @Transactional
    public void indexJournal(Journal journal, JournalIndex index) {
        index.setDatabaseId(journal.getId());

        indexCommonFields(journal, index);
        reindexJournalVolatileInformation(journal.getId());
        journalIndexRepository.save(index);
    }

    @Override
    @Transactional
    public void reindexJournalVolatileInformation(Integer journalId) {
        journalIndexRepository.findJournalIndexByDatabaseId(journalId).ifPresent(journalIndex -> {
            journalIndex.setRelatedInstitutionIds(
                journalRepository.findInstitutionIdsByJournalIdAndAuthorContribution(journalId)
                    .stream().toList());
            journalIndex.setClassifiedBy(
                commissionRepository.findCommissionsThatClassifiedJournal(journalId));

            journalIndexRepository.save(journalIndex);
        });
    }

    @Override
    @Transactional
    public void addOldId(Integer id, Integer oldId) {
        var journal = findOne(id);
        journal.getOldIds().add(oldId);
        save(journal);
    }

    @Override
    @Transactional
    public void save(Journal journal) {
        journalRepository.save(journal);
    }

    @Override
    @Transactional
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
    @Transactional
    public Journal findJournalByJournalName(String journalName,
                                            LanguageTag defaultLanguage,
                                            String eIssn, String printIssn) {
        var potentialHits = searchJournals(
            Arrays.stream(journalName.split(" ")).toList(), PageRequest.of(0, 2),
            null, null).getContent();

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
        index.setOpenAlexId(journal.getOpenAlexId());

        index.setRelatedInstitutionIds(
            journalRepository.findInstitutionIdsByJournalIdAndAuthorContribution(journal.getId())
                .stream().toList());
        index.setClassifiedBy(
            commissionRepository.findCommissionsThatClassifiedJournal(journal.getId()));
    }

    private Query buildSimpleSearchQuery(List<String> tokens, Integer institutionId,
                                         Integer commissionId) {
        var minShouldMatch = "2<-100% 5<-80% 10<-70%";

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            if (Objects.nonNull(institutionId) && institutionId > 0) {
                b.must(sb -> sb.term(
                    m -> m.field("related_institution_ids").value(institutionId)));
            }

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
                } else if (token.contains("\\-") &&
                    issnPattern.matcher(token.replace("\\-", "-")).matches()) {
                    String normalizedToken = token.replace("\\-", "-");

                    b.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("e_issn").value(normalizedToken)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("print_issn").value(normalizedToken)))
                    ));
                } else if (token.contains("\\-") &&
                    partialIssnPattern.matcher(token.replace("\\-", "-")).matches()) {
                    String normalizedToken = token.replace("\\-", "-");

                    b.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.prefix(
                            p -> p.field("e_issn").value(normalizedToken)))
                        .should(sb -> sb.prefix(
                            p -> p.field("print_issn").value(normalizedToken)))
                    ));
                } else if (token.endsWith("\\*") || token.endsWith(".")) {
                    var wildcard = token.replace("\\*", "").replace(".", "");
                    b.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_sr")
                                .value(StringUtil.performSimpleLatinPreprocessing(wildcard) + "*")
                                .caseInsensitive(true)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_other").value(wildcard + "*")
                                .caseInsensitive(true)))
                    ));
                } else {
                    var wildcard = token + "*";
                    b.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_sr")
                                .value(StringUtil.performSimpleLatinPreprocessing(wildcard) + "*")
                                .caseInsensitive(true)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_other").value(wildcard).caseInsensitive(true)))
                        .should(sb -> sb.match(
                            mq -> mq.field("title_sr").query(token)))
                        .should(sb -> sb.match(
                            mq -> mq.field("title_other").query(token)))
                        .should(sb -> sb.prefix(
                            p -> p.field("e_issn").value(token)))
                        .should(sb -> sb.prefix(
                            p -> p.field("print_issn").value(token)))
                    ));
                }
            });

            if (Objects.nonNull(commissionId)) {
                b.mustNot(mnb -> {
                    mnb.term(m -> m.field("classified_by").value(commissionId));
                    return mnb;
                });
            }

            return b.minimumShouldMatch(minShouldMatch);
        })))._toQuery();
    }
}
