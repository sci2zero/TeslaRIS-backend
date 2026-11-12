package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.converter.push.vivo.VivoOrganisationUnitMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoTeslarisLinks;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.model.push.PushedEntityState;
import rs.teslaris.exporter.service.impl.PushExportServiceImpl;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.Auth;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.RdfMapperRegistry;
import rs.teslaris.exporter.util.push.SparqlUpdateClient;

@ExtendWith(MockitoExtension.class)
public class PushExportServiceTest {

    private static final PushTarget TARGET =
        new PushTarget("vivo-local", "vivo", true, "http://vivo.example/api/sparqlUpdate",
            "http://graph.example", "http://vivo.example/individual/",
            new Auth(AuthType.NONE, null, null), 100, null, null, List.of("org_units"));

    private static final Date LAST_UPDATED = new Date(1_000L);

    private static final String STALE_TRIPLE = "<http://stale> <http://p> \"old\" .";

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private RdfMapperRegistry rdfMapperRegistry;

    @Mock
    private SparqlUpdateClient sparqlUpdateClient;

    @Captor
    private ArgumentCaptor<Collection<String>> deletedCaptor;

    @Captor
    private ArgumentCaptor<Collection<String>> insertedCaptor;

    private PushExportServiceImpl pushExportService;


    private static ExportOrganisationUnit organisationUnit(boolean deleted) {
        var organisationUnit = new ExportOrganisationUnit();
        organisationUnit.setDatabaseId(5);
        organisationUnit.setLastUpdated(LAST_UPDATED);
        organisationUnit.setDeleted(deleted);
        organisationUnit.setName(
            new ArrayList<>(List.of(new ExportMultilingualContent("EN", "Faculty", 1))));
        return organisationUnit;
    }

    private static PushedEntityState state(Date snapshotLastUpdated, List<String> triples) {
        var state = new PushedEntityState(TARGET.identifier(), PushEntityType.ORGANISATION_UNITS,
            5);
        state.setSnapshotLastUpdated(snapshotLastUpdated);
        state.setTriples(new ArrayList<>(triples));
        return state;
    }

    @BeforeEach
    public void setUp() {
        pushExportService =
            new PushExportServiceImpl(mongoTemplate, rdfMapperRegistry, sparqlUpdateClient);
        when(rdfMapperRegistry.find("vivo", ExportOrganisationUnit.class))
            .thenReturn(Optional.of(new VivoOrganisationUnitMapper(
                new VivoTeslarisLinks("http://localhost:5173/", "sr,en", "CRIS UNS"))));
    }

    private void givenSnapshotAndStates(ExportOrganisationUnit snapshot,
                                        List<PushedEntityState> states) {
        when(mongoTemplate.find(any(Query.class), eq(ExportOrganisationUnit.class)))
            .thenReturn(List.of(snapshot));
        when(mongoTemplate.find(any(Query.class), eq(PushedEntityState.class)))
            .thenReturn(states);
    }

    private void push(boolean force) {
        pushExportService.pushEntities(TARGET, PushEntityType.ORGANISATION_UNITS, force);
        verify(sparqlUpdateClient).deleteData(eq(TARGET), deletedCaptor.capture());
        verify(sparqlUpdateClient).insertData(eq(TARGET), insertedCaptor.capture());
    }

    @Test
    public void shouldInsertNewEntityAndSaveState() {
        // Given
        givenSnapshotAndStates(organisationUnit(false), List.of());

        // When
        push(false);

        // Then
        assertTrue(deletedCaptor.getValue().isEmpty());
        assertFalse(insertedCaptor.getValue().isEmpty());
        var stateCaptor = ArgumentCaptor.forClass(PushedEntityState.class);
        verify(mongoTemplate).save(stateCaptor.capture());
        assertEquals(LAST_UPDATED, stateCaptor.getValue().getSnapshotLastUpdated());
        assertEquals(List.copyOf(insertedCaptor.getValue()), stateCaptor.getValue().getTriples());
    }

    @Test
    public void shouldSkipUnchangedEntity() {
        // Given
        givenSnapshotAndStates(organisationUnit(false),
            List.of(state(LAST_UPDATED, List.of(STALE_TRIPLE))));

        // When
        push(false);

        // Then
        assertTrue(deletedCaptor.getValue().isEmpty());
        assertTrue(insertedCaptor.getValue().isEmpty());
        verify(mongoTemplate, never()).save(any(PushedEntityState.class));
    }

    @Test
    public void shouldReplaceStaleTriplesWhenForced() {
        // Given
        givenSnapshotAndStates(organisationUnit(false),
            List.of(state(LAST_UPDATED, List.of(STALE_TRIPLE))));

        // When
        push(true);

        // Then
        assertEquals(List.of(STALE_TRIPLE), List.copyOf(deletedCaptor.getValue()));
        assertFalse(insertedCaptor.getValue().isEmpty());
        verify(mongoTemplate).save(any(PushedEntityState.class));
    }

    @Test
    public void shouldDeletePushedTriplesOfTombstone() {
        // Given
        var state = state(new Date(500L), List.of(STALE_TRIPLE));
        givenSnapshotAndStates(organisationUnit(true), List.of(state));

        // When
        push(false);

        // Then
        assertEquals(List.of(STALE_TRIPLE), List.copyOf(deletedCaptor.getValue()));
        assertTrue(insertedCaptor.getValue().isEmpty());
        verify(mongoTemplate).remove(state);
    }

    @Test
    public void shouldIgnoreTombstoneThatWasNeverPushed() {
        // Given
        givenSnapshotAndStates(organisationUnit(true), List.of());

        // When
        push(false);

        // Then
        assertTrue(deletedCaptor.getValue().isEmpty());
        assertTrue(insertedCaptor.getValue().isEmpty());
        verify(mongoTemplate, never()).save(any(PushedEntityState.class));
        verify(mongoTemplate, never()).remove(any(PushedEntityState.class));
    }
}
