package rs.teslaris.core.importer.converter.institution;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTORequest;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.importer.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.organisationunit.OrgUnit;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;

@Component
@RequiredArgsConstructor
public class OrganisationUnitConverter
    implements RecordConverter<OrgUnit, OrganisationUnitDTORequest> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final OrganisationUnitService organisationUnitService;


    public OrganisationUnitDTORequest toDTO(OrgUnit organisationUnit) {
        var dto = new OrganisationUnitDTORequest();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(organisationUnit.getId()));

        dto.setName(multilingualContentConverter.toDTO(organisationUnit.getMultilingualContent()));

        dto.setNameAbbreviation("");

        dto.setKeyword(new ArrayList<>());
        dto.setResearchAreasId(new ArrayList<>());

        dto.setLocation(new GeoLocationDTO());
        dto.setContact(new ContactDTO());

        return dto;
    }

    public Optional<OrganisationUnitsRelationDTO> toRelationDTO(OrgUnit sourceOU) {
        if (!Objects.nonNull(sourceOU.getPartOf())) {
            return Optional.empty();
        }

        var dto = new OrganisationUnitsRelationDTO();
        dto.setRelationType(OrganisationUnitRelationType.BELONGS_TO);

        var source = organisationUnitService.findOrganisationUnitByOldId(
            OAIPMHParseUtility.parseBISISID(sourceOU.getId()));
        var target = organisationUnitService.findOrganisationUnitByOldId(
            OAIPMHParseUtility.parseBISISID(sourceOU.getPartOf().getOrgUnit().getId()));

        if (!Objects.nonNull(target)) {
            return Optional.empty();
        }

        dto.setSourceOrganisationUnitId(source.getId());
        dto.setTargetOrganisationUnitId(target.getId());

        dto.setSourceAffiliationStatement(new ArrayList<>());
        dto.setTargetAffiliationStatement(new ArrayList<>());
        dto.setProofs(new ArrayList<>());

        return Optional.of(dto);
    }
}
