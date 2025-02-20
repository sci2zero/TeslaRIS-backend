package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.document.PublicationSeriesServiceImpl;

@SpringBootTest
public class PublicationSeriesTest {

    @Mock
    private PublicationSeriesRepository publicationSeriesRepository;

    @InjectMocks
    private PublicationSeriesServiceImpl publicationSeriesService;

    private PublicationSeries publicationSeries;


    @BeforeEach
    void setUp() {
        publicationSeries = new Journal(); // Can be BookSeries, makes no difference for now
    }

    @Test
    void shouldFindPublicationSeriesByIssnWhenBothIssnsProvided() {
        // Given
        when(publicationSeriesRepository.findPublicationSeriesByeISSNOrPrintISSN("1234-5678",
            "8765-4321"))
            .thenReturn(List.of(publicationSeries));

        // When
        var result = publicationSeriesService.findPublicationSeriesByIssn("1234-5678", "8765-4321");

        // Then
        assertNotNull(result);
        assertEquals(publicationSeries, result);
        verify(publicationSeriesRepository).findPublicationSeriesByeISSNOrPrintISSN("1234-5678",
            "8765-4321");
    }

    @Test
    void shouldFindPublicationSeriesByIssnWhenOnlyeIssnProvided() {
        // Given
        when(publicationSeriesRepository.findPublicationSeriesByeISSNOrPrintISSN("1234-5678", ""))
            .thenReturn(List.of(publicationSeries));

        // When
        var result = publicationSeriesService.findPublicationSeriesByIssn("1234-5678", null);

        // Then
        assertNotNull(result);
        assertEquals(publicationSeries, result);
        verify(publicationSeriesRepository).findPublicationSeriesByeISSNOrPrintISSN("1234-5678",
            "");
    }

    @Test
    void shouldFindPublicationSeriesByIssnWhenOnlyPrintIssnProvided() {
        // Given
        when(publicationSeriesRepository.findPublicationSeriesByeISSNOrPrintISSN("", "8765-4321"))
            .thenReturn(List.of(publicationSeries));

        // When
        var result = publicationSeriesService.findPublicationSeriesByIssn(null, "8765-4321");

        // Then
        assertNotNull(result);
        assertEquals(publicationSeries, result);
        verify(publicationSeriesRepository).findPublicationSeriesByeISSNOrPrintISSN("",
            "8765-4321");
    }

    @Test
    void shouldNotFindPublicationSeriesByIssnWhenIssnsNotFound() {
        // Given
        when(publicationSeriesRepository.findPublicationSeriesByeISSNOrPrintISSN("1234-5678",
            "8765-4321"))
            .thenReturn(List.of());

        // When
        var result = publicationSeriesService.findPublicationSeriesByIssn("1234-5678", "8765-4321");

        // Then
        assertNull(result);
        verify(publicationSeriesRepository).findPublicationSeriesByeISSNOrPrintISSN("1234-5678",
            "8765-4321");
    }
}
