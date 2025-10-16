package rs.teslaris.importer.model.converter.load.institution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitWizardDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@RequiredArgsConstructor
public class OrganisationUnitConverter
    implements RecordConverter<OrgUnit, OrganisationUnitRequestDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final OrganisationUnitService organisationUnitService;


    public OrganisationUnitWizardDTO toDTO(OrgUnit organisationUnit) {
        var dto = new OrganisationUnitWizardDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(organisationUnit.getOldId()));

        dto.setName(multilingualContentConverter.toDTO(organisationUnit.getName()));

        dto.setNameAbbreviation("");
        if (Objects.nonNull(organisationUnit.getAcronym()) &&
            !organisationUnit.getAcronym().isEmpty()) {
            dto.setNameAbbreviation(organisationUnit.getAcronym().getFirst().getValue());
        }

        if (Objects.nonNull(organisationUnit.getKeywords()) &&
            !organisationUnit.getKeywords().isEmpty()) {
            dto.setKeyword(multilingualContentConverter.toDTO(organisationUnit.getKeywords()));
        } else {
            dto.setKeyword(new ArrayList<>());
        }

        // TODO: How to set research areas from serbian names?
        dto.setResearchAreasId(new ArrayList<>());

        if (Objects.nonNull(organisationUnit.getIsInstitution()) &&
            organisationUnit.getIsInstitution()) {
            dto.setLegalEntity(true);
            dto.setAllowedThesisTypes(new HashSet<>(
                List.of(ThesisType.PHD, ThesisType.PHD_ART_PROJECT, ThesisType.MASTER,
                    ThesisType.MR, ThesisType.BACHELOR, ThesisType.BACHELOR_WITH_HONORS,
                    ThesisType.UNDERGRADUATE_THESIS)));
        }

        dto.setClientInstitutionCris(
            false); // explicitly set every single one to false, CRIS clients will be updated manually

        dto.setLocation(new GeoLocationDTO());

        if (Objects.nonNull(organisationUnit.getPlace()) &&
            !organisationUnit.getPlace().isBlank()) {
            dto.getLocation().setAddress(organisationUnit.getPlace());
        }

        dto.setUris(new HashSet<>());
        if (Objects.nonNull(organisationUnit.getIdentifier()) &&
            !organisationUnit.getIdentifier().isBlank()) {
            dto.getUris().add(organisationUnit.getIdentifier());
        }

        dto.setContact(new ContactDTO());

        if (Objects.nonNull(organisationUnit.getPartOf()) &&
            Objects.nonNull(organisationUnit.getPartOf().getOrgUnit())) {
            dto.setSuperOrganisationUnitId(
                OAIPMHParseUtility.parseBISISID(
                    organisationUnit.getPartOf().getOrgUnit().getOldId()));
            dto.setSuperOrganisationUnitName(multilingualContentConverter.toDTO(
                organisationUnit.getPartOf().getOrgUnit().getName()));
        }
        return dto;
    }

    public Optional<OrganisationUnitsRelationDTO> toRelationDTO(OrgUnit sourceOU) {
        if (Objects.isNull(sourceOU.getPartOf()) ||
            Objects.isNull(sourceOU.getPartOf().getOrgUnit())) {
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
