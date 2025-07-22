package rs.teslaris.core.service.impl.institution;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.institution.OrganisationUnitTrustConfigurationDTO;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.institution.OrganisationUnitTrustConfiguration;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitTrustConfigurationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.util.Pair;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationUnitTrustConfigurationServiceImpl
    extends JPAServiceImpl<OrganisationUnitTrustConfiguration> implements
    OrganisationUnitTrustConfigurationService {

    private final OrganisationUnitTrustConfigurationRepository
        organisationUnitTrustConfigurationRepository;

    private final OrganisationUnitService organisationUnitService;

    private final DocumentRepository documentRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final DocumentFileRepository documentFileRepository;


    @Override
    protected JpaRepository<OrganisationUnitTrustConfiguration, Integer> getEntityRepository() {
        return organisationUnitTrustConfigurationRepository;
    }

    @Override
    public OrganisationUnitTrustConfigurationDTO readTrustConfigurationForOrganisationUnit(
        Integer organisationUnitId) {
        var configuration =
            organisationUnitTrustConfigurationRepository.findConfigurationForOrganisationUnit(
                organisationUnitId);

        return configuration.map(
                organisationUnitTrustConfiguration -> new OrganisationUnitTrustConfigurationDTO(
                    organisationUnitTrustConfiguration.getTrustNewPublications(),
                    organisationUnitTrustConfiguration.getTrustNewDocumentFiles()))
            .orElseGet(() -> new OrganisationUnitTrustConfigurationDTO(true, false));
    }

    @Override
    public OrganisationUnitTrustConfigurationDTO saveConfiguration(
        OrganisationUnitTrustConfigurationDTO dto, Integer organisationUnitId) {
        var configuration =
            organisationUnitTrustConfigurationRepository.findConfigurationForOrganisationUnit(
                organisationUnitId).orElseGet(() -> {
                var config = new OrganisationUnitTrustConfiguration();
                config.setOrganisationUnit(
                    organisationUnitService.findOrganisationUnitById(organisationUnitId));
                return config;
            });


        configuration.setTrustNewPublications(dto.trustNewPublications());
        configuration.setTrustNewDocumentFiles(dto.trustNewDocumentFiles());
        save(configuration);

        return dto;
    }

    @Override
    public void approvePublicationMetadata(Integer documentId) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setIsMetadataValid(true);
            document.setApproveStatus(ApproveStatus.APPROVED);
            updateDocumentIndex(documentId);

            documentRepository.save(document);

        });
    }

    @Override
    public void approvePublicationUploadedDocuments(Integer documentId) {
        documentRepository.findById(documentId).ifPresent(document -> {

            document.getFileItems().forEach(file -> {
                file.setIsVerifiedData(true);
                documentFileRepository.save(file);
            });

            document.getProofs().forEach(file -> {
                file.setIsVerifiedData(true);
                documentFileRepository.save(file);
            });

            document.setAreFilesValid(true);
            documentRepository.save(document);
        });
    }

    @Override
    public Pair<Boolean, Boolean> fetchValidationStatusForDocument(Integer documentId) {
        var document = documentRepository.findById(documentId);

        return document.map(
                value -> new Pair<>(value.getIsMetadataValid(), value.getAreFilesValid()))
            .orElseGet(() -> new Pair<>(false, false));
    }

    private void updateDocumentIndex(Integer documentId) {
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            documentId).ifPresent(index -> {
            index.setIsApproved(true);
            documentPublicationIndexRepository.save(index);
        });
    }
}
