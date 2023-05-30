package rs.teslaris.core.converter.institution;

import java.util.stream.Collectors;
import rs.teslaris.core.converter.commontypes.GeoLocationToGeoLocationDTO;
import rs.teslaris.core.converter.commontypes.MultilingualContentToMultilingualContentDTO;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.model.institution.OrganisationUnit;

public class OrganisationUnitConverter {
    public static OrganisationUnitDTO toDTO(OrganisationUnit organisationUnit) {
        var dto = new OrganisationUnitDTO();
        dto.setName(MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
            organisationUnit.getName()));

        dto.setNameAbbreviation(organisationUnit.getNameAbbreviation());

        dto.setKeyword(MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
            organisationUnit.getKeyword()));
        dto.setResearchAreas(
            organisationUnit.getResearchAreas().stream().map(ResearchAreaConverter::toDTO)
                .collect(Collectors.toList())
        );

        dto.setLocation(GeoLocationToGeoLocationDTO.toDTO(organisationUnit.getLocation()));
        dto.setApproveStatus(organisationUnit.getApproveStatus());
        dto.setContact(ContactConverter.toDTO(organisationUnit.getContact()));

        return dto;
    }
}
