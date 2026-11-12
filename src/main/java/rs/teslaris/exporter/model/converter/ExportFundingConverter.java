package rs.teslaris.exporter.model.converter;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import rs.teslaris.core.export.ProjectExportSource;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.exporter.model.common.ExportFunding;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;

public class ExportFundingConverter extends ExportConverterBase {

    public static ExportFunding toCommonExportModel(ProjectExportSource.FundingRecord funding,
                                                    boolean computeRelations) {
        var commonExportFunding = new ExportFunding();

        commonExportFunding.setDatabaseId(funding.id());
        commonExportFunding.setLastUpdated(funding.lastModification());
        commonExportFunding.setDeleted(funding.deleted());
        if (commonExportFunding.getDeleted()) {
            return commonExportFunding;
        }

        commonExportFunding.getOldIds().addAll(
            ExportProjectConverter.numericOldIds(funding.oldIds()));
        commonExportFunding.setName(
            ExportMultilingualContentConverter.toCommonExportModel(funding.name()));
        commonExportFunding.setNameAbbreviation(
            ExportMultilingualContentConverter.toCommonExportModel(funding.nameAbbreviation()));
        commonExportFunding.setDescription(
            ExportMultilingualContentConverter.toCommonExportModel(funding.description()));
        commonExportFunding.setKeywords(
            ExportMultilingualContentConverter.toCommonExportModel(funding.keywords()));
        commonExportFunding.setDoi(funding.doi());
        commonExportFunding.setGrantAgreementId(funding.grantAgreementId());
        commonExportFunding.setIdentifiers(
            ExportProjectConverter.toIdentifiers(funding.identifiers()));
        commonExportFunding.setUris(ExportProjectConverter.toList(funding.uris()));

        if (Objects.nonNull(funding.project())) {
            commonExportFunding.setProject(
                ExportProjectConverter.toCommonExportModel(funding.project(), false));
        }

        if (Objects.nonNull(funding.funder())) {
            commonExportFunding.setFunder(
                ExportOrganisationUnitConverter.toCommonExportModel(funding.funder(), false));
        }

        commonExportFunding.setDisplayFunder(
            ExportMultilingualContentConverter.toCommonExportModel(funding.displayFunder()));
        commonExportFunding.setDisplayCall(
            ExportMultilingualContentConverter.toCommonExportModel(funding.displayCall()));
        commonExportFunding.setDisplayProgram(
            ExportMultilingualContentConverter.toCommonExportModel(funding.displayProgram()));
        commonExportFunding.setFundingCallName(toContent(funding.fundingCallName()));
        commonExportFunding.setFundingProgramName(toContent(funding.fundingProgramName()));

        if (Objects.nonNull(funding.fundingTypes())) {
            commonExportFunding.setFundingTypes(new HashSet<>(funding.fundingTypes()));
        }

        commonExportFunding.setAmount(ExportProjectConverter.toMonetaryAmount(funding.amount()));
        commonExportFunding.setCompetitive(funding.competitive());
        commonExportFunding.setRenewable(funding.renewable());
        commonExportFunding.setDateSubmitted(funding.dateSubmitted());
        commonExportFunding.setDateAwarded(funding.dateAwarded());
        commonExportFunding.setDateFrom(funding.dateFrom());
        commonExportFunding.setDateTo(funding.dateTo());
        commonExportFunding.setOaMandated(funding.oaMandated());
        commonExportFunding.setOaMandateUrl(funding.oaMandateUrl());
        commonExportFunding.setInternalInvestment(funding.internalInvestment());

        if (computeRelations) {
            commonExportFunding.getRelatedInstitutionIds().addAll(funding.institutionIds());
            commonExportFunding.getActivelyRelatedInstitutionIds()
                .addAll(funding.institutionIds());
        }

        return commonExportFunding;
    }

    private static List<ExportMultilingualContent> toContent(Set<MultiLingualContent> content) {
        return ExportMultilingualContentConverter.toCommonExportModel(
            Objects.nonNull(content) ? content : Set.of());
    }
}
