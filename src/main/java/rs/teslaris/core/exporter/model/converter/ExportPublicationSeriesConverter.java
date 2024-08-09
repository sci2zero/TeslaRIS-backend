package rs.teslaris.core.exporter.model.converter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import rs.teslaris.core.exporter.model.common.ExportContribution;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.document.PublicationSeriesContributionType;

public class ExportPublicationSeriesConverter extends ExportConverterBase {

    public static ExportDocument toCommonExportModel(
        PublicationSeries publicationSeries) {
        var commonExportPublicationSeries = new ExportDocument();

        setBaseFields(commonExportPublicationSeries, publicationSeries);
        if (commonExportPublicationSeries.getDeleted()) {
            return commonExportPublicationSeries;
        }

        commonExportPublicationSeries.setTitle(
            ExportMultilingualContentConverter.toCommonExportModel(publicationSeries.getTitle()));
        commonExportPublicationSeries.setNameAbbreviation(
            ExportMultilingualContentConverter.toCommonExportModel(
                publicationSeries.getNameAbbreviation()));
        commonExportPublicationSeries.setEIssn(publicationSeries.getEISSN());
        commonExportPublicationSeries.setPrintIssn(publicationSeries.getPrintISSN());
        commonExportPublicationSeries.setOldId(publicationSeries.getOldId());

        publicationSeries.getContributions().forEach(contribution -> {
            if (contribution.getContributionType()
                .equals(PublicationSeriesContributionType.EDITOR)) {
                var exportContribution = new ExportContribution();
                exportContribution.setDisplayName(
                    contribution.getAffiliationStatement().getDisplayPersonName().toString());
                if (Objects.nonNull(contribution.getPerson())) {
                    exportContribution.setPerson(
                        ExportPersonConverter.toCommonExportModel(contribution.getPerson()));
                }
                commonExportPublicationSeries.getEditors().add(exportContribution);
            }

        });

        publicationSeries.getLanguages().forEach(languageTag -> {
            commonExportPublicationSeries.getLanguageTags().add(languageTag.getLanguageTag());
        });

        commonExportPublicationSeries.getRelatedInstitutionIds()
            .addAll(getRelatedInstitutions(publicationSeries, false));
        commonExportPublicationSeries.getActivelyRelatedInstitutionIds()
            .addAll(getRelatedInstitutions(publicationSeries, true));
        return commonExportPublicationSeries;
    }

    private static Set<Integer> getRelatedInstitutions(PublicationSeries publicationSeries,
                                                       boolean onlyActive) {
        var relations = new HashSet<Integer>();
        publicationSeries.getContributions().forEach(contribution -> {
            if (Objects.nonNull(contribution.getPerson())) {
                relations.addAll(ExportPersonConverter.getRelatedInstitutions(
                    contribution.getPerson(), onlyActive));
            }
        });
        return relations;
    }
}
