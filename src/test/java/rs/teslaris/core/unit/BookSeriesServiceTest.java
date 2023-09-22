package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.service.impl.document.BookSeriesServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.BookSeriesReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class BookSeriesServiceTest {

    @Mock
    private BookSeriesRepository bookSeriesRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private PersonContributionService personContributionService;

    @InjectMocks
    private BookSeriesServiceImpl bookSeriesService;


    @Test
    public void shouldReturnBookSeriesWhenItExists() {
        // given
        var expected = new BookSeries();
        when(bookSeriesRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = bookSeriesService.findOne(1);

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
        bookSeriesDTO.setEISSN("eISSN");
        bookSeriesDTO.setPrintISSN("printISSN");
        bookSeriesDTO.setContributions(new ArrayList<>());
        bookSeriesDTO.setLanguageTagIds(new ArrayList<>());

        when(bookSeriesRepository.save(any())).thenReturn(new BookSeries());

        // when
        var savedBookSeries = bookSeriesService.createBookSeries(bookSeriesDTO);

        // then
        assertNotNull(savedBookSeries);
        verify(bookSeriesRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdateBookSeriesWhenProvidedWithValidData() {
        // given
        var bookSeriesId = 1;
        var bookSeriesDTO = new BookSeriesDTO();
        bookSeriesDTO.setTitle(new ArrayList<>());
        bookSeriesDTO.setNameAbbreviation(new ArrayList<>());
        bookSeriesDTO.setEISSN("eISSN");
        bookSeriesDTO.setPrintISSN("printISSN");
        bookSeriesDTO.setContributions(new ArrayList<>());
        bookSeriesDTO.setLanguageTagIds(new ArrayList<>());

        var bookSeries = new BookSeries();
        bookSeries.setLanguages(new HashSet<>());

        when(bookSeriesRepository.findById(bookSeriesId)).thenReturn(Optional.of(bookSeries));
        when(bookSeriesRepository.save(any())).thenReturn(new BookSeries());

        // when
        bookSeriesService.updateBookSeries(bookSeriesDTO, bookSeriesId);

        // then
        verify(bookSeriesRepository, times(1)).save(any());
    }

    @Test
    public void shouldDeleteBookSeriesWhenNotUsed() {
        // given
        var bookSeriesId = 1;
        var bookSeriesToDelete = new BookSeries();

        when(bookSeriesRepository.findById(bookSeriesId)).thenReturn(
            Optional.of(bookSeriesToDelete));
        when(bookSeriesRepository.hasProceedings(bookSeriesId)).thenReturn(false);

        // when
        bookSeriesService.deleteBookSeries(bookSeriesId);

        // then
        verify(bookSeriesRepository, times(1)).save(bookSeriesToDelete);
        verify(bookSeriesRepository, never()).delete(any());
    }

    @Test
    public void shouldNotDeleteBookSeriesIfInUsage() {
        // given
        var bookSeriesId = 1;
        var bookSeriesToDelete = new BookSeries();

        when(bookSeriesRepository.findById(bookSeriesId)).thenReturn(
            Optional.of(bookSeriesToDelete));
        when(bookSeriesRepository.hasProceedings(bookSeriesId)).thenReturn(true);

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
        bookSeries1.setTitle(new HashSet<>());
        bookSeries1.setNameAbbreviation(new HashSet<>());
        bookSeries1.setEISSN("eISSN1");
        bookSeries1.setPrintISSN("printISSN1");
        bookSeries1.setContributions(new HashSet<>());
        bookSeries1.setLanguages(new HashSet<>());
        var bookSeries2 = new BookSeries();
        bookSeries2.setTitle(new HashSet<>());
        bookSeries2.setNameAbbreviation(new HashSet<>());
        bookSeries2.setEISSN("eISSN2");
        bookSeries2.setPrintISSN("printISSN2");
        bookSeries2.setContributions(new HashSet<>());
        bookSeries2.setLanguages(new HashSet<>());

        when(bookSeriesRepository.findAll(pageable)).thenReturn(
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
        bookSeries.setTitle(new HashSet<>());
        bookSeries.setNameAbbreviation(new HashSet<>());
        bookSeries.setEISSN("eISSN1");
        bookSeries.setPrintISSN("printISSN1");
        bookSeries.setContributions(new HashSet<>());
        bookSeries.setLanguages(new HashSet<>());

        when(bookSeriesRepository.findById(bookSeriesId)).thenReturn(Optional.of(bookSeries));

        // when
        var response = bookSeriesService.readBookSeries(bookSeriesId);

        // then
        assertNotNull(response);
        assertEquals(response.getEISSN(), bookSeries.getEISSN());
        assertEquals(response.getPrintISSN(), bookSeries.getPrintISSN());
    }
}
