package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.BookSeriesIndexRepository;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.document.BookSeriesServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.BookSeriesJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
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

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @InjectMocks
    private BookSeriesServiceImpl bookSeriesService;


    @Test
    public void shouldReturnBookSeriesWhenItExists() {
        // given
        var expected = new BookSeries();
        when(publicationSeriesRepository.findById(1)).thenReturn(Optional.of(expected));

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
        bookSeriesService.updateBookSeries(bookSeriesId, bookSeriesDTO);

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

    @Test
    void shouldFindPublicationsForBookSeries() {
        // Given
        var bookSeriesId = 123;
        var pageable = Pageable.ofSize(10).withPage(0);

        when(documentPublicationIndexRepository.findByTypeInAndPublicationSeriesId(
            List.of(DocumentPublicationType.PROCEEDINGS.name(),
                DocumentPublicationType.MONOGRAPH.name()), bookSeriesId, pageable))
            .thenReturn(new PageImpl<>(
                List.of(new DocumentPublicationIndex(), new DocumentPublicationIndex())));

        // When
        var result = bookSeriesService.findPublicationsForBookSeries(bookSeriesId, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getSize() >= 2);
    }

    @Test
    void shouldForceDeleteDeleteBookSeries() {
        // Given
        var bookSeriesId = 1;

        when(bookSeriesIndexRepository.findBookSeriesIndexByDatabaseId(bookSeriesId)).thenReturn(
            Optional.empty());

        // When
        bookSeriesService.forceDeleteBookSeries(bookSeriesId);

        // Then
        verify(publicationSeriesRepository).unbindProceedings(bookSeriesId);
        verify(bookSeriesJPAService).delete(bookSeriesId);
        verify(bookSeriesIndexRepository, never()).delete(any());
    }

    @Test
    void shouldReturnRawBookSeries() {
        // Given
        var entityId = 123;
        var expected = new BookSeries();
        expected.setId(entityId);
        when(bookSeriesRepository.findRaw(entityId)).thenReturn(Optional.of(expected));

        // When
        var actual = bookSeriesService.findRaw(entityId);

        // Then
        assertEquals(expected, actual);
        verify(bookSeriesRepository).findRaw(entityId);
    }

    @Test
    void shouldThrowsNotFoundExceptionWhenBookSeriesDoesNotExist() {
        // Given
        var entityId = 123;
        when(bookSeriesRepository.findRaw(entityId)).thenReturn(Optional.empty());

        // When & Then
        var exception = assertThrows(NotFoundException.class,
            () -> bookSeriesService.findRaw(entityId));

        assertEquals("Book Series with given ID does not exist.", exception.getMessage());
        verify(bookSeriesRepository).findRaw(entityId);
    }

    @Test
    void givenBothIssnsBlank_whenReadBookSeriesByIssn_thenReturnNull() {
        // Given
        var eIssn = " ";
        String printIssn = null;

        // When
        BookSeriesIndex result = bookSeriesService.readBookSeriesByIssn(eIssn, printIssn);

        // Then
        assertNull(result);
        verifyNoInteractions(bookSeriesIndexRepository);
    }

    @Test
    void givenEissnBlank_whenReadBookSeriesByIssn_thenUsePrintIssn() {
        // Given
        var mockIndex = mock(BookSeriesIndex.class);
        var eIssn = " ";
        var printIssn = "1234-5678";
        when(bookSeriesIndexRepository.findBookSeriesIndexByeISSNOrPrintISSN(printIssn, printIssn))
            .thenReturn(Optional.of(mockIndex));

        // When
        BookSeriesIndex result = bookSeriesService.readBookSeriesByIssn(eIssn, printIssn);

        // Then
        assertEquals(result, mockIndex);
        verify(bookSeriesIndexRepository).findBookSeriesIndexByeISSNOrPrintISSN(printIssn,
            printIssn);
    }

    @Test
    void givenPrintIssnBlank_whenReadBookSeriesByIssn_thenUseEissn() {
        // Given
        var mockIndex = mock(BookSeriesIndex.class);
        var eIssn = "8765-4321";
        var printIssn = "";
        when(bookSeriesIndexRepository.findBookSeriesIndexByeISSNOrPrintISSN(eIssn, eIssn))
            .thenReturn(Optional.of(mockIndex));

        // When
        var result = bookSeriesService.readBookSeriesByIssn(eIssn, printIssn);

        // Then
        assertEquals(result, mockIndex);
        verify(bookSeriesIndexRepository).findBookSeriesIndexByeISSNOrPrintISSN(eIssn, eIssn);
    }

    @Test
    void givenBothIssnsPresentAndRepositoryReturnsResult_whenReadBookSeriesByIssn_thenReturnEntity() {
        // Given
        var mockIndex = mock(BookSeriesIndex.class);
        var eIssn = "8765-4321";
        var printIssn = "1234-5678";
        when(bookSeriesIndexRepository.findBookSeriesIndexByeISSNOrPrintISSN(eIssn, printIssn))
            .thenReturn(Optional.of(mockIndex));

        // When
        var result = bookSeriesService.readBookSeriesByIssn(eIssn, printIssn);

        // Then
        assertEquals(result, mockIndex);
        verify(bookSeriesIndexRepository).findBookSeriesIndexByeISSNOrPrintISSN(eIssn, printIssn);
    }

    @Test
    void givenBothIssnsPresentAndRepositoryReturnsEmpty_whenReadBookSeriesByIssn_thenReturnNull() {
        // Given
        String eIssn = "8765-4321";
        String printIssn = "1234-5678";
        when(bookSeriesIndexRepository.findBookSeriesIndexByeISSNOrPrintISSN(eIssn, printIssn))
            .thenReturn(Optional.empty());

        // When
        var result = bookSeriesService.readBookSeriesByIssn(eIssn, printIssn);

        // Then
        assertNull(result);
        verify(bookSeriesIndexRepository).findBookSeriesIndexByeISSNOrPrintISSN(eIssn, printIssn);
    }
}
