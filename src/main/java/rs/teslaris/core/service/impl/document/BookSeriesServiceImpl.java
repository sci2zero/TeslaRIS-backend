package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.PublicationSeriesConverter;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
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
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.BookSeriesReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Transactional
@Traceable
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
                                 LanguageTagService languageTagService,
                                 PersonContributionService personContributionService,
                                 IndexBulkUpdateService indexBulkUpdateService,
                                 BookSeriesJPAServiceImpl bookSeriesJPAService,
                                 BookSeriesIndexRepository bookSeriesIndexRepository,
                                 SearchService<BookSeriesIndex> searchService,
                                 DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                 BookSeriesRepository bookSeriesRepository) {
        super(publicationSeriesRepository, multilingualContentService, languageTagService,
            personContributionService, indexBulkUpdateService);
        this.bookSeriesJPAService = bookSeriesJPAService;
        this.bookSeriesIndexRepository = bookSeriesIndexRepository;
        this.searchService = searchService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.bookSeriesRepository = bookSeriesRepository;
    }

    @Override
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
    public BookSeries findBookSeriesById(Integer bookSeriesId) {
        return bookSeriesJPAService.findOne(bookSeriesId);
    }

    @Override
    public BookSeries findRaw(Integer bookSeriesId) {
        return bookSeriesRepository.findRaw(bookSeriesId)
            .orElseThrow(() -> new NotFoundException("Book Series with given ID does not exist."));
    }

    @Override
    public BookSeries createBookSeries(BookSeriesDTO bookSeriesDTO, Boolean index) {
        var bookSeries = new BookSeries();

        clearPublicationSeriesCommonFields(bookSeries);
        setPublicationSeriesCommonFields(bookSeries, bookSeriesDTO);
        setBookSeriesFields(bookSeries, bookSeriesDTO);

        var newBookSeries = bookSeriesJPAService.save(bookSeries);
        if (index) {
            indexBookSeries(newBookSeries, new BookSeriesIndex());
        }

        return newBookSeries;
    }

    @Override
    public void updateBookSeries(Integer bookSeriesId, BookSeriesDTO bookSeriesDTO) {
        var bookSeriesToUpdate = bookSeriesJPAService.findOne(bookSeriesId);
        bookSeriesToUpdate.getLanguages().clear();

        clearPublicationSeriesCommonFields(bookSeriesToUpdate);
        setPublicationSeriesCommonFields(bookSeriesToUpdate, bookSeriesDTO);
        setBookSeriesFields(bookSeriesToUpdate, bookSeriesDTO);

        bookSeriesJPAService.save(bookSeriesToUpdate);
        var index =
            bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeriesToUpdate.getId())
                .orElse(new BookSeriesIndex());
        indexBookSeries(bookSeriesToUpdate, index);
    }

    @Override
    public void deleteBookSeries(Integer bookSeriesId) {
        if (publicationSeriesRepository.hasProceedings(bookSeriesId)) {
            throw new BookSeriesReferenceConstraintViolationException(
                "BookSeries with given ID is already in use.");
        }

        bookSeriesJPAService.delete(bookSeriesId);
        bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeriesId)
            .ifPresent(bookSeriesIndexRepository::delete);
    }

    @Override
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
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<BookSeries> chunk =
                bookSeriesJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((bookSeries) -> indexBookSeries(bookSeries, new BookSeriesIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
        return null;
    }

    @Override
    public void indexBookSeries(BookSeries bookSeries) {
        indexBookSeries(bookSeries, bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(
            bookSeries.getId()).orElse(new BookSeriesIndex()));
    }

    @Override
    public void save(BookSeries bookSeries) {
        bookSeriesJPAService.save(bookSeries);
    }

    private void setBookSeriesFields(BookSeries bookSeries, BookSeriesDTO bookSeriesDTO) {
        if (Objects.nonNull(bookSeriesDTO.getContributions())) {
            personContributionService.setPersonPublicationSeriesContributionsForBookSeries(
                bookSeries,
                bookSeriesDTO);
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
                } else if (token.contains("\\-") &&
                    issnPattern.matcher(token.replace("\\-", "-")).matches()) {
                    String normalizedToken = token.replace("\\-", "-");

                    b.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("e_issn").value(normalizedToken)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("print_issn").value(normalizedToken)))
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
                                .value(StringUtil.performSimpleLatinPreprocessing(token) + "*")
                                .caseInsensitive(true)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_other").value(wildcard).caseInsensitive(true)))
                        .should(sb -> sb.match(
                            mq -> mq.field("title_sr").query(token)))
                        .should(sb -> sb.match(
                            mq -> mq.field("title_other").query(token)))
                    ));
                }
            });
            return b.minimumShouldMatch(Integer.toString(minShouldMatch));
        })))._toQuery();
    }
}
