package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.assessment.dto.EventIndicatorDTO;
import rs.teslaris.assessment.model.EventIndicator;
import rs.teslaris.assessment.model.Indicator;
import rs.teslaris.assessment.repository.EventIndicatorRepository;
import rs.teslaris.assessment.service.impl.EventIndicatorServiceImpl;
import rs.teslaris.assessment.service.impl.cruddelegate.EventIndicatorJPAServiceImpl;
import rs.teslaris.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.user.UserService;

@SpringBootTest
public class EventIndicatorServiceTest {

    @Mock
    private EventIndicatorRepository eventIndicatorRepository;

    @Mock
    private IndicatorService indicatorService;

    @Mock
    private EventService eventService;

    @Mock
    private UserService userService;

    @Mock
    private EventIndicatorJPAServiceImpl eventIndicatorJPAService;

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

    @Test
    void shouldCreateEventIndicator() {
        var eventIndicatorDTO = new EventIndicatorDTO();
        eventIndicatorDTO.setNumericValue(20d);
        eventIndicatorDTO.setFromDate(LocalDate.of(2023, 3, 4));
        eventIndicatorDTO.setToDate(LocalDate.of(2023, 4, 4));
        eventIndicatorDTO.setEventId(1);
        eventIndicatorDTO.setIndicatorId(1);

        var newEventIndicator = new EventIndicator();
        newEventIndicator.setIndicator(new Indicator());

        when(eventService.findOne(1)).thenReturn(new Conference());
        when(eventIndicatorJPAService.save(any(EventIndicator.class)))
            .thenReturn(newEventIndicator);
        when(indicatorService.findOne(1)).thenReturn(new Indicator());
        when(userService.findOne(1)).thenReturn(new User());

        var result = eventIndicatorService.createEventIndicator(
            eventIndicatorDTO, 1);

        assertNotNull(result);
        verify(eventIndicatorJPAService).save(any(EventIndicator.class));
        verify(userService).findOne(1);
    }

    @Test
    void shouldUpdateEventIndicator() {
        var eventIndicatorId = 1;
        var eventIndicatorDTO = new EventIndicatorDTO();
        eventIndicatorDTO.setNumericValue(20d);
        eventIndicatorDTO.setFromDate(LocalDate.of(2023, 3, 4));
        eventIndicatorDTO.setToDate(LocalDate.of(2023, 4, 4));
        eventIndicatorDTO.setEventId(1);
        eventIndicatorDTO.setIndicatorId(1);

        var existingEventIndicator = new EventIndicator();
        existingEventIndicator.setIndicator(new Indicator());

        when(eventIndicatorJPAService.findOne(eventIndicatorId)).thenReturn(
            existingEventIndicator);
        when(eventService.findOne(1)).thenReturn(new Conference());
        when(indicatorService.findOne(1)).thenReturn(new Indicator());

        eventIndicatorService.updateEventIndicator(eventIndicatorId,
            eventIndicatorDTO);

        verify(eventIndicatorJPAService).findOne(eventIndicatorId);
        verify(eventIndicatorJPAService).save(existingEventIndicator);
    }
}