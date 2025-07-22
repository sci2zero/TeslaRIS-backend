package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.institution.OrganisationUnitTrustConfigurationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitTrustConfiguration;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitTrustConfigurationRepository;
import rs.teslaris.core.service.impl.institution.OrganisationUnitTrustConfigurationServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;

@SpringBootTest
public class OrganisationUnitTrustConfigurationServiceTest {

    @Mock
    private OrganisationUnitTrustConfigurationRepository configurationRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @InjectMocks
    private OrganisationUnitTrustConfigurationServiceImpl service;


    @Test
    public void shouldReturnExistingTrustConfiguration() {
        // Given
        int organisationUnitId = 1;
        var config = new OrganisationUnitTrustConfiguration();
        config.setTrustNewDocumentFiles(false);
        config.setTrustNewPublications(true);

        when(configurationRepository.findConfigurationForOrganisationUnit(organisationUnitId))
            .thenReturn(Optional.of(config));

        // When
        var result = service.readTrustConfigurationForOrganisationUnit(organisationUnitId);

        // Then
        assertTrue(result.trustNewPublications());
        assertFalse(result.trustNewDocumentFiles());
    }

    @Test
    public void shouldReturnDefaultTrustConfigurationWhenNoneExists() {
        // Given
        int organisationUnitId = 2;
        when(configurationRepository.findConfigurationForOrganisationUnit(organisationUnitId))
            .thenReturn(Optional.empty());

        // When
        var result = service.readTrustConfigurationForOrganisationUnit(organisationUnitId);

        // Then
        assertTrue(result.trustNewPublications());
        assertFalse(result.trustNewDocumentFiles());
    }

    @Test
    public void shouldSaveNewTrustConfigurationIfNoneExists() {
        // Given
        int organisationUnitId = 3;
        var dto = new OrganisationUnitTrustConfigurationDTO(false, true);
        var organisationUnit = new OrganisationUnit();

        when(configurationRepository.findConfigurationForOrganisationUnit(organisationUnitId))
            .thenReturn(Optional.empty());
        when(organisationUnitService.findOrganisationUnitById(organisationUnitId))
            .thenReturn(organisationUnit);

        // Spy to verify save
        doAnswer(invocation -> invocation.getArgument(0))
            .when(configurationRepository).save(any());

        // When
        var result = service.saveConfiguration(dto, organisationUnitId);

        // Then
        assertEquals(dto, result);
        verify(configurationRepository).save(argThat(config ->
            config.getOrganisationUnit() == organisationUnit &&
                !config.getTrustNewPublications() &&
                config.getTrustNewDocumentFiles()
        ));
    }

    @Test
    public void shouldUpdateExistingTrustConfiguration() {
        // Given
        int organisationUnitId = 4;
        var dto = new OrganisationUnitTrustConfigurationDTO(true, false);
        var existingConfig = new OrganisationUnitTrustConfiguration();
        existingConfig.setTrustNewDocumentFiles(true);
        existingConfig.setTrustNewPublications(false);

        when(configurationRepository.findConfigurationForOrganisationUnit(organisationUnitId))
            .thenReturn(Optional.of(existingConfig));

        // When
        var result = service.saveConfiguration(dto, organisationUnitId);

        // Then
        assertEquals(dto, result);
        verify(configurationRepository).save(argThat(config ->
            config.getTrustNewPublications() &&
                !config.getTrustNewDocumentFiles()
        ));
    }

    @Test
    public void shouldApproveMetadataAndIndexWhenFilesValid() {
        // Given
        var document = new Software();
        document.setId(1);
        document.setAreFilesValid(true);
        document.setIsMetadataValid(false);

        var index = new DocumentPublicationIndex();

        when(documentRepository.findById(1)).thenReturn(Optional.of(document));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            1)).thenReturn(Optional.of(index));

        // When
        service.approvePublicationMetadata(1);

        // Then
        assertTrue(document.getIsMetadataValid());
        assertEquals(ApproveStatus.APPROVED, document.getApproveStatus());
        assertTrue(index.getIsApproved());

        verify(documentRepository).save(document);
        verify(documentPublicationIndexRepository).save(index);
    }

    @Test
    public void shouldApproveMetadataOnlyWhenFilesNotValid() {
        // Given
        var document = new Thesis();
        document.setId(2);
        document.setAreFilesValid(false);

        when(documentRepository.findById(2)).thenReturn(Optional.of(document));

        // When
        service.approvePublicationMetadata(2);

        // Then
        assertTrue(document.getIsMetadataValid());
        assertEquals(ApproveStatus.APPROVED, document.getApproveStatus());
        verify(documentRepository).save(document);
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    public void shouldApproveFilesAndIndexWhenMetadataValid() {
        // Given
        var document = new Dataset();
        document.setId(3);
        document.setIsMetadataValid(true);
        document.setAreFilesValid(false);

        var index = new DocumentPublicationIndex();

        when(documentRepository.findById(3)).thenReturn(Optional.of(document));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            3)).thenReturn(Optional.of(index));

        // When
        service.approvePublicationUploadedDocuments(3);

        // Then
        assertTrue(document.getAreFilesValid());

        verify(documentRepository).save(document);
        verify(documentPublicationIndexRepository, never()).save(index);
    }

    @Test
    public void shouldApproveFilesOnlyWhenMetadataNotValid() {
        // Given
        var document = new JournalPublication();
        document.setId(4);
        document.setIsMetadataValid(false);

        when(documentRepository.findById(4)).thenReturn(Optional.of(document));

        // When
        service.approvePublicationUploadedDocuments(4);

        // Then
        assertTrue(document.getAreFilesValid());
        assertNull(document.getApproveStatus());

        verify(documentRepository).save(document);
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    public void shouldReturnValidationStatusWhenDocumentExists() {
        // Given
        var documentId = 1;
        var document = new Thesis();
        document.setIsMetadataValid(true);
        document.setAreFilesValid(false);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        // When
        var result = service.fetchValidationStatusForDocument(documentId);

        // Then
        assertEquals(true, result.a);
        assertEquals(false, result.b);
    }

    @Test
    public void shouldReturnFalseFalseWhenDocumentDoesNotExist() {
        // Given
        var documentId = 99;

        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When
        var result = service.fetchValidationStatusForDocument(documentId);

        // Then
        assertEquals(false, result.a);
        assertEquals(false, result.b);
    }
}
