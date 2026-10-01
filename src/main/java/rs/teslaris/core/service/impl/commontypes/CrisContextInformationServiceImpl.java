package rs.teslaris.core.service.impl.commontypes;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.CrisContextInformationConverter;
import rs.teslaris.core.dto.commontypes.CrisContextInformationDTO;
import rs.teslaris.core.model.commontypes.CrisContextInformation;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.repository.commontypes.CrisContextInformationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CrisContextInformationService;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrisContextInformationServiceImpl extends JPAServiceImpl<CrisContextInformation>
    implements CrisContextInformationService {

    private static final String DEFAULT_NATIONAL_ID_PATTERN = ".*";

    private static final License DEFAULT_METADATA_LICENSE = License.CC0;

    // Mirrors the placeholder default in application.properties.
    private static final String UNCONFIGURED_OAUTH2_CREDENTIAL = "NOT_CONFIGURED";

    private final CrisContextInformationRepository crisContextInformationRepository;

    @Value("${spring.security.oauth2.client.registration.orcid.client-id:}")
    private String orcidClientId;

    @Value("${spring.security.oauth2.client.registration.orcid.client-secret:}")
    private String orcidClientSecret;


    @Override
    protected JpaRepository<CrisContextInformation, Integer> getEntityRepository() {
        return crisContextInformationRepository;
    }

    @Override
    @Transactional
    public CrisContextInformationDTO readConfigurationForSystem() {
        return findConfiguration().map(
                configuration -> CrisContextInformationConverter.toDTO(configuration,
                    isOrcidLoginConfigured(), DEFAULT_METADATA_LICENSE, DEFAULT_NATIONAL_ID_PATTERN))
            .orElseGet(this::readDefaultConfiguration);
    }

    @Override
    @Transactional
    public CrisContextInformationDTO saveConfiguration(CrisContextInformationDTO dto) {
        validatePattern(dto.personNationalIdRegularExpression(), "person");
        validatePattern(dto.projectNationalIdRegularExpression(), "project");
        validatePattern(dto.organisationUnitNationalIdRegularExpression(), "organisation unit");
        validatePattern(dto.documentNationalIdRegularExpression(), "document");

        var configuration = findConfiguration().orElseGet(CrisContextInformation::new);

        configuration.setToggleAssessmentModule(dto.toggleAssessmentModule());
        configuration.setToggleDigitalLibrary(dto.toggleDigitalLibrary());
        configuration.setToggleDigitalRepository(dto.toggleDigitalRepository());
        configuration.setToggleRegistration(dto.toggleRegistration());
        configuration.setPersonNationalIdRegularExpression(
            dto.personNationalIdRegularExpression());
        configuration.setProjectNationalIdRegularExpression(
            dto.projectNationalIdRegularExpression());
        configuration.setOrganisationUnitNationalIdRegularExpression(
            dto.organisationUnitNationalIdRegularExpression());
        configuration.setDocumentNationalIdRegularExpression(
            dto.documentNationalIdRegularExpression());
        configuration.setMetadataLicense(dto.metadataLicense());
        save(configuration);

        return CrisContextInformationConverter.toDTO(configuration, isOrcidLoginConfigured(),
            DEFAULT_METADATA_LICENSE, DEFAULT_NATIONAL_ID_PATTERN);
    }

    private boolean isOrcidLoginConfigured() {
        return isCredentialConfigured(orcidClientId) && isCredentialConfigured(orcidClientSecret);
    }

    private boolean isCredentialConfigured(String credential) {
        return StringUtil.valueExists(credential) &&
            !UNCONFIGURED_OAUTH2_CREDENTIAL.equalsIgnoreCase(credential.trim());
    }

    private Optional<CrisContextInformation> findConfiguration() {
        var configurations = crisContextInformationRepository.findAll(Sort.by("id"));

        if (configurations.isEmpty()) {
            return Optional.empty();
        }

        if (configurations.size() > 1) {
            var redundantConfigurations = configurations.subList(1, configurations.size());
            log.warn("Found {} CRIS context information records, keeping the oldest (id={}) " +
                    "and deleting the rest.", configurations.size(),
                configurations.getFirst().getId());
            crisContextInformationRepository.deleteAll(redundantConfigurations);
        }

        return Optional.of(configurations.getFirst());
    }

    private CrisContextInformationDTO readDefaultConfiguration() {
        return new CrisContextInformationDTO(
            true, true, true, true,
            DEFAULT_NATIONAL_ID_PATTERN, DEFAULT_NATIONAL_ID_PATTERN, DEFAULT_NATIONAL_ID_PATTERN,
            DEFAULT_NATIONAL_ID_PATTERN, DEFAULT_METADATA_LICENSE, isOrcidLoginConfigured());
    }

    private void validatePattern(String pattern, String entityName) {
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(
                "Invalid " + entityName + " national ID pattern: " + e.getDescription());
        }
    }
}
