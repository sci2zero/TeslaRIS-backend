package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
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

    @Test
    void shouldReturnIFTableContentForPublicationSeries() {
        // Given
        var publicationSeriesId = 1;

        var indicator1 = new Indicator();
        indicator1.setCode("IF2");

        var indicator2 = new Indicator();
        indicator2.setCode("IF2_RANK");

        var indicator3 = new Indicator();
        indicator3.setCode("IF5");

        var indicator4 = new Indicator();
        indicator4.setCode("IF5_RANK");

        var publicationSeriesIndicator1 =
            createPublicationSeriesIndicator("MATERIALS SCIENCE", indicator1, 2021, 5.2, null);
        var publicationSeriesIndicator2 =
            createPublicationSeriesIndicator("MATERIALS SCIENCE", indicator2, 2021, null, "10/100");
        var publicationSeriesIndicator3 =
            createPublicationSeriesIndicator("METALLURGY", indicator3, 2022, 4.8, null);
        var publicationSeriesIndicator4 =
            createPublicationSeriesIndicator("METALLURGY", indicator4, 2022, null, "5/50");

        when(publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeriesAndCode(
            eq(publicationSeriesId), anyString()))
            .thenReturn(List.of(publicationSeriesIndicator1, publicationSeriesIndicator2,
                publicationSeriesIndicator3, publicationSeriesIndicator4));

        // When
        var result = publicationSeriesIndicatorService.getIFTableContent(publicationSeriesId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    private PublicationSeriesIndicator createPublicationSeriesIndicator(String category,
                                                                        Indicator indicator,
                                                                        int year,
                                                                        Double numericValue,
                                                                        String textualValue) {
        var psi = new PublicationSeriesIndicator();
        psi.setCategoryIdentifier(category);
        psi.setIndicator(indicator);
        psi.setFromDate(LocalDate.of(year, 1, 1));
        psi.setNumericValue(numericValue);
        psi.setTextualValue(textualValue);
        return psi;
    }
}
