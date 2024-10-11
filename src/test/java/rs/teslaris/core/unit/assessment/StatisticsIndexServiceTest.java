package rs.teslaris.core.unit.assessment;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.service.impl.statistics.StatisticsServiceImpl;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.indexmodel.statistics.StatisticsIndex;
import rs.teslaris.core.indexrepository.statistics.StatisticsIndexRepository;

@SpringBootTest
public class StatisticsIndexServiceTest {

    @Mock
    private IndicatorService indicatorService;

    @Mock
    private StatisticsIndexRepository statisticsIndexRepository;

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
}
