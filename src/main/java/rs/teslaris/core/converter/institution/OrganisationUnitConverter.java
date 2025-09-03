package rs.teslaris.core.converter.institution;

import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import rs.teslaris.core.converter.commontypes.GeoLocationConverter;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;

public class OrganisationUnitConverter {

    public static OrganisationUnitDTO toDTO(OrganisationUnit organisationUnit) {
        var dto = new OrganisationUnitDTO();
        dto.setId(organisationUnit.getId());

        dto.setName(MultilingualContentConverter.getMultilingualContentDTO(
            organisationUnit.getName()));

        dto.setNameAbbreviation(organisationUnit.getNameAbbreviation());

        dto.setKeyword(MultilingualContentConverter.getMultilingualContentDTO(
            organisationUnit.getKeyword()));
        dto.setResearchAreas(
            organisationUnit.getResearchAreas().stream().map(ResearchAreaConverter::toDTO)
                .collect(Collectors.toList())
        );

        dto.setLocation(GeoLocationConverter.toDTO(organisationUnit.getLocation()));
        dto.setContact(ContactConverter.toDTO(organisationUnit.getContact()));
        dto.setScopusAfid(organisationUnit.getScopusAfid());
        dto.setOpenAlexId(organisationUnit.getOpenAlexId());
        dto.setRor(organisationUnit.getRor());
        dto.setUris(organisationUnit.getUris());
        dto.setAllowedThesisTypes(
            organisationUnit.getAllowedThesisTypes().stream().map(ThesisType::valueOf).toList());

        dto.setClientInstitution(organisationUnit.getIsClientInstitution());
        dto.setValidatingEmailDomain(organisationUnit.getValidateEmailDomain());
        dto.setAllowingSubdomains(organisationUnit.getAllowSubdomains());
        dto.setInstitutionEmailDomain(organisationUnit.getInstitutionEmailDomain());

        if (Objects.nonNull(organisationUnit.getLogo())) {
            dto.setLogoServerFilename(organisationUnit.getLogo().getImageServerName());
            dto.setLogoBackgroundHex(organisationUnit.getLogo().getBackgroundHex());
        }

        filterSensitiveData(dto);

        return dto;
    }

    private static void filterSensitiveData(OrganisationUnitDTO organisationUnitResponse) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (Objects.isNull(authentication) || !authentication.isAuthenticated() ||
            (authentication.getPrincipal() instanceof String &&
                authentication.getPrincipal().equals("anonymousUser"))) {
            organisationUnitResponse.getContact().setPhoneNumber("");
        }
    }
}
