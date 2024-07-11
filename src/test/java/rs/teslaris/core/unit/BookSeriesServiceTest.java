package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexrepository.BookSeriesIndexRepository;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.document.BookSeriesServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.BookSeriesJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.BookSeriesReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class BookSeriesServiceTest {

    @Mock
    private BookSeriesRepository bookSeriesRepository;

    @Mock
    private BookSeriesJPAServiceImpl bookSeriesJPAService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private SearchService<BookSeriesIndex> searchService;

    @Mock
    private BookSeriesIndexRepository bookSeriesIndexRepository;

    @Mock
    private PublicationSeriesRepository publicationSeriesRepository;

    @InjectMocks
    private BookSeriesServiceImpl bookSeriesService;


    @Test
    public void shouldReturnBookSeriesWhenItExists() {
        // given
        var expected = new BookSeries();
        when(publicationSeriesRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = bookSeriesService.findPublicationSeriesById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenBookSeriesDoesNotExist() {
        // given
        when(bookSeriesRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> bookSeriesService.findOne(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldCreateBookSeriesWhenProvidedWithValidData() {
        // given
        var bookSeriesDTO = new BookSeriesDTO();
        bookSeriesDTO.setTitle(new ArrayList<>());
        bookSeriesDTO.setNameAbbreviation(new ArrayList<>());
        bookSeriesDTO.setEissn("1234-5678");
        bookSeriesDTO.setPrintISSN("1234-5678");
        bookSeriesDTO.setContributions(new ArrayList<>());
        bookSeriesDTO.setLanguageTagIds(new ArrayList<>());

        var bookSeriesIndex = new BookSeriesIndex();
        bookSeriesIndex.setDatabaseId(1);

        when(bookSeriesJPAService.save(any())).thenReturn(new BookSeries());
        when(bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(
            bookSeriesIndex.getDatabaseId())).thenReturn(
            Optional.of(bookSeriesIndex));

        // when
        var savedBookSeries = bookSeriesService.createBookSeries(bookSeriesDTO, true);

        // then
        assertNotNull(savedBookSeries);
        verify(bookSeriesJPAService, times(1)).save(any());
    }

    @Test
    public void shouldUpdateBookSeriesWhenProvidedWithValidData() {
        // given
        var bookSeriesId = 1;
        var bookSeriesDTO = new BookSeriesDTO();
        bookSeriesDTO.setTitle(new ArrayList<>());
        bookSeriesDTO.setNameAbbreviation(new ArrayList<>());
        bookSeriesDTO.setEissn("1234-5676");
        bookSeriesDTO.setPrintISSN("1234-5676");
        bookSeriesDTO.setContributions(new ArrayList<>());
        bookSeriesDTO.setLanguageTagIds(new ArrayList<>());

        var bookSeries = new BookSeries();

        when(bookSeriesJPAService.findOne(bookSeriesId)).thenReturn(bookSeries);
        when(bookSeriesJPAService.save(any())).thenReturn(new BookSeries());
        when(bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeriesId)).thenReturn(
            Optional.empty());

        // when
        bookSeriesService.updateBookSeries(bookSeriesDTO, bookSeriesId);

        // then
        verify(bookSeriesJPAService, times(1)).save(any());
    }

    @Test
    public void shouldDeleteBookSeriesWhenNotUsed() {
        // given
        var bookSeriesId = 1;
        var bookSeriesToDelete = new BookSeries();

        when(bookSeriesJPAService.findOne(bookSeriesId)).thenReturn(bookSeriesToDelete);
        when(publicationSeriesRepository.hasProceedings(bookSeriesId)).thenReturn(false);
        when(bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeriesId)).thenReturn(
            Optional.of(new BookSeriesIndex()));

        // when
        bookSeriesService.deleteBookSeries(bookSeriesId);

        // then
        verify(bookSeriesJPAService, times(1)).delete(any());
        verify(bookSeriesIndexRepository, times(1)).delete(any());
    }

    @Test
    public void shouldNotDeleteBookSeriesIfInUsage() {
        // given
        var bookSeriesId = 1;
        var bookSeriesToDelete = new BookSeries();

        when(bookSeriesJPAService.findOne(bookSeriesId)).thenReturn(bookSeriesToDelete);
        when(publicationSeriesRepository.hasProceedings(bookSeriesId)).thenReturn(true);

        // when
        assertThrows(BookSeriesReferenceConstraintViolationException.class,
            () -> bookSeriesService.deleteBookSeries(bookSeriesId));

        // then (BookSeriesReferenceConstraintViolationException should be thrown)
    }

    @Test
    public void shouldReadAllBookSeriess() {
        // given
        var pageable = Pageable.ofSize(5);
        var bookSeries1 = new BookSeries();
        bookSeries1.setEISSN("eISSN1");
        bookSeries1.setPrintISSN("printISSN1");
        var bookSeries2 = new BookSeries();
        bookSeries2.setEISSN("eISSN2");
        bookSeries2.setPrintISSN("printISSN2");

        when(bookSeriesJPAService.findAll(pageable)).thenReturn(
            new PageImpl<>(List.of(bookSeries1, bookSeries2)));

        // when
        var response = bookSeriesService.readAllBookSeries(pageable);

        // then
        assertNotNull(response);
    }

    @Test
    public void shouldReadBookSeries() {
        // given
        var bookSeriesId = 1;
        var bookSeries = new BookSeries();
        bookSeries.setEISSN("eISSN1");
        bookSeries.setPrintISSN("printISSN1");

        when(bookSeriesJPAService.findOne(bookSeriesId)).thenReturn(bookSeries);

        // when
        var response = bookSeriesService.readBookSeries(bookSeriesId);

        // then
        assertNotNull(response);
        assertEquals(response.getEissn(), bookSeries.getEISSN());
        assertEquals(response.getPrintISSN(), bookSeries.getPrintISSN());
    }

    @Test
    public void shouldFindBookSeriesWhenSearchingWithSimpleQuery() {
        // given
        var tokens = Arrays.asList("DEF CON", "DEFCON");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new BookSeriesIndex(), new BookSeriesIndex())));

        // when
        var result = bookSeriesService.searchBookSeries(new ArrayList<>(tokens), pageable);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldReindexBookSeries() {
        // Given
        var bookSeries1 = new BookSeries();
        var bookSeries2 = new BookSeries();
        var bookSeries3 = new BookSeries();
        var bookSeries = Arrays.asList(bookSeries1, bookSeries2, bookSeries3);
        var page1 =
            new PageImpl<>(bookSeries.subList(0, 2), PageRequest.of(0, 10), bookSeries.size());
        var page2 =
            new PageImpl<>(bookSeries.subList(2, 3), PageRequest.of(1, 10), bookSeries.size());

        when(bookSeriesJPAService.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        bookSeriesService.reindexBookSeries();

        // Then
        verify(bookSeriesIndexRepository, times(1)).deleteAll();
        verify(bookSeriesJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(bookSeriesIndexRepository, atLeastOnce()).save(any(BookSeriesIndex.class));
    }
}
