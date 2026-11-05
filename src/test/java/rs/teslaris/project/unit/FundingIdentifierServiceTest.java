package rs.teslaris.project.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.project.dto.identifier.FundingIdentifierDTO;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.identifier.FundingIdentifier;
import rs.teslaris.project.repository.identifier.FundingIdentifierRepository;
import rs.teslaris.project.service.impl.identifier.FundingIdentifierServiceImpl;
import rs.teslaris.project.service.impl.identifier.cruddelegate.FundingIdentifierJPAServiceImpl;
import rs.teslaris.project.service.interfaces.funding.FundingService;

@SpringBootTest
public class FundingIdentifierServiceTest {

    @Mock
    private FundingIdentifierRepository fundingIdentifierRepository;

    @Mock
    private FundingIdentifierJPAServiceImpl fundingIdentifierJPAService;

    @Mock
    private FundingService fundingService;

    @Mock
    private EntityIdentifierRepository entityIdentifierRepository;

    @Mock
    private IdentifierService identifierService;

    @InjectMocks
    private FundingIdentifierServiceImpl fundingIdentifierService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllFundingIdentifiersForFunding(AccessLevel accessLevel) {
        // Given
        var fundingId = 1;

        var identifier = new Identifier();
        identifier.setAccessLevel(accessLevel);
        identifier.setApplicableTypes(new HashSet<>(List.of(ApplicableEntityType.FUNDING)));

        var fundingIdentifier1 = new FundingIdentifier();
        fundingIdentifier1.setIdentifier(identifier);

        var fundingIdentifier2 = new FundingIdentifier();
        fundingIdentifier2.setIdentifier(identifier);

        when(
            fundingIdentifierRepository.findIdentifiersForFundingAndIdentifierAccessLevel(fundingId,
                accessLevel)).thenReturn(List.of(fundingIdentifier1, fundingIdentifier2));

        // When
        var response = fundingIdentifierService.getIdentifiersForFunding(fundingId, accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
        verify(fundingIdentifierRepository)
            .findIdentifiersForFundingAndIdentifierAccessLevel(fundingId, accessLevel);
    }

    @Test
    void shouldReturnEmptyListWhenNoIdentifiersExistForFunding() {
        // Given
        var fundingId = 1;

        when(
            fundingIdentifierRepository.findIdentifiersForFundingAndIdentifierAccessLevel(fundingId,
                AccessLevel.OPEN)).thenReturn(List.of());

        // When
        var response =
            fundingIdentifierService.getIdentifiersForFunding(fundingId, AccessLevel.OPEN);

        // Then
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldCreateFundingIdentifier() {
        // Given
        var fundingIdentifierDTO = new FundingIdentifierDTO();
        fundingIdentifierDTO.setFundingId(1);
        fundingIdentifierDTO.setIdentifierId(1);
        fundingIdentifierDTO.setValue("10.1234/test");

        var newFundingIdentifier = new FundingIdentifier();
        newFundingIdentifier.setIdentifier(new Identifier());

        when(fundingService.findOne(1)).thenReturn(new Funding());
        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(fundingIdentifierJPAService.save(any(FundingIdentifier.class)))
            .thenReturn(newFundingIdentifier);

        // When
        var result = fundingIdentifierService.createFundingIdentifier(fundingIdentifierDTO);

        // Then
        assertNotNull(result);
        verify(fundingService).findOne(1);
        verify(fundingIdentifierJPAService).save(any(FundingIdentifier.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingFundingIdentifierWithNonExistentFunding() {
        // Given
        var fundingIdentifierDTO = new FundingIdentifierDTO();
        fundingIdentifierDTO.setFundingId(99);
        fundingIdentifierDTO.setIdentifierId(1);
        fundingIdentifierDTO.setValue("10.1234/test");

        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(fundingService.findOne(99)).thenThrow(new NotFoundException("Funding not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            fundingIdentifierService.createFundingIdentifier(fundingIdentifierDTO));

        verify(fundingIdentifierJPAService, never()).save(any());
    }

    @Test
    void shouldUpdateFundingIdentifier() {
        // Given
        var fundingIdentifierId = 1;
        var fundingIdentifierDTO = new FundingIdentifierDTO();
        fundingIdentifierDTO.setFundingId(1);
        fundingIdentifierDTO.setIdentifierId(1);
        fundingIdentifierDTO.setValue("10.1234/updated");

        var existingFundingIdentifier = new FundingIdentifier();
        existingFundingIdentifier.setIdentifier(new Identifier());

        when(fundingIdentifierJPAService.findOne(fundingIdentifierId))
            .thenReturn(existingFundingIdentifier);
        when(fundingService.findOne(1)).thenReturn(new Funding());
        when(identifierService.findOne(1)).thenReturn(new Identifier());

        // When
        fundingIdentifierService.updateFundingIdentifier(fundingIdentifierId, fundingIdentifierDTO);

        // Then
        verify(fundingIdentifierJPAService).findOne(fundingIdentifierId);
        verify(fundingService).findOne(1);
        verify(fundingIdentifierJPAService).save(existingFundingIdentifier);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentFundingIdentifier() {
        // Given
        var fundingIdentifierId = 99;
        var fundingIdentifierDTO = new FundingIdentifierDTO();
        fundingIdentifierDTO.setFundingId(1);
        fundingIdentifierDTO.setIdentifierId(1);

        when(fundingIdentifierJPAService.findOne(fundingIdentifierId))
            .thenThrow(new NotFoundException("Funding identifier not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            fundingIdentifierService.updateFundingIdentifier(fundingIdentifierId,
                fundingIdentifierDTO));

        verify(fundingIdentifierJPAService, never()).save(any());
    }
}
