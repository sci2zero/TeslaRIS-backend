package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.dto.person.PrizeResponseDTO;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.migrator.converter.hydrator.HydratorSource;
import rs.teslaris.migrator.converter.hydrator.PrizeEntityCreator;
import rs.teslaris.migrator.converter.hydrator.PrizeMigrationDTO;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;

@ExtendWith(MockitoExtension.class)
public class PrizeEntityCreatorTest {

    @Mock
    private PrizeService prizeService;

    @Mock
    private MigrationIdResolver idResolver;

    @InjectMocks
    private PrizeEntityCreator creator;


    @Test
    void shouldAddPrizeToResolvedPerson() {
        var prize = new PrizeDTO();
        var response = new PrizeResponseDTO();
        response.setId(42);
        when(idResolver.resolve(HydratorSource.NAME, MigrationEntityType.PERSON, "cv-1"))
            .thenReturn(Optional.of(5));
        when(prizeService.addPrize(5, prize, false)).thenReturn(response);

        var id = creator.create(new PrizeMigrationDTO("cv-1", "7", prize, null), false);

        assertEquals(42, id);
    }

    @Test
    void shouldFailWhenPersonIsNotMigrated() {
        when(idResolver.resolve(HydratorSource.NAME, MigrationEntityType.PERSON, "cv-1"))
            .thenReturn(Optional.empty());

        assertThrows(MigrationException.class,
            () -> creator.create(new PrizeMigrationDTO("cv-1", "7", new PrizeDTO(), null), true));
        verify(prizeService, never()).addPrize(anyInt(), any(), anyBoolean());
    }

    @Test
    void shouldFailItemWithInvalidSourceValue() {
        var dto = new PrizeMigrationDTO("cv-1", "7", new PrizeDTO(), "invalid effective date 'x'");

        var exception = assertThrows(InvalidSourceValueException.class,
            () -> creator.create(dto, false));

        assertEquals("invalid effective date 'x'", exception.getMessage());
        verify(prizeService, never()).addPrize(anyInt(), any(), anyBoolean());
    }
}
