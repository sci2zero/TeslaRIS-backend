package rs.teslaris.core.exporter.model.converter;

import org.springframework.stereotype.Component;
import rs.teslaris.core.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;

@Component
public class ExportOrganisationUnitConverter {

    private static OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    public ExportOrganisationUnitConverter(
        OrganisationUnitsRelationRepository organisationUnitsRelationRepository) {
        ExportOrganisationUnitConverter.organisationUnitsRelationRepository =
            organisationUnitsRelationRepository;
    }

    public static ExportOrganisationUnit toCommonExportModel(OrganisationUnit organisationUnit) {
        var commonExportOU = new ExportOrganisationUnit();
        commonExportOU.setDatabaseId(organisationUnit.getId());
        commonExportOU.setLastUpdated(organisationUnit.getLastModification());

        if (organisationUnit.getDeleted()) {
            commonExportOU.setDeleted(true);
            return commonExportOU;
        }

        commonExportOU.setName(
            ExportMultilingualContentConverter.toCommonExportModel(organisationUnit.getName()));
        commonExportOU.setNameAbbreviation(organisationUnit.getNameAbbreviation());
        commonExportOU.setScopusAfid(organisationUnit.getScopusAfid());

        var superOu = organisationUnitsRelationRepository.getSuperOU(organisationUnit.getId());
        superOu.ifPresent(organisationUnitsRelation -> commonExportOU.setSuperOU(
            ExportOrganisationUnitConverter.toCommonExportModel(
                organisationUnitsRelation.getTargetOrganisationUnit())));

        return commonExportOU;
    }
}
