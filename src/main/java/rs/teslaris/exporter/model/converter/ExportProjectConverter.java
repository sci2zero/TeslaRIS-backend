package rs.teslaris.exporter.model.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import rs.teslaris.core.export.ProjectExportSource;
import rs.teslaris.core.model.commontypes.MonetaryAmount;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.exporter.model.common.ExportEntityIdentifier;
import rs.teslaris.exporter.model.common.ExportMonetaryAmount;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;
import rs.teslaris.exporter.model.common.ExportProject;
import rs.teslaris.exporter.model.common.ExportProjectOrganisationContribution;
import rs.teslaris.exporter.model.common.ExportProjectPersonContribution;

public class ExportProjectConverter extends ExportConverterBase {

    public static ExportProject toCommonExportModel(ProjectExportSource.ProjectRecord project,
                                                    boolean computeRelations) {
        var commonExportProject = new ExportProject();

        commonExportProject.setDatabaseId(project.id());
        commonExportProject.setLastUpdated(project.lastModification());
        commonExportProject.setDeleted(project.deleted());
        if (commonExportProject.getDeleted()) {
            return commonExportProject;
        }

        commonExportProject.getOldIds().addAll(numericOldIds(project.oldIds()));
        commonExportProject.setName(
            ExportMultilingualContentConverter.toCommonExportModel(project.name()));
        commonExportProject.setNameAbbreviation(
            ExportMultilingualContentConverter.toCommonExportModel(project.nameAbbreviation()));
        commonExportProject.setDescription(
            ExportMultilingualContentConverter.toCommonExportModel(project.description()));
        commonExportProject.setKeywords(
            ExportMultilingualContentConverter.toCommonExportModel(project.keywords()));
        commonExportProject.setDoi(project.doi());
        commonExportProject.setRaid(project.raid());
        commonExportProject.setNationalId(project.nationalId());
        commonExportProject.setIdentifiers(toIdentifiers(project.identifiers()));
        commonExportProject.setUris(toList(project.uris()));
        commonExportProject.setDateFrom(project.dateFrom());
        commonExportProject.setDateTo(project.dateTo());
        commonExportProject.setStatus(project.status());
        commonExportProject.setCollaborationType(project.collaborationType());
        commonExportProject.setResearchType(project.researchType());
        commonExportProject.setNotFunded(project.notFunded());
        commonExportProject.setCosts(toMonetaryAmount(project.costs()));

        project.persons().forEach(contribution -> commonExportProject.getPersons()
            .add(toPersonContribution(contribution)));
        project.organisations().forEach(contribution -> commonExportProject.getOrganisations()
            .add(toOrganisationContribution(contribution)));

        if (computeRelations) {
            commonExportProject.getRelatedInstitutionIds().addAll(project.institutionIds());
            commonExportProject.getActivelyRelatedInstitutionIds()
                .addAll(project.institutionIds());
        }

        return commonExportProject;
    }

    static Set<Integer> numericOldIds(Set<String> oldIds) {
        if (Objects.isNull(oldIds)) {
            return Set.of();
        }

        return oldIds.stream()
            .filter(oldId -> Objects.nonNull(oldId) && oldId.matches("\\d{1,9}"))
            .map(Integer::valueOf)
            .collect(Collectors.toSet());
    }

    static List<ExportEntityIdentifier> toIdentifiers(
        List<ProjectExportSource.IdentifierRecord> identifiers) {
        return new ArrayList<>(identifiers.stream()
            .map(identifier -> new ExportEntityIdentifier(identifier.code(), identifier.value(),
                identifier.urlTemplate()))
            .toList());
    }

    static ExportMonetaryAmount toMonetaryAmount(MonetaryAmount monetaryAmount) {
        if (Objects.isNull(monetaryAmount)) {
            return null;
        }

        return new ExportMonetaryAmount(monetaryAmount.getAmount(),
            Objects.nonNull(monetaryAmount.getCurrency()) ?
                monetaryAmount.getCurrency().getCode() : null);
    }

    static List<String> toList(Set<String> values) {
        return Objects.nonNull(values) ? new ArrayList<>(values) : new ArrayList<>();
    }

    private static ExportProjectPersonContribution toPersonContribution(
        ProjectExportSource.PersonContributionRecord contribution) {
        var exportContribution = new ExportProjectPersonContribution();

        exportContribution.setDisplayName(contribution.displayName());
        exportContribution.setPerson(toPerson(contribution.person()));
        if (Objects.nonNull(contribution.orderNumber())) {
            exportContribution.setOrderNumber(contribution.orderNumber());
        }
        exportContribution.setDeclaredContributions(contribution.institutionIds());
        exportContribution.setContributionType(contribution.contributionType());
        exportContribution.setInvestigationRole(contribution.investigationRole());
        exportContribution.setDateFrom(contribution.dateFrom());
        exportContribution.setDateTo(contribution.dateTo());

        return exportContribution;
    }

    private static ExportPerson toPerson(Person person) {
        if (Objects.isNull(person)) {
            return null;
        }

        var exportPerson = new ExportPerson();
        exportPerson.setDatabaseId(person.getId());
        exportPerson.setOldIds(person.getOldIds());
        exportPerson.setName(new ExportPersonName(person.getName().getFirstname(),
            person.getName().getOtherName(), person.getName().getLastname()));
        exportPerson.setOrcid(person.getOrcid());
        exportPerson.setScopusAuthorId(person.getScopusAuthorId());
        exportPerson.setOpenAlexId(person.getOpenAlexId());

        return exportPerson;
    }

    private static ExportProjectOrganisationContribution toOrganisationContribution(
        ProjectExportSource.OrganisationContributionRecord contribution) {
        var exportContribution = new ExportProjectOrganisationContribution();

        if (Objects.nonNull(contribution.organisationUnit())) {
            exportContribution.setOrganisationUnit(
                ExportOrganisationUnitConverter.toCommonExportModel(
                    contribution.organisationUnit(), false));
        }

        exportContribution.setDisplayName(
            ExportMultilingualContentConverter.toCommonExportModel(contribution.displayName()));
        exportContribution.setOrderNumber(contribution.orderNumber());
        exportContribution.setContributionType(contribution.contributionType());
        exportContribution.setDateFrom(contribution.dateFrom());
        exportContribution.setDateTo(contribution.dateTo());

        return exportContribution;
    }
}
