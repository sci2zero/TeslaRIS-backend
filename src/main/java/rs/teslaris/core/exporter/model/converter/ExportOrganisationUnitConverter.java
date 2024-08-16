package rs.teslaris.core.exporter.model.converter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.core.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.core.importer.model.oaipmh.dublincore.DC;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.PartOf;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;

@Component
public class ExportOrganisationUnitConverter extends ExportConverterBase {

    private static OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    @Autowired
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
        commonExportOU.setOldId(organisationUnit.getOldId());

        var superOu = organisationUnitsRelationRepository.getSuperOU(organisationUnit.getId());
        superOu.ifPresent(organisationUnitsRelation -> commonExportOU.setSuperOU(
            ExportOrganisationUnitConverter.toCommonExportModel(
                organisationUnitsRelation.getTargetOrganisationUnit())));

        var topLevelIds = getTopLevelOrganisationUnitIds(organisationUnit.getId());
        commonExportOU.getRelatedInstitutionIds().addAll(topLevelIds);
        commonExportOU.getActivelyRelatedInstitutionIds().addAll(topLevelIds);
        return commonExportOU;
    }

    private static Set<Integer> getTopLevelOrganisationUnitIds(Integer organisationUnitId) {
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

    public static OrgUnit toOpenaireModel(ExportOrganisationUnit organisationUnit) {
        var orgUnit = new OrgUnit();
        orgUnit.setOldId("TESLARIS(" + organisationUnit.getDatabaseId() + ")");
        orgUnit.setName(
            ExportMultilingualContentConverter.toOpenaireModel(organisationUnit.getName()));
        if (Objects.nonNull(organisationUnit.getSuperOU())) {
            orgUnit.setPartOf(new PartOf(
                ExportOrganisationUnitConverter.toOpenaireModel(organisationUnit.getSuperOU())));
        }

        return orgUnit;
    }

    public static DC toDCModel(ExportOrganisationUnit exportOrganisationUnit) {
        var dcOrgUnit = new DC();
        dcOrgUnit.getType().add("party");
        dcOrgUnit.getSource().add(repositoryName);
        dcOrgUnit.getIdentifier().add("TESLARIS(" + exportOrganisationUnit.getDatabaseId() + ")");
        dcOrgUnit.getIdentifier().add(exportOrganisationUnit.getScopusAfid());

        clientLanguages.forEach(lang -> {
            dcOrgUnit.getIdentifier()
                .add(baseFrontendUrl + lang + "/organisation-units/" +
                    exportOrganisationUnit.getDatabaseId());
        });

        addContentToList(
            exportOrganisationUnit.getName(),
            ExportMultilingualContent::getContent,
            content -> dcOrgUnit.getTitle().add(content)
        );

        if (Objects.nonNull(exportOrganisationUnit.getNameAbbreviation()) &&
            !exportOrganisationUnit.getNameAbbreviation().isBlank()) {
            dcOrgUnit.getTitle().add(exportOrganisationUnit.getNameAbbreviation());
        }

        return dcOrgUnit;
    }
}
