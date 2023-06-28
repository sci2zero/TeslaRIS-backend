package rs.teslaris.core.converter.institution;

import java.util.stream.Collectors;
import rs.teslaris.core.converter.commontypes.GeoLocationConverter;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.model.institution.OrganisationUnit;

public class OrganisationUnitConverter {
    public static OrganisationUnitDTO toDTO(OrganisationUnit organisationUnit) {
        var dto = new OrganisationUnitDTO();
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
        dto.setApproveStatus(organisationUnit.getApproveStatus());
        dto.setContact(ContactConverter.toDTO(organisationUnit.getContact()));

        return dto;
    }
}
