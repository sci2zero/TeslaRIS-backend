package rs.teslaris.project.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.dto.funding.FundingDTO;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.project.migration.FundingMigrationWriterImpl;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.identifier.FundingIdentifier;
import rs.teslaris.project.repository.identifier.FundingIdentifierRepository;
import rs.teslaris.project.service.interfaces.funding.FundingService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FundingMigrationWriterTest {

    private static final String CODE = "TESTCODE";

    @Mock
    private FundingService fundingService;

    @Mock
    private IdentifierService identifierService;

    @Mock
    private EntityIdentifierRepository entityIdentifierRepository;

    @Mock
    private FundingIdentifierRepository fundingIdentifierRepository;

    @InjectMocks
    private FundingMigrationWriterImpl writer;


    private static Funding funding(Integer id) {
        var funding = new Funding();
        funding.setId(id);
        return funding;
    }

    private static Identifier identifier(Integer id) {
        var identifier = new Identifier();
        identifier.setId(id);
        return identifier;
    }

    @Test
    public void shouldAttachIdentifierToCreatedFunding() {
        var funding = funding(1);
        var identifier = new Identifier();
        when(fundingService.createFunding(any())).thenReturn(funding);
        when(identifierService.getIdentifierByCode(CODE)).thenReturn(identifier);

        var migrated = writer.migrate(new FundingDTO(), null, Map.of(CODE, "12345ABC"));

        assertEquals(1, migrated.id());
        assertTrue(migrated.created());
        var captor = ArgumentCaptor.forClass(FundingIdentifier.class);
        verify(fundingIdentifierRepository).save(captor.capture());
        assertEquals(funding, captor.getValue().getFunding());
        assertEquals(identifier, captor.getValue().getIdentifier());
        assertEquals("12345ABC", captor.getValue().getValue());
        verify(entityIdentifierRepository, never()).existsByValueForType(any(), any(), any());
        assertTrue(migrated.droppedIdentifiers().isEmpty());
    }

    @Test
    public void shouldNotAttachIdentifierTwiceOnRerun() {
        when(fundingService.findOne(1)).thenReturn(funding(1));
        when(identifierService.getIdentifierByCode(CODE)).thenReturn(identifier(3));
        when(fundingIdentifierRepository.existsByFundingIdAndIdentifierIdAndValue(1, 3,
            "12345ABC")).thenReturn(true);

        var migrated = writer.migrate(new FundingDTO(), 1, Map.of(CODE, "12345ABC"));

        assertEquals(1, migrated.id());
        assertTrue(migrated.droppedIdentifiers().isEmpty());
        verify(fundingService).updateFunding(any(), any());
        verify(fundingIdentifierRepository, never()).save(any());
    }

    @Test
    public void shouldDropIdentifierTakenByAnotherFunding() {
        when(fundingService.findOne(1)).thenReturn(funding(1));
        when(identifierService.getIdentifierByCode(CODE)).thenReturn(identifier(3));
        when(entityIdentifierRepository.existsByValueForType("12345ABC", FundingIdentifier.class,
            null)).thenReturn(true);

        var migrated = writer.migrate(new FundingDTO(), 1, Map.of(CODE, "12345ABC"));

        assertEquals("value 12345ABC is already taken by another funding",
            migrated.droppedIdentifiers().get(CODE));
        verify(fundingIdentifierRepository, never()).save(any());
    }

    @Test
    public void shouldDropIdentifierWithoutDefinition() {
        when(fundingService.createFunding(any())).thenReturn(funding(1));
        when(identifierService.getIdentifierByCode(CODE)).thenReturn(null);

        var migrated = writer.migrate(new FundingDTO(), null, Map.of(CODE, "12345ABC"));

        assertEquals("no identifier is configured for code " + CODE,
            migrated.droppedIdentifiers().get(CODE));
        verify(fundingIdentifierRepository, never()).save(any());
    }

    @Test
    public void shouldNotAttachIdentifierWhenFundingIsNotCreated() {
        when(fundingService.createFunding(any())).thenReturn(null);

        assertNull(writer.migrate(new FundingDTO(), null, Map.of(CODE, "12345ABC")));
        verify(fundingIdentifierRepository, never()).save(any());
    }
}
