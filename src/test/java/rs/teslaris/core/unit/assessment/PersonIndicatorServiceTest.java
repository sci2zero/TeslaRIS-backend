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
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.model.indicator.PersonIndicator;
import rs.teslaris.assessment.repository.indicator.PersonIndicatorRepository;
import rs.teslaris.assessment.service.impl.indicator.PersonIndicatorServiceImpl;
import rs.teslaris.core.model.commontypes.AccessLevel;

@SpringBootTest
public class PersonIndicatorServiceTest {

    @Mock
    private PersonIndicatorRepository personIndicatorRepository;

    @InjectMocks
    private PersonIndicatorServiceImpl personIndicatorService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllPersonIndicatorsForPerson(AccessLevel accessLevel) {
        // Given
        var personId = 1;

        var indicator = new Indicator();
        indicator.setAccessLevel(AccessLevel.OPEN);

        var personIndicator1 = new PersonIndicator();
        personIndicator1.setNumericValue(12d);
        personIndicator1.setIndicator(indicator);

        var personIndicator2 = new PersonIndicator();
        personIndicator2.setNumericValue(11d);
        personIndicator2.setIndicator(indicator);

        when(
            personIndicatorRepository.findIndicatorsForPersonAndIndicatorAccessLevel(personId,
                accessLevel)).thenReturn(
            List.of(personIndicator1, personIndicator2));

        // When
        var response =
            personIndicatorService.getIndicatorsForPerson(personId,
                accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }
}
