package rs.teslaris.exporter.model.converter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.document.PublicationSeriesContributionType;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.exporter.model.common.ExportContribution;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportPublicationType;

@Component
public class ExportPublicationSeriesConverter extends ExportConverterBase {

    private static JournalRepository journalRepository;

    @Autowired
    public ExportPublicationSeriesConverter(JournalRepository journalRepository) {
        ExportPublicationSeriesConverter.journalRepository = journalRepository;
    }

    public static ExportDocument toCommonExportModel(
        PublicationSeries publicationSeries, boolean computeRelations) {
        var commonExportPublicationSeries = new ExportDocument();
        commonExportPublicationSeries.setType(ExportPublicationType.JOURNAL);

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
        commonExportPublicationSeries.getOldIds().addAll(publicationSeries.getOldIds());

        publicationSeries.getContributions().forEach(contribution -> {
            if (contribution.getContributionType()
                .equals(PublicationSeriesContributionType.EDITOR)) {
                var exportContribution = new ExportContribution();
                exportContribution.setDisplayName(
                    contribution.getAffiliationStatement().getDisplayPersonName().toString());

                if (Objects.nonNull(contribution.getPerson())) {
                    exportContribution.setPerson(
                        ExportPersonConverter.toCommonExportModel(contribution.getPerson(), false));
                }

                commonExportPublicationSeries.getEditors().add(exportContribution);
            }

        });

        publicationSeries.getLanguages().forEach(languageTag -> {
            commonExportPublicationSeries.getLanguageTags().add(languageTag.getLanguageTag());
        });

        if (computeRelations) {
            var relations = getRelatedInstitutions(publicationSeries);
            commonExportPublicationSeries.getRelatedInstitutionIds().addAll(relations);
            commonExportPublicationSeries.getActivelyRelatedInstitutionIds().addAll(relations);
        }

        return commonExportPublicationSeries;
    }

    private static Set<Integer> getRelatedInstitutions(PublicationSeries publicationSeries) {
        var relations = new HashSet<Integer>();
        publicationSeries.getContributions().forEach(contribution -> {
            contribution.getInstitutions().forEach(institution -> {
                relations.add(institution.getId());
            });
        });

        relations.addAll(journalRepository.findInstitutionIdsByJournalIdAndAuthorContribution(
            publicationSeries.getId()));

        return relations;
    }
}
