package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.identifier.IdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.IdentifierRepository;
import rs.teslaris.core.service.impl.identifier.IdentifierServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierCodeInUseException;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
class IdentifierServiceTest {

    @Mock
    private IdentifierRepository identifierRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private IdentifierServiceImpl identifierService;


    @Test
    void shouldReadAllIdentifiers() {
        // Given
        var pageable = Pageable.ofSize(10);

        var identifier = new Identifier();
        identifier.setId(1);
        identifier.setApplicableTypes(Set.of(ApplicableEntityType.ALL));

        var page = new PageImpl<>(List.of(identifier));

        when(identifierRepository.readAll("en", pageable)).thenReturn(page);

        // When
        var result = identifierService.readAllIdentifiers(pageable, "en");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(identifierRepository).readAll("en", pageable);
    }

    @Test
    void shouldReturnEmptyPageWhenNoIdentifiersExist() {
        // Given
        var pageable = Pageable.ofSize(10);
        when(identifierRepository.readAll("en", pageable)).thenReturn(Page.empty());

        // When
        var result = identifierService.readAllIdentifiers(pageable, "en");

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(identifierRepository).readAll("en", pageable);
    }


    // getIdentifiersApplicableToEntity

    @Test
    void shouldGetIdentifiersApplicableToEntity() {
        // Given
        var identifier1 = new Identifier();
        identifier1.setId(1);
        identifier1.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));
        var identifier2 = new Identifier();
        identifier2.setId(2);
        identifier2.setApplicableTypes(Set.of(ApplicableEntityType.PERSON));

        var applicableEntityTypes =
            new ArrayList<>(List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON));
        when(identifierRepository.getIdentifiersApplicableToEntity(applicableEntityTypes))
            .thenReturn(List.of(identifier1, identifier2));

        // When
        var result = identifierService.getIdentifiersApplicableToEntity(applicableEntityTypes);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(identifierRepository).getIdentifiersApplicableToEntity(applicableEntityTypes);
    }

    @Test
    void shouldAddAllTypeWhenNotPresentInApplicableTypes() {
        // Given
        var applicableEntityTypes =
            new ArrayList<>(List.of(ApplicableEntityType.DOCUMENT));
        when(identifierRepository.getIdentifiersApplicableToEntity(applicableEntityTypes))
            .thenReturn(List.of());

        // When
        identifierService.getIdentifiersApplicableToEntity(applicableEntityTypes);

        // Then
        assertTrue(applicableEntityTypes.contains(ApplicableEntityType.ALL));
        verify(identifierRepository).getIdentifiersApplicableToEntity(applicableEntityTypes);
    }

    @Test
    void shouldNotAddAllTypeWhenListIsEmpty() {
        // Given
        var applicableEntityTypes = new ArrayList<ApplicableEntityType>();
        when(identifierRepository.getIdentifiersApplicableToEntity(applicableEntityTypes))
            .thenReturn(List.of());

        // When
        identifierService.getIdentifiersApplicableToEntity(applicableEntityTypes);

        // Then
        assertFalse(applicableEntityTypes.contains(ApplicableEntityType.ALL));
        verify(identifierRepository).getIdentifiersApplicableToEntity(applicableEntityTypes);
    }


    // readIdentifierById

    @Test
    void shouldReadIdentifierById() {
        // Given
        var identifierId = 1;
        var identifier = new Identifier();
        identifier.setId(identifierId);
        identifier.setApplicableTypes(Set.of(ApplicableEntityType.ALL));
        when(identifierRepository.findById(identifierId)).thenReturn(Optional.of(identifier));

        // When
        var result = identifierService.readIdentifierById(identifierId);

        // Then
        assertNotNull(result);
        verify(identifierRepository).findById(identifierId);
    }

    @Test
    void shouldThrowExceptionWhenReadingNonExistentIdentifier() {
        // Given
        var identifierId = 99;
        when(identifierRepository.findById(identifierId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () ->
            identifierService.readIdentifierById(identifierId));
    }

    @Test
    void shouldReadIdentifierAccessLevel() {
        // Given
        var identifierId = 1;
        var identifier = new Identifier();
        identifier.setAccessLevel(AccessLevel.OPEN);
        when(identifierRepository.findById(identifierId)).thenReturn(Optional.of(identifier));

        // When
        var result = identifierService.readIdentifierAccessLevel(identifierId);

        // Then
        assertEquals(AccessLevel.OPEN, result);
        verify(identifierRepository).findById(identifierId);
    }

    @Test
    void shouldThrowExceptionWhenReadingAccessLevelForNonExistentIdentifier() {
        // Given
        var identifierId = 99;
        when(identifierRepository.findById(identifierId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () ->
            identifierService.readIdentifierAccessLevel(identifierId));
    }

    @Test
    void shouldGetIdentifierByCode() {
        // Given
        var code = "DOI";
        var identifier = new Identifier();
        identifier.setCode(code);
        when(identifierRepository.findByCode(code)).thenReturn(identifier);

        // When
        var result = identifierService.getIdentifierByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(identifierRepository).findByCode(code);
    }

    @Test
    void shouldReturnNullWhenCodeNotFound() {
        // Given
        when(identifierRepository.findByCode("UNKNOWN")).thenReturn(null);

        // When
        var result = identifierService.getIdentifierByCode("UNKNOWN");

        // Then
        assertNull(result);
        verify(identifierRepository).findByCode("UNKNOWN");
    }


    // createIdentifier

    @Test
    void shouldCreateIdentifier() {
        // Given
        var dto = new IdentifierDTO(null, "DOI",
            List.of(), List.of(), AccessLevel.OPEN, List.of(ApplicableEntityType.DOCUMENT),
            null, null);

        when(identifierRepository.identifierCodeInUse(dto.code(), null)).thenReturn(false);
        when(multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(any()))
            .thenReturn(new HashSet<>());
        when(identifierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        var result = identifierService.createIdentifier(dto);

        // Then
        assertNotNull(result);
        assertEquals("DOI", result.getCode());
        verify(identifierRepository).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCreatingIdentifierWithDuplicateCode() {
        // Given
        var dto = new IdentifierDTO(null, "DOI", List.of(), List.of(),
            AccessLevel.OPEN, List.of(ApplicableEntityType.DOCUMENT),
            null, null);

        when(identifierRepository.identifierCodeInUse(dto.code(), null)).thenReturn(true);

        // When & Then
        assertThrows(IdentifierCodeInUseException.class, () ->
            identifierService.createIdentifier(dto));

        verify(identifierRepository, never()).save(any());
    }

    @Test
    void shouldUpdateIdentifier() {
        // Given
        var identifierId = 1;
        var identifier = new Identifier();
        identifier.setId(identifierId);
        var dto = new IdentifierDTO(1, "ISSN",
            List.of(), List.of(), AccessLevel.OPEN,
            List.of(ApplicableEntityType.DOCUMENT), null, null);

        when(identifierRepository.findById(identifierId)).thenReturn(Optional.of(identifier));
        when(identifierRepository.identifierCodeInUse(dto.code(), identifierId)).thenReturn(false);
        when(multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(any()))
            .thenReturn(new HashSet<>());
        when(identifierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        identifierService.updateIdentifier(identifierId, dto);

        // Then
        assertEquals("ISSN", identifier.getCode());
        verify(identifierRepository).save(identifier);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithDuplicateCode() {
        // Given
        var identifierId = 1;
        var identifier = new Identifier();
        identifier.setId(identifierId);
        var dto = new IdentifierDTO(1, "DOI", List.of(), List.of(),
            AccessLevel.OPEN, null, null, null);

        when(identifierRepository.findById(identifierId)).thenReturn(Optional.of(identifier));
        when(identifierRepository.identifierCodeInUse(dto.code(), identifierId)).thenReturn(true);

        // When & Then
        assertThrows(IdentifierCodeInUseException.class, () ->
            identifierService.updateIdentifier(identifierId, dto));

        verify(identifierRepository, never()).save(any());
    }


    // deleteIdentifier

    @Test
    void shouldDeleteIdentifier() {
        // Given
        var identifierId = 1;
        when(identifierRepository.isInUse(identifierId)).thenReturn(false);
        when(identifierRepository.findById(identifierId)).thenReturn(Optional.of(new Identifier()));

        // When
        identifierService.deleteIdentifier(identifierId);

        // Then
        verify(identifierRepository).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingIdentifierInUse() {
        // Given
        var identifierId = 1;
        when(identifierRepository.isInUse(identifierId)).thenReturn(true);

        // When & Then
        assertThrows(IdentifierReferenceConstraintViolationException.class, () ->
            identifierService.deleteIdentifier(identifierId));

        verify(identifierRepository, never()).save(any());
    }
}
