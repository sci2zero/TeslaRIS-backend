package rs.teslaris.core.converter.commontypes;

import java.util.Objects;
import rs.teslaris.core.dto.commontypes.CrisContextInformationDTO;
import rs.teslaris.core.model.commontypes.CrisContextInformation;
import rs.teslaris.core.model.document.License;

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
            isOrcidLoginConfigured);
    }

    private static String patternOrDefault(String pattern, String defaultNationalIdPattern) {
        return (Objects.isNull(pattern) || pattern.isBlank()) ? defaultNationalIdPattern :
            pattern;
    }
}
