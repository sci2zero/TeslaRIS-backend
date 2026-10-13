package rs.teslaris.core.service.impl.commontypes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
import rs.teslaris.core.dto.commontypes.IdentifierConfigurationDTO;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.commontypes.CrisContextInformation;
import rs.teslaris.core.model.commontypes.IdentifierConfiguration;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.repository.commontypes.CrisContextInformationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CrisContextInformationService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.persistence.IdentifierConfigurationCatalogue;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrisContextInformationServiceImpl extends JPAServiceImpl<CrisContextInformation>
    implements CrisContextInformationService {

    private static final String DEFAULT_NATIONAL_ID_PATTERN = ".*";

    private static final String ID_PLACEHOLDER = "{ID}";

    private static final Pattern URL_PATTERN =
        Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE);

    private static final License DEFAULT_METADATA_LICENSE = License.CC0;

    // Mirrors the placeholder default in application.properties.
    private static final String UNCONFIGURED_OAUTH2_CREDENTIAL = "NOT_CONFIGURED";

    private final CrisContextInformationRepository crisContextInformationRepository;

    private final MultilingualContentService multilingualContentService;

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
        validateNationalIdUrl(dto.personNationalIdUrl(), "person");
        validateNationalIdUrl(dto.organisationUnitNationalIdUrl(), "organisation unit");
        validateNationalIdUrl(dto.projectNationalIdUrl(), "project");
        validateNationalIdUrl(dto.documentNationalIdUrl(), "document");

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
        configuration.setIdentifierConfiguration(
            toIdentifierConfiguration(dto.identifierConfiguration()));
        configuration.setNationalIdentifierName(
            multilingualContentService.getMultilingualContent(dto.nationalIdentifierName()));
        configuration.setPersonNationalIdUrl(dto.personNationalIdUrl());
        configuration.setOrganisationUnitNationalIdUrl(dto.organisationUnitNationalIdUrl());
        configuration.setProjectNationalIdUrl(dto.projectNationalIdUrl());
        configuration.setDocumentNationalIdUrl(dto.documentNationalIdUrl());
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
            DEFAULT_NATIONAL_ID_PATTERN, DEFAULT_METADATA_LICENSE,
            CrisContextInformationConverter.toIdentifierConfigurationDTO(null),
            Collections.emptyList(), "", "", "", "", isOrcidLoginConfigured());
    }

    private Map<ApplicableEntityType, Map<String, IdentifierConfiguration>>
    toIdentifierConfiguration(
        Map<ApplicableEntityType, Map<String, IdentifierConfigurationDTO>> dto) {
        var configuration =
            new LinkedHashMap<ApplicableEntityType, Map<String, IdentifierConfiguration>>();

        if (Objects.isNull(dto)) {
            return configuration;
        }

        dto.forEach((entityType, perIdentifier) -> {
            if (Objects.isNull(perIdentifier)) {
                return;
            }

            var converted = new LinkedHashMap<String, IdentifierConfiguration>();

            perIdentifier.forEach((identifier, policy) -> {
                if (!IdentifierConfigurationCatalogue.isConfigurable(entityType, identifier)) {
                    throw new IllegalArgumentException(
                        "Identifier " + identifier + " is not configurable for " + entityType +
                            ".");
                }

                if (Objects.isNull(policy)) {
                    return;
                }

                converted.put(identifier, new IdentifierConfiguration(
                    Objects.requireNonNullElse(policy.hideWhenNotSet(), true),
                    Objects.requireNonNullElse(policy.featured(), false),
                    Objects.requireNonNullElse(policy.canBeUpdated(), true)));
            });

            configuration.put(entityType, converted);
        });

        return configuration;
    }

    private void validateNationalIdUrl(String url, String entityName) {
        if (Objects.isNull(url) || url.isBlank()) {
            return;
        }

        if (!url.contains(ID_PLACEHOLDER)) {
            throw new IllegalArgumentException(
                "Invalid " + entityName + " national ID URL: it has to contain the " +
                    ID_PLACEHOLDER + " placeholder.");
        }

        if (!URL_PATTERN.matcher(url.replace(ID_PLACEHOLDER, "1")).matches()) {
            throw new IllegalArgumentException(
                "Invalid " + entityName + " national ID URL: it has to be an HTTP or HTTPS URL.");
        }
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
