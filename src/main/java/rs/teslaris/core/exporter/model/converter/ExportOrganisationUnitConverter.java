package rs.teslaris.core.exporter.model.converter;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import rs.teslaris.core.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;

@Component
public class ExportOrganisationUnitConverter extends ExportConverterBase {

    private static OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    public ExportOrganisationUnitConverter(
        OrganisationUnitsRelationRepository organisationUnitsRelationRepository) {
        ExportOrganisationUnitConverter.organisationUnitsRelationRepository =
            organisationUnitsRelationRepository;
    }

    public static ExportOrganisationUnit toCommonExportModel(OrganisationUnit organisationUnit) {
        var commonExportOU = new ExportOrganisationUnit();

        setBaseFields(commonExportOU, organisationUnit);
        if (commonExportOU.getDeleted()) {
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

        commonExportOU.getRelatedInstitutionIds()
            .addAll(getTopLevelOrganisationUnitId(organisationUnit.getId()));
        return commonExportOU;
    }

    private static Set<Integer> getTopLevelOrganisationUnitId(Integer organisationUnitId) {
        var relations = new HashSet<Integer>();
        Integer currentId;
        Optional<OrganisationUnitsRelation> superRelation = Optional.empty();

        do {
            currentId =
                superRelation.isPresent() ?
                    superRelation.get().getTargetOrganisationUnit().getId() : organisationUnitId;
            relations.add(currentId);
            superRelation = organisationUnitsRelationRepository.getSuperOU(currentId);
        } while (superRelation.isPresent());

        return relations;
    }
}
