package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.assessment.service.impl.statistics.StatisticsServiceImpl;
import rs.teslaris.assessment.service.interfaces.indicator.IndicatorService;
import rs.teslaris.assessment.util.GeoliteIPUtil;
import rs.teslaris.core.indexmodel.statistics.StatisticsIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.indexrepository.statistics.StatisticsIndexRepository;

@SpringBootTest
public class StatisticsIndexServiceTest {

    @Mock
    private IndicatorService indicatorService;

    @Mock
    private StatisticsIndexRepository statisticsIndexRepository;

    @Mock
    private GeoliteIPUtil geoliteIPUtil;

    @InjectMocks
    private StatisticsServiceImpl statisticsIndexService;


    @Test
    void shouldSavePersonView() {
        // Given
        var personId = 123;
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setPersonId(personId);

        // When
        statisticsIndexService.savePersonView(personId);

        // Then
        verify(statisticsIndexRepository, times(1)).save(argThat(statistics ->
            statistics.getPersonId().equals(personId)
        ));
    }

    @ParameterizedTest
    @EnumSource(value = StatisticsType.class, names = {"VIEW", "DOWNLOAD"})
    void shouldFetchStatisticsTypeIndicators(StatisticsType type) {
        // Given & When
        var result = statisticsIndexService.fetchStatisticsTypeIndicators(type);

        // Then
        assertNotNull(result);
    }

    @Test
    void shouldSaveDocumentView() {
        // Given
        var documentId = 456;
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setDocumentId(documentId);

        // When
        statisticsIndexService.saveDocumentView(documentId);

        // Then
        verify(statisticsIndexRepository, times(1)).save(argThat(statistics ->
            statistics.getDocumentId().equals(documentId)
        ));
    }

    @Test
    void shouldSaveOrganisationUnitView() {
        // Given
        var organisationUnitId = 789;
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setOrganisationUnitId(organisationUnitId);

        // When
        statisticsIndexService.saveOrganisationUnitView(organisationUnitId);

        // Then
        verify(statisticsIndexRepository, times(1)).save(argThat(statistics ->
            statistics.getOrganisationUnitId().equals(organisationUnitId)
        ));
    }

    @Test
    void shouldSaveDocumentDownload() {
        // Given
        var documentId = 456;
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setDocumentId(documentId);

        // When
        statisticsIndexService.saveDocumentDownload(documentId);

        // Then
        verify(statisticsIndexRepository, times(1)).save(argThat(statistics ->
            statistics.getDocumentId().equals(documentId)
        ));
    }

    @Test
    void shouldSavePublicationSeriesView() {
        // Given
        var publicationSeriesId = 456;

        // When
        statisticsIndexService.savePublicationSeriesView(publicationSeriesId);

        // Then
        verify(statisticsIndexRepository, times(1)).save(argThat(statistics ->
            publicationSeriesId == statistics.getPublicationSeriesId()
        ));
    }

    @Test
    void shouldSaveEventView() {
        // Given
        var eventId = 789;

        // When
        statisticsIndexService.saveEventView(eventId);

        // Then
        verify(statisticsIndexRepository, times(1)).save(argThat(statistics ->
            eventId == statistics.getEventId()
        ));
    }
}
