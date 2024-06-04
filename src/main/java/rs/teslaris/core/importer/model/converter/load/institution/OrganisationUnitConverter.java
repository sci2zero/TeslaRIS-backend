package rs.teslaris.core.importer.model.converter.load.institution;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitWizardDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;

@Component
@RequiredArgsConstructor
public class OrganisationUnitConverter
    implements RecordConverter<OrgUnit, OrganisationUnitRequestDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final OrganisationUnitService organisationUnitService;


    public OrganisationUnitWizardDTO toDTO(OrgUnit organisationUnit) {
        var dto = new OrganisationUnitWizardDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(organisationUnit.getOldId()));

        dto.setName(multilingualContentConverter.toDTO(organisationUnit.getMultilingualContent()));

        dto.setNameAbbreviation("");

        dto.setKeyword(new ArrayList<>());
        dto.setResearchAreasId(new ArrayList<>());

        dto.setLocation(new GeoLocationDTO());
        dto.setContact(new ContactDTO());

        if (Objects.nonNull(organisationUnit.getPartOf())) {
            dto.setSuperOrganisationUnitId(
                OAIPMHParseUtility.parseBISISID(
                    organisationUnit.getPartOf().getOrgUnit().getOldId()));
            dto.setSuperOrganisationUnitName(multilingualContentConverter.toDTO(
                organisationUnit.getPartOf().getOrgUnit().getMultilingualContent()));
        }
        return dto;
    }

    public Optional<OrganisationUnitsRelationDTO> toRelationDTO(OrgUnit sourceOU) {
        if (Objects.isNull(sourceOU.getPartOf())) {
            return Optional.empty();
        }

        var dto = new OrganisationUnitsRelationDTO();
        dto.setRelationType(OrganisationUnitRelationType.BELONGS_TO);

        var source = organisationUnitService.findOrganisationUnitByOldId(
            OAIPMHParseUtility.parseBISISID(sourceOU.getOldId()));
        var target = organisationUnitService.findOrganisationUnitByOldId(
            OAIPMHParseUtility.parseBISISID(sourceOU.getPartOf().getOrgUnit().getOldId()));

        if (Objects.isNull(target)) {
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
