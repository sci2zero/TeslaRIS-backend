package rs.teslaris.core.converter.commontypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import rs.teslaris.core.dto.commontypes.CrisContextInformationDTO;
import rs.teslaris.core.dto.commontypes.IdentifierConfigurationDTO;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.commontypes.CrisContextInformation;
import rs.teslaris.core.model.commontypes.IdentifierConfiguration;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.util.persistence.IdentifierConfigurationCatalogue;

public class CrisContextInformationConverter {

    public static CrisContextInformationDTO toDTO(CrisContextInformation configuration,
                                                  boolean isOrcidLoginConfigured,
                                                  License defaultMetadataLicense,
                                                  String defaultNationalIdPattern) {
        return new CrisContextInformationDTO(
            configuration.getToggleAssessmentModule(),
            configuration.getToggleDigitalLibrary(),
            configuration.getToggleDigitalRepository(),
            Objects.requireNonNullElse(configuration.getToggleRegistration(), false),
            patternOrDefault(configuration.getPersonNationalIdRegularExpression(),
                defaultNationalIdPattern),
            patternOrDefault(configuration.getProjectNationalIdRegularExpression(),
                defaultNationalIdPattern),
            patternOrDefault(configuration.getOrganisationUnitNationalIdRegularExpression(),
                defaultNationalIdPattern),
            patternOrDefault(configuration.getDocumentNationalIdRegularExpression(),
                defaultNationalIdPattern),
            Objects.requireNonNullElse(configuration.getMetadataLicense(),
                defaultMetadataLicense),
            toIdentifierConfigurationDTO(configuration.getIdentifierConfiguration()),
            MultilingualContentConverter.getMultilingualContentDTO(
                configuration.getNationalIdentifierName()),
            isOrcidLoginConfigured);
    }

    public static Map<ApplicableEntityType, Map<String, IdentifierConfigurationDTO>>
    toIdentifierConfigurationDTO(
        Map<ApplicableEntityType, Map<String, IdentifierConfiguration>> stored) {
        var result =
            new LinkedHashMap<ApplicableEntityType, Map<String, IdentifierConfigurationDTO>>();

        IdentifierConfigurationCatalogue.withDefaults(stored)
            .forEach((entityType, perIdentifier) -> {
                var converted = new LinkedHashMap<String, IdentifierConfigurationDTO>();

                perIdentifier.forEach((identifier, policy) -> converted.put(identifier,
                    new IdentifierConfigurationDTO(
                        Objects.requireNonNullElse(policy.getHideWhenNotSet(), true),
                        Objects.requireNonNullElse(policy.getFeatured(), false),
                        Objects.requireNonNullElse(policy.getCanBeUpdated(), true))));

                result.put(entityType, converted);
            });

        return result;
    }

    private static String patternOrDefault(String pattern, String defaultNationalIdPattern) {
        return (Objects.isNull(pattern) || pattern.isBlank()) ? defaultNationalIdPattern :
            pattern;
    }
}
