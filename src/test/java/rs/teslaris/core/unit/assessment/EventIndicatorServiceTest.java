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
import rs.teslaris.core.assessment.model.EventIndicator;
import rs.teslaris.core.assessment.model.Indicator;
import rs.teslaris.core.assessment.repository.EventIndicatorRepository;
import rs.teslaris.core.assessment.service.impl.EventIndicatorServiceImpl;
import rs.teslaris.core.model.commontypes.AccessLevel;

@SpringBootTest
public class EventIndicatorServiceTest {

    @Mock
    private EventIndicatorRepository eventIndicatorRepository;

    @InjectMocks
    private EventIndicatorServiceImpl eventIndicatorService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllEventIndicatorsForEvent(AccessLevel accessLevel) {
        // Given
        var eventId = 1;

        var indicator = new Indicator();
        indicator.setAccessLevel(AccessLevel.OPEN);

        var eventIndicator1 = new EventIndicator();
        eventIndicator1.setNumericValue(12d);
        eventIndicator1.setIndicator(indicator);

        var eventIndicator2 = new EventIndicator();
        eventIndicator2.setNumericValue(11d);
        eventIndicator2.setIndicator(indicator);

        when(
            eventIndicatorRepository.findIndicatorsForEventAndIndicatorAccessLevel(eventId,
                accessLevel)).thenReturn(
            List.of(eventIndicator1, eventIndicator2));

        // When
        var response =
            eventIndicatorService.getIndicatorsForEvent(eventId,
                accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }
}