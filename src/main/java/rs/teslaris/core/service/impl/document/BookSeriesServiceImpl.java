package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.PersonContributionsChangeEvent;
import rs.teslaris.core.converter.document.PublicationSeriesConverter;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.BookSeriesIndexRepository;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.BookSeriesJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.BookSeriesReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Traceable
@Slf4j
public class BookSeriesServiceImpl extends PublicationSeriesServiceImpl
    implements BookSeriesService {

    private final BookSeriesJPAServiceImpl bookSeriesJPAService;

    private final BookSeriesIndexRepository bookSeriesIndexRepository;

    private final SearchService<BookSeriesIndex> searchService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final BookSeriesRepository bookSeriesRepository;


    @Autowired
    public BookSeriesServiceImpl(PublicationSeriesRepository publicationSeriesRepository,
                                 MultilingualContentService multilingualContentService,
                                 LanguageService languageService,
                                 PersonContributionService personContributionService,
                                 IndexBulkUpdateService indexBulkUpdateService,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 BookSeriesJPAServiceImpl bookSeriesJPAService,
                                 BookSeriesIndexRepository bookSeriesIndexRepository,
                                 SearchService<BookSeriesIndex> searchService,
                                 DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                 BookSeriesRepository bookSeriesRepository) {
        super(publicationSeriesRepository, multilingualContentService, languageService,
            personContributionService, indexBulkUpdateService, applicationEventPublisher);
        this.bookSeriesJPAService = bookSeriesJPAService;
        this.bookSeriesIndexRepository = bookSeriesIndexRepository;
        this.searchService = searchService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.bookSeriesRepository = bookSeriesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookSeriesResponseDTO> readAllBookSeries(Pageable pageable) {
        return bookSeriesJPAService.findAll(pageable).map(PublicationSeriesConverter::toDTO);
    }

    @Override
    public Page<BookSeriesIndex> searchBookSeries(List<String> tokens, Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens), pageable,
            BookSeriesIndex.class,
            "book_series");
    }

    @Override
    public Page<DocumentPublicationIndex> findPublicationsForBookSeries(Integer bookSeriesId,
                                                                        Pageable pageable) {
        return documentPublicationIndexRepository.findByTypeInAndPublicationSeriesId(
            List.of(DocumentPublicationType.PROCEEDINGS.name(),
                DocumentPublicationType.MONOGRAPH.name()), bookSeriesId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public BookSeriesResponseDTO readBookSeries(Integer bookSeriesId) {
        BookSeries bookSeries;
        try {
            bookSeries = bookSeriesJPAService.findOne(bookSeriesId);
        } catch (NotFoundException e) {
            bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeriesId)
                .ifPresent(bookSeriesIndexRepository::delete);
            throw e;
        }

        return PublicationSeriesConverter.toDTO(bookSeries);
    }

    @Override
    @Transactional
    public BookSeries findBookSeriesById(Integer bookSeriesId) {
        return bookSeriesJPAService.findOne(bookSeriesId);
    }

    @Override
    @Transactional
    public BookSeries findRaw(Integer bookSeriesId) {
        return bookSeriesRepository.findRaw(bookSeriesId)
            .orElseThrow(() -> new NotFoundException("Book Series with given ID does not exist."));
    }

    @Override
    @Transactional
    public BookSeries createBookSeries(BookSeriesDTO bookSeriesDTO, Boolean index) {
        var bookSeries = new BookSeries();

        var oldContributorIds = clearPublicationSeriesCommonFields(bookSeries);
        setPublicationSeriesCommonFields(bookSeries, bookSeriesDTO);
        setBookSeriesFields(bookSeries, bookSeriesDTO, oldContributorIds);

        var newBookSeries = bookSeriesJPAService.save(bookSeries);
        if (index) {
            indexBookSeries(newBookSeries, new BookSeriesIndex());
        }

        return newBookSeries;
    }

    @Override
    @Transactional
    public void updateBookSeries(Integer bookSeriesId, BookSeriesDTO bookSeriesDTO) {
        var bookSeriesToUpdate = bookSeriesJPAService.findOne(bookSeriesId);
        bookSeriesToUpdate.getLanguages().clear();

        var oldContributorIds = clearPublicationSeriesCommonFields(bookSeriesToUpdate);
        setPublicationSeriesCommonFields(bookSeriesToUpdate, bookSeriesDTO);
        setBookSeriesFields(bookSeriesToUpdate, bookSeriesDTO, oldContributorIds);

        bookSeriesJPAService.save(bookSeriesToUpdate);
        var index =
            bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeriesToUpdate.getId())
                .orElse(new BookSeriesIndex());
        indexBookSeries(bookSeriesToUpdate, index);
    }

    @Override
    @Transactional
    public void deleteBookSeries(Integer bookSeriesId) {
        if (publicationSeriesRepository.hasProceedings(bookSeriesId)) {
            throw new BookSeriesReferenceConstraintViolationException(
                "BookSeries with given ID is already in use.");
        }

        var bookSeries = bookSeriesJPAService.findOne(bookSeriesId);
        publicationSeriesRepository.deletePublicationSeriesContributions(bookSeriesId);
        updateIndexedPersonContributions(bookSeries);

        bookSeriesJPAService.delete(bookSeriesId);
        bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeriesId)
            .ifPresent(bookSeriesIndexRepository::delete);
    }

    @Override
    @Transactional
    public void forceDeleteBookSeries(Integer bookSeriesId) {
        publicationSeriesRepository.unbindProceedings(bookSeriesId);

        bookSeriesJPAService.delete(bookSeriesId);

        var index = bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeriesId);
        index.ifPresent(bookSeriesIndexRepository::delete);

        indexBulkUpdateService.removeIdFromRecord("document_publication", "publication_series_id",
            bookSeriesId);
    }

    @Override
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexBookSeries() {
        bookSeriesIndexRepository.deleteAll();

        FunctionalUtil.performBulkOperation(
            bookSeriesJPAService::findAll,
            Sort.by(Sort.Direction.ASC, "id"),
            (bookSeries) -> indexBookSeries(bookSeries, new BookSeriesIndex())
        );

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public void indexBookSeries(BookSeries bookSeries) {
        indexBookSeries(bookSeries, bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(
            bookSeries.getId()).orElse(new BookSeriesIndex()));
    }

    @Override
    @Transactional
    public void save(BookSeries bookSeries) {
        bookSeriesJPAService.save(bookSeries);
    }

    @Override
    @Transactional
    @Nullable
    public BookSeriesIndex readBookSeriesByIssn(String eIssn, String printIssn) {
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

        return bookSeriesIndexRepository.findBookSeriesIndexByeISSNOrPrintISSN(eIssn, printIssn)
            .orElse(null);
    }

    private void setBookSeriesFields(BookSeries bookSeries, BookSeriesDTO bookSeriesDTO,
                                     HashSet<Integer> oldContributorIds) {
        if (Objects.nonNull(bookSeriesDTO.getContributions())) {
            personContributionService.setPersonPublicationSeriesContributionsForBookSeries(
                bookSeries,
                bookSeriesDTO);

            oldContributorIds.addAll(bookSeriesDTO.getContributions().stream()
                .map(PersonContributionDTO::getPersonId)
                .filter(Objects::nonNull).toList());

            applicationEventPublisher.publishEvent(
                new PersonContributionsChangeEvent(oldContributorIds));
        }
    }

    private void indexBookSeries(BookSeries bookSeries, BookSeriesIndex index) {
        index.setDatabaseId(bookSeries.getId());

        indexCommonFields(bookSeries, index);
        bookSeriesIndexRepository.save(index);
    }

    private void indexCommonFields(BookSeries bookSeries, BookSeriesIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            bookSeries.getTitle(), true);

        if (srContent.isEmpty() && !otherContent.isEmpty()) {
            srContent.append(otherContent);
        } else if (!srContent.isEmpty() && otherContent.isEmpty()) {
            otherContent.append(srContent);
        }

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            bookSeries.getNameAbbreviation(), false);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        index.setTitleSr(!srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setTitleSrSortable(index.getTitleSr());
        index.setTitleOther(
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setTitleOtherSortable(index.getTitleOther());
        index.setEISSN(bookSeries.getEISSN());
        index.setPrintISSN(bookSeries.getPrintISSN());
        index.setOpenAlexId(bookSeries.getOpenAlexId());
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        var minShouldMatch = computeMinimumShouldMatch(tokens.size());

        return BoolQuery.of(q -> {
            var textValueTokens = new ArrayList<String>();

            tokens.forEach(token -> {
                if (token.contains("\\-") &&
                    issnPattern.matcher(token.replace("\\-", "-")).matches()) {

                    String normalizedToken = token.replace("\\-", "-");

                    q.should(s -> s.wildcard(w ->
                        w.field("e_issn").value(normalizedToken)
                    ));
                    q.should(s -> s.wildcard(w ->
                        w.field("print_issn").value(normalizedToken)
                    ));

                } else if (token.contains("\\-") &&
                    partialIssnPattern.matcher(token.replace("\\-", "-")).matches()) {

                    String normalizedToken = token.replace("\\-", "-");

                    q.should(s -> s.prefix(p ->
                        p.field("e_issn").value(normalizedToken)
                    ));
                    q.should(s -> s.prefix(p ->
                        p.field("print_issn").value(normalizedToken)
                    ));
                } else {
                    textValueTokens.add(token);
                }
            });

            if (!textValueTokens.isEmpty()) {
                q.must(mainBool -> mainBool.bool(main -> {
                    main.should(srBool -> srBool.bool(sr -> {
                        textValueTokens.forEach(token -> {
                            if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                                sr.must(m -> m.matchPhrase(mp ->
                                    mp.field("title_sr")
                                        .query(token.replace("\\\"", ""))
                                ));
                            } else if (token.endsWith("\\*") || token.endsWith(".")) {
                                var wildcard = token.replace("\\*", "").replace(".", "");
                                sr.should(s -> s.wildcard(w ->
                                    w.field("title_sr")
                                        .value(
                                            StringUtil.performSimpleLatinPreprocessing(wildcard) +
                                                "*")
                                        .caseInsensitive(true)
                                ));
                            } else {
                                var normalized = StringUtil.performSimpleLatinPreprocessing(token);
                                sr.should(s -> s.wildcard(w ->
                                    w.field("title_sr")
                                        .value(normalized + "*")
                                        .caseInsensitive(true)
                                ));
                                sr.should(s -> s.match(m ->
                                    m.field("title_sr")
                                        .query(token)
                                ));
                            }
                        });
                        return sr.minimumShouldMatch(minShouldMatch);
                    }));

                    main.should(otherBool -> otherBool.bool(other -> {
                        textValueTokens.forEach(token -> {
                            if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                                other.must(m -> m.matchPhrase(mp ->
                                    mp.field("title_other")
                                        .query(token.replace("\\\"", ""))
                                ));
                            } else if (token.endsWith("\\*") || token.endsWith(".")) {
                                var wildcard = token.replace("\\*", "").replace(".", "");
                                other.should(s -> s.wildcard(w ->
                                    w.field("title_other")
                                        .value(wildcard + "*")
                                        .caseInsensitive(true)
                                ));
                            } else {
                                other.should(s -> s.wildcard(w ->
                                    w.field("title_other")
                                        .value(token + "*")
                                        .caseInsensitive(true)
                                ));
                                other.should(s -> s.match(m ->
                                    m.field("title_other")
                                        .query(token)
                                ));
                            }
                        });
                        return other.minimumShouldMatch(minShouldMatch);
                    }));

                    return main.minimumShouldMatch("1");
                }));
            }

            return q;
        })._toQuery();
    }

    private String computeMinimumShouldMatch(int tokensCount) {
        if (tokensCount <= 1) {
            return "1";
        }

        if (tokensCount <= 6) {
            return String.valueOf((int) Math.floor(tokensCount * 0.75));
        }

        return String.valueOf((int) Math.floor(tokensCount * 0.6));
    }
}
