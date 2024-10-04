package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.model.Indicator;
import rs.teslaris.core.assessment.model.PublicationSeriesIndicator;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.assessment.service.impl.PublicationSeriesIndicatorServiceImpl;
import rs.teslaris.core.model.commontypes.AccessLevel;

@SpringBootTest
public class PublicationSeriesIndicatorServiceTest {

    @Mock
    private PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    @InjectMocks
    private PublicationSeriesIndicatorServiceImpl publicationSeriesIndicatorService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllPublicationSeriesIndicatorsForPublicationSeries(
        AccessLevel accessLevel) {
        // Given
        var publicationSeriesId = 1;

        var indicator = new Indicator();
        indicator.setAccessLevel(AccessLevel.OPEN);

        var publicationSeriesIndicator1 = new PublicationSeriesIndicator();
        publicationSeriesIndicator1.setNumericValue(12d);
        publicationSeriesIndicator1.setIndicator(indicator);

        var publicationSeriesIndicator2 = new PublicationSeriesIndicator();
        publicationSeriesIndicator2.setNumericValue(11d);
        publicationSeriesIndicator2.setIndicator(indicator);

        when(
            publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeriesAndIndicatorAccessLevel(
                publicationSeriesId,
                accessLevel)).thenReturn(
            List.of(publicationSeriesIndicator1, publicationSeriesIndicator2));

        // When
        var response =
            publicationSeriesIndicatorService.getIndicatorsForPublicationSeries(publicationSeriesId,
                accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }
}
