package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.OrganisationalUnit;
import rs.teslaris.core.repository.OrganisationalUnitRepository;
import rs.teslaris.core.service.impl.OrganisationalUnitServiceImpl;

@SpringBootTest
public class OrganisationalUnitServiceTest {

    @Mock
    private OrganisationalUnitRepository organisationalUnitRepository;

    @InjectMocks
    private OrganisationalUnitServiceImpl organisationalUnitService;


    @Test
    public void shouldReturnOrganisationalUnitWhenItExists() {
        // given
        var expected = new OrganisationalUnit();
        when(organisationalUnitRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        OrganisationalUnit result = organisationalUnitService.findOrganisationalUnitById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenOrganisationalUnitDoesNotExist() {
        // given
        when(organisationalUnitRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> organisationalUnitService.findOrganisationalUnitById(1));

        // then (NotFoundException should be thrown)
    }

}
