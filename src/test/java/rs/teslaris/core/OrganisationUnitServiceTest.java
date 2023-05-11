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
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.person.OrganisationalUnitRepository;
import rs.teslaris.core.service.impl.OrganisationUnitServiceImpl;

@SpringBootTest
public class OrganisationUnitServiceTest {

    @Mock
    private OrganisationalUnitRepository organisationalUnitRepository;

    @InjectMocks
    private OrganisationUnitServiceImpl organisationalUnitService;


    @Test
    public void shouldReturnOrganisationUnitWhenItExists() {
        // given
        var expected = new OrganisationUnit();
        when(organisationalUnitRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = organisationalUnitService.findOrganisationalUnitById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenOrganisationUnitDoesNotExist() {
        // given
        when(organisationalUnitRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> organisationalUnitService.findOrganisationalUnitById(1));

        // then (NotFoundException should be thrown)
    }

}
