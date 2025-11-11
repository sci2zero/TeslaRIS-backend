package rs.teslaris.exporter.model.converter;

import io.github.coordinates2country.Coordinates2Country;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.dublincore.DCMultilingualContent;
import rs.teslaris.core.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.model.oaipmh.organisationunit.PartOf;
import rs.teslaris.core.repository.institution.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;

@Component
public class ExportOrganisationUnitConverter extends ExportConverterBase {

    private static OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private static OrganisationUnitService organisationUnitService;

    @Autowired
    public ExportOrganisationUnitConverter(
        OrganisationUnitsRelationRepository organisationUnitsRelationRepository,
        OrganisationUnitService organisationUnitService) {
        ExportOrganisationUnitConverter.organisationUnitsRelationRepository =
            organisationUnitsRelationRepository;
        ExportOrganisationUnitConverter.organisationUnitService = organisationUnitService;
    }

    public static ExportOrganisationUnit toCommonExportModel(OrganisationUnit organisationUnit,
                                                             boolean computeRelations) {
        var commonExportOU = new ExportOrganisationUnit();

        setBaseFields(commonExportOU, organisationUnit);
        if (commonExportOU.getDeleted()) {
            return commonExportOU;
        }

        commonExportOU.setName(
            ExportMultilingualContentConverter.toCommonExportModel(organisationUnit.getName()));
        commonExportOU.setNameAbbreviation(organisationUnit.getNameAbbreviation());
        commonExportOU.setScopusAfid(organisationUnit.getScopusAfid());
        commonExportOU.setRor(organisationUnit.getRor());
        commonExportOU.setOpenAlex(organisationUnit.getOpenAlexId());
        commonExportOU.getOldIds().addAll(organisationUnit.getOldIds());

        if (Objects.nonNull(organisationUnit.getUris()) && !organisationUnit.getUris().isEmpty()) {
            commonExportOU.setUris(organisationUnit.getUris().stream().toList());
        }

        if (Objects.nonNull(organisationUnit.getLocation()) &&
            Objects.nonNull(organisationUnit.getLocation().getLatitude()) &&
            Objects.nonNull(organisationUnit.getLocation().getLongitude())) {
            commonExportOU.setCountry(getCountryCodeFromName(
                Coordinates2Country.country(organisationUnit.getLocation().getLatitude(),
                    organisationUnit.getLocation().getLongitude())));
        }

        var superOu = organisationUnitsRelationRepository.getSuperOU(organisationUnit.getId());
        superOu.ifPresent(organisationUnitsRelation -> commonExportOU.setSuperOU(
            ExportOrganisationUnitConverter.toCommonExportModel(
                organisationUnitsRelation.getTargetOrganisationUnit(), false)));

        if (computeRelations) {
            var topLevelIds = getOrganisationUnitRelationIds(organisationUnit.getId());
            commonExportOU.getRelatedInstitutionIds().addAll(topLevelIds);
            commonExportOU.getActivelyRelatedInstitutionIds().addAll(topLevelIds);
        }

        return commonExportOU;
    }

    private static Set<Integer> getOrganisationUnitRelationIds(Integer organisationUnitId) {
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

        relations.addAll(
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId));

        return relations;
    }

    public static OrgUnit toOpenaireModel(ExportOrganisationUnit organisationUnit,
                                          boolean supportLegacyIdentifiers) {
        var orgUnit = new OrgUnit();

        if (supportLegacyIdentifiers && Objects.nonNull(organisationUnit.getOldIds()) &&
            !organisationUnit.getOldIds().isEmpty()) {
            orgUnit.setOldId("Orgunits/" + legacyIdentifierPrefix +
                organisationUnit.getOldIds().stream().findFirst().get());
        } else {
            orgUnit.setOldId(
                "Orgunits/" + IdentifierUtil.identifierPrefix + organisationUnit.getDatabaseId());
        }

        orgUnit.setName(
            ExportMultilingualContentConverter.toOpenaireModel(organisationUnit.getName()));
        if (Objects.nonNull(organisationUnit.getSuperOU())) {
            orgUnit.setPartOf(new PartOf(
                ExportOrganisationUnitConverter.toOpenaireModel(organisationUnit.getSuperOU(),
                    supportLegacyIdentifiers)));
        }

        return orgUnit;
    }

    public static DC toDCModel(ExportOrganisationUnit exportOrganisationUnit,
                               boolean supportLegacyIdentifiers) {
        var dcOrgUnit = new DC();
        dcOrgUnit.getType().add("party");
        dcOrgUnit.getSource().add(repositoryName);

        if (supportLegacyIdentifiers && Objects.nonNull(exportOrganisationUnit.getOldIds()) &&
            !exportOrganisationUnit.getOldIds().isEmpty()) {
            dcOrgUnit.getIdentifier().add(legacyIdentifierPrefix +
                exportOrganisationUnit.getOldIds().stream().findFirst().get());
        } else {
            dcOrgUnit.getIdentifier()
                .add(identifierPrefix + exportOrganisationUnit.getDatabaseId());
        }

        if (StringUtil.valueExists(exportOrganisationUnit.getScopusAfid())) {
            dcOrgUnit.getIdentifier().add("SCOPUS:" + exportOrganisationUnit.getScopusAfid());
        }

        if (StringUtil.valueExists(exportOrganisationUnit.getRor())) {
            dcOrgUnit.getIdentifier().add("ROR:" + exportOrganisationUnit.getRor());
        }

        clientLanguages.forEach(lang -> {
            dcOrgUnit.getIdentifier()
                .add(baseFrontendUrl + lang + "/organisation-units/" +
                    exportOrganisationUnit.getDatabaseId());
        });

        addContentToList(
            exportOrganisationUnit.getName(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcOrgUnit.getTitle()
                .add(new DCMultilingualContent(content, languageTag))
        );

        if (Objects.nonNull(exportOrganisationUnit.getNameAbbreviation()) &&
            !exportOrganisationUnit.getNameAbbreviation().isBlank()) {
            dcOrgUnit.getTitle()
                .add(new DCMultilingualContent(exportOrganisationUnit.getNameAbbreviation(), null));
        }

        return dcOrgUnit;
    }

    public static String getCountryCodeFromName(String countryName) {
        var locales = Locale.getAvailableLocales();

        for (Locale locale : locales) {
            if (countryName.equalsIgnoreCase(locale.getDisplayCountry())) {
                return locale.getCountry(); // Returns ISO 3166-1 alpha-2 code (e.g., "US")
            }
        }
        return null;
    }
}
