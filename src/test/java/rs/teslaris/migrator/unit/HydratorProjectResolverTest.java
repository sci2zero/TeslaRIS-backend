package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.migration.ProjectMigrationLookup;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingReference;
import rs.teslaris.migrator.converter.hydrator.HydratorProjectResolver;

@ExtendWith(MockitoExtension.class)
public class HydratorProjectResolverTest {

    @Mock
    private ProjectMigrationLookup projectMigrationLookup;

    @InjectMocks
    private HydratorProjectResolver resolver;


    @Test
    void shouldResolveFctCodeByNationalId() {
        when(projectMigrationLookup.findIdsByNationalId("PTDC/ABC/1234/2020"))
            .thenReturn(List.of(7));

        var match = resolver.resolve(HydratorFundingReference.of("PTDC/ABC/1234/2020"));

        assertEquals(7, match.projectId());
        verify(projectMigrationLookup, never()).findIdsByInternalIdentifier(any());
    }

    @Test
    void shouldResolveQrenByInternalIdentifier() {
        when(projectMigrationLookup.findIdsByInternalIdentifier("POCI-01-0145-FEDER-000001"))
            .thenReturn(List.of(3));

        var match = resolver.resolve(HydratorFundingReference.of("POCI-01-0145-FEDER-000001"));

        assertEquals(3, match.projectId());
        verify(projectMigrationLookup, never()).findIdsByNationalId(any());
    }

    @Test
    void shouldNotResolveAmbiguousReference() {
        when(projectMigrationLookup.findIdsByNationalId("PTDC/ABC/1234/2020"))
            .thenReturn(List.of(4, 9));

        var match = resolver.resolve(HydratorFundingReference.of("PTDC/ABC/1234/2020"));

        assertFalse(match.found());
        assertEquals("ambiguous reference PTDC/ABC/1234/2020: matches 2 projects",
            match.reason());
    }

    @Test
    void shouldNotResolveWhenNotFound() {
        when(projectMigrationLookup.findIdsByNationalId("PTDC/ABC/1234/2020"))
            .thenReturn(List.of());

        var match = resolver.resolve(HydratorFundingReference.of("PTDC/ABC/1234/2020"));

        assertFalse(match.found());
        assertEquals("no project with reference PTDC/ABC/1234/2020", match.reason());
    }

    @Test
    void shouldNotQueryForWeakReference() {
        var match = resolver.resolve(HydratorFundingReference.of("123456"));

        assertFalse(match.found());
        verify(projectMigrationLookup, never()).findIdsByNationalId(any());
        verify(projectMigrationLookup, never()).findIdsByInternalIdentifier(any());
    }
}
