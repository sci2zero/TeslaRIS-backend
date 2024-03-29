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
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexrepository.BookSeriesIndexRepository;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.BookSeriesJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.BookSeriesReferenceConstraintViolationException;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Transactional
public class BookSeriesServiceImpl extends PublicationSeriesServiceImpl
    implements BookSeriesService {

    private final BookSeriesJPAServiceImpl bookSeriesJPAService;

    private final BookSeriesIndexRepository bookSeriesIndexRepository;

    private final SearchService<BookSeriesIndex> searchService;


    @Autowired
    public BookSeriesServiceImpl(PublicationSeriesRepository publicationSeriesRepository,
                                 MultilingualContentService multilingualContentService,
                                 LanguageTagService languageTagService,
                                 PersonContributionService personContributionService,
                                 EmailUtil emailUtil, BookSeriesJPAServiceImpl bookSeriesJPAService,
                                 BookSeriesIndexRepository bookSeriesIndexRepository,
                                 SearchService<BookSeriesIndex> searchService) {
        super(publicationSeriesRepository, multilingualContentService, languageTagService,
            personContributionService, emailUtil);
        this.bookSeriesJPAService = bookSeriesJPAService;
        this.bookSeriesIndexRepository = bookSeriesIndexRepository;
        this.searchService = searchService;
    }

    @Override
    public Page<BookSeriesResponseDTO> readAllBookSeries(Pageable pageable) {
        return bookSeriesJPAService.findAll(pageable).map(BookSeriesConverter::toDTO);
    }

    @Override
    public Page<BookSeriesIndex> searchBookSeries(List<String> tokens, Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens), pageable,
            BookSeriesIndex.class,
            "book_series");
    }

    @Override
    public BookSeriesResponseDTO readBookSeries(Integer bookSeriesId) {
        return BookSeriesConverter.toDTO(bookSeriesJPAService.findOne(bookSeriesId));
    }

    @Override
    public BookSeries createBookSeries(BookSeriesDTO bookSeriesDTO) {
        var bookSeries = new BookSeries();
        bookSeries.setLanguages(new HashSet<>());

        setPublicationSeriesCommonFields(bookSeries, bookSeriesDTO);
        setBookSeriesFields(bookSeries, bookSeriesDTO);

        var newBookSeries = bookSeriesJPAService.save(bookSeries);
        indexBookSeries(newBookSeries);

        return newBookSeries;
    }

    @Override
    public void updateBookSeries(BookSeriesDTO bookSeriesDTO, Integer bookSeriesId) {
        var bookSeriesToUpdate = bookSeriesJPAService.findOne(bookSeriesId);
        bookSeriesToUpdate.getLanguages().clear();

        setPublicationSeriesCommonFields(bookSeriesToUpdate, bookSeriesDTO);
        setBookSeriesFields(bookSeriesToUpdate, bookSeriesDTO);

        bookSeriesJPAService.save(bookSeriesToUpdate);
        indexBookSeries(bookSeriesToUpdate);
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

    private void setBookSeriesFields(BookSeries bookSeries, BookSeriesDTO bookSeriesDTO) {
        if (Objects.nonNull(bookSeriesDTO.getContributions())) {
            personContributionService.setPersonPublicationSeriesContributionsForBookSeries(
                bookSeries,
                bookSeriesDTO);
        }
    }

    private void indexBookSeries(BookSeries bookSeries) {
        var index = bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeries.getId())
            .orElse(new BookSeriesIndex());
        index.setDatabaseId(bookSeries.getId());
        indexCommonFields(bookSeries, index);
        bookSeriesIndexRepository.save(index);
    }

    private void indexCommonFields(BookSeries bookSeries, BookSeriesIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            bookSeries.getTitle());
        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            bookSeries.getNameAbbreviation());

        StringUtil.removeTrailingPipeDelimiter(srContent, otherContent);
        index.setTitleSr(srContent.length() > 0 ? srContent.toString() : otherContent.toString());
        index.setTitleSrSortable(index.getTitleSr());
        index.setTitleOther(
            otherContent.length() > 0 ? otherContent.toString() : srContent.toString());
        index.setTitleOtherSortable(index.getTitleOther());
        index.setEISSN(bookSeries.getEISSN());
        index.setPrintISSN(bookSeries.getPrintISSN());
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
