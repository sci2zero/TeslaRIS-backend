package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportEmployment;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.common.ExportPublisher;
import rs.teslaris.exporter.model.converter.push.vivo.VivoDocumentMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoOrganisationUnitMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoPersonMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoTeslarisLinks;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.model.push.PushedEntityState;
import rs.teslaris.exporter.model.push.PushedSharedNodeState;
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
            new Auth(AuthType.NONE, null, null), 100, null, null, List.of("org_units"), null);

    private static final Date LAST_UPDATED = new Date(1_000L);

    private static final String STALE_TRIPLE = "<http://stale> <http://p> \"old\" .";
    private static final String SHARED_TRIPLE = "<http://shared> <http://p> \"label\" .";
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

    private static ExportPerson person(boolean withExternalEmployment) {
        var person = new ExportPerson();
        person.setDatabaseId(7);
        person.setLastUpdated(LAST_UPDATED);
        person.setName(new ExportPersonName("Ivan", null, "Mrsulja"));
        if (withExternalEmployment) {
            person.getEmployments().add(new ExportEmployment(null, null, null, null, 43, null,
                new ArrayList<>(
                    List.of(new ExportMultilingualContent("EN", "University of Belgrade", 1)))));
        }
        return person;
    }

    private static VivoPersonMapper personMapper() {
        return new VivoPersonMapper(
            new VivoTeslarisLinks("http://localhost:5173/", "sr,en", "CRIS UNS"),
            new StaticMessageSource());
    }

    private static String externalOrganisationUri() {
        return personMapper().map(person(true), TARGET).sharedNodes().getFirst().uri();
    }

    private static PushedEntityState personState(List<String> dependencies) {
        var state = new PushedEntityState(TARGET.identifier(), PushEntityType.PERSONS, 7);
        state.setSnapshotLastUpdated(new Date(500L));
        state.setTriples(new ArrayList<>(List.of(STALE_TRIPLE)));
        state.setDependencies(new ArrayList<>(dependencies));
        return state;
    }

    private static PushedSharedNodeState sharedState(String uri) {
        var sharedState = new PushedSharedNodeState(TARGET.identifier(), uri);
        sharedState.setTriples(new ArrayList<>(List.of(SHARED_TRIPLE)));
        return sharedState;
    }

    private static ExportDocument documentSnapshot(ExportPublicationType type, Integer id) {
        var document = new ExportDocument();
        document.setType(type);
        document.setDatabaseId(id);
        document.setLastUpdated(LAST_UPDATED);
        document.setTitle(
            new ArrayList<>(List.of(new ExportMultilingualContent("EN", "Title " + id, 1))));
        return document;
    }

    @BeforeEach
    public void setUp() {
        pushExportService =
            new PushExportServiceImpl(mongoTemplate, rdfMapperRegistry, sparqlUpdateClient);
        lenient().when(rdfMapperRegistry.find("vivo", ExportOrganisationUnit.class))
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

    private void givenPerson(ExportPerson snapshot, List<PushedEntityState> states) {
        when(rdfMapperRegistry.find("vivo", ExportPerson.class))
            .thenReturn(Optional.of(personMapper()));
        when(mongoTemplate.find(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(List.of(snapshot));
        when(mongoTemplate.find(any(Query.class), eq(PushedEntityState.class)))
            .thenReturn(states);
    }

    private void verifySaved(Predicate<Object> condition) {
        verify(mongoTemplate).save(ArgumentMatchers.<Object>argThat(condition::test));
    }

    private void pushPersons() {
        pushExportService.pushEntities(TARGET, PushEntityType.PERSONS, false);
        verify(sparqlUpdateClient).deleteData(eq(TARGET), deletedCaptor.capture());
        verify(sparqlUpdateClient).insertData(eq(TARGET), insertedCaptor.capture());
    }

    @Test
    public void shouldCreateSharedNodeAndSaveDependency() {
        // Given
        var uri = externalOrganisationUri();
        givenPerson(person(true), List.of());

        // When
        pushPersons();

        // Then
        assertTrue(insertedCaptor.getValue().stream().anyMatch(triple ->
            triple.startsWith("<" + uri + "> <http://www.w3.org/2000/01/rdf-schema#label>")));
        verifySaved(saved -> saved instanceof PushedSharedNodeState sharedState &&
            uri.equals(sharedState.getUri()));
        verifySaved(saved -> saved instanceof PushedEntityState state &&
            List.of(uri).equals(state.getDependencies()));
    }

    @Test
    public void shouldNotSendSharedNodeThatAlreadyExists() {
        // Given
        var uri = externalOrganisationUri();
        givenPerson(person(true), List.of());
        when(mongoTemplate.find(any(Query.class), eq(PushedSharedNodeState.class)))
            .thenReturn(List.of(sharedState(uri)));

        // When
        pushPersons();

        // Then
        assertFalse(insertedCaptor.getValue().stream().anyMatch(triple ->
            triple.startsWith("<" + uri + "> <http://www.w3.org/2000/01/rdf-schema#label>")));
        verify(mongoTemplate, never()).save(any(PushedSharedNodeState.class));
    }

    @Test
    public void shouldDeleteSharedNodeWhenLastLinkIsRemoved() {
        // Given
        var uri = externalOrganisationUri();
        givenPerson(person(false), List.of(personState(List.of(uri))));
        var sharedState = sharedState(uri);
        when(mongoTemplate.find(any(Query.class), eq(PushedSharedNodeState.class)))
            .thenReturn(List.of(sharedState));
        when(mongoTemplate.count(any(Query.class), eq(PushedEntityState.class))).thenReturn(0L);

        // When
        pushPersons();

        // Then
        assertTrue(deletedCaptor.getValue().contains(SHARED_TRIPLE));
        verify(mongoTemplate).remove(sharedState);
        verifySaved(saved -> saved instanceof PushedEntityState state &&
            state.getDependencies().isEmpty());
    }

    @Test
    public void shouldKeepSharedNodeThatOtherEntitiesStillLinkTo() {
        // Given
        var uri = externalOrganisationUri();
        givenPerson(person(false), List.of(personState(List.of(uri))));
        when(mongoTemplate.find(any(Query.class), eq(PushedSharedNodeState.class)))
            .thenReturn(List.of(sharedState(uri)));
        when(mongoTemplate.count(any(Query.class), eq(PushedEntityState.class))).thenReturn(1L);

        // When
        pushPersons();

        // Then
        assertFalse(deletedCaptor.getValue().contains(SHARED_TRIPLE));
        verify(mongoTemplate, never()).remove(any(PushedSharedNodeState.class));
    }

    private void givenDocumentGroups(Map<ExportPublicationType, List<ExportDocument>> byType) {
        when(rdfMapperRegistry.find("vivo", ExportDocument.class)).thenReturn(Optional.of(
            new VivoDocumentMapper(
                new VivoTeslarisLinks("http://localhost:5173/", "sr,en", "CRIS UNS"))));
        when(mongoTemplate.find(any(Query.class), eq(ExportDocument.class)))
            .thenAnswer(invocation -> {
                Query query = invocation.getArgument(0);
                var filter = (org.bson.Document) query.getQueryObject().get("type");
                var types = (List<?>) filter.get("$in");
                return byType.entrySet().stream()
                    .filter(entry -> types.contains(entry.getKey().name()))
                    .flatMap(entry -> entry.getValue().stream()).toList();
            });
        when(mongoTemplate.find(any(Query.class), eq(PushedEntityState.class)))
            .thenReturn(List.of());
    }

    @Test
    public void shouldPushSeriesBeforeDocumentsWithSeparateStateTypes() {
        // Given
        var journal = documentSnapshot(ExportPublicationType.JOURNAL, 20);
        var article = documentSnapshot(ExportPublicationType.JOURNAL_PUBLICATION, 20);
        article.setJournal(documentSnapshot(ExportPublicationType.JOURNAL, 20));
        givenDocumentGroups(Map.of(
            ExportPublicationType.JOURNAL, List.of(journal),
            ExportPublicationType.JOURNAL_PUBLICATION, List.of(article)));

        // When
        pushExportService.pushEntities(TARGET, PushEntityType.DOCUMENTS, false);

        // Then
        verify(sparqlUpdateClient, times(2)).insertData(eq(TARGET), insertedCaptor.capture());
        var first = insertedCaptor.getAllValues().get(0);
        var second = insertedCaptor.getAllValues().get(1);
        assertTrue(first.stream().anyMatch(triple ->
            triple.startsWith("<http://vivo.example/individual/teslaris-series-20> " +
                "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")));
        assertTrue(second.stream().anyMatch(triple ->
            triple.startsWith("<http://vivo.example/individual/teslaris-document-20> " +
                "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")));

        // Same database ID, but different state types, so neither overwrites the other.
        verifySaved(saved -> saved instanceof PushedEntityState state &&
            state.getEntityType() == PushEntityType.PUBLICATION_SERIES &&
            state.getDatabaseId() == 20);
        verifySaved(saved -> saved instanceof PushedEntityState state &&
            state.getEntityType() == PushEntityType.DOCUMENTS && state.getDatabaseId() == 20);
    }

    @Test
    public void shouldReplaceChangedPublisherNodeMadeFromEntity() {
        // Given
        var monograph = documentSnapshot(ExportPublicationType.MONOGRAPH, 40);
        monograph.getPublishers().add(new ExportPublisher(
            new ArrayList<>(List.of(new ExportMultilingualContent("EN", "New Name", 1))), 9));
        givenDocumentGroups(Map.of(ExportPublicationType.MONOGRAPH, List.of(monograph)));

        var publisherUri = "http://vivo.example/individual/teslaris-publisher-9";
        var oldLabel = "<" + publisherUri + "> <http://www.w3.org/2000/01/rdf-schema#label> " +
            "\"Old Name\"@en .";
        var sharedState = new PushedSharedNodeState(TARGET.identifier(), publisherUri);
        sharedState.setTriples(new ArrayList<>(List.of(oldLabel)));
        when(mongoTemplate.find(any(Query.class), eq(PushedSharedNodeState.class)))
            .thenReturn(List.of(sharedState));

        // When
        pushExportService.pushEntities(TARGET, PushEntityType.DOCUMENTS, false);

        // Then
        verify(sparqlUpdateClient).deleteData(eq(TARGET), deletedCaptor.capture());
        verify(sparqlUpdateClient).insertData(eq(TARGET), insertedCaptor.capture());
        assertTrue(deletedCaptor.getValue().contains(oldLabel));
        assertTrue(insertedCaptor.getValue().contains(
            "<" + publisherUri + "> <http://www.w3.org/2000/01/rdf-schema#label> " +
                "\"New Name\"@en ."));
        verifySaved(saved -> saved == sharedState);
    }
}
