package rs.teslaris.core.exporter.model.converter;

import java.util.ArrayList;
import java.util.List;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.importer.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.importer.model.oaipmh.product.Product;

public class ExportProductConverter extends ExportConverterBase {

    public static Product toOpenaireModel(
        ExportDocument exportDocument) {
        var openairePatent = new Product();
        openairePatent.setOldId("TESLARIS(" + exportDocument.getDatabaseId() + ")");
        openairePatent.setName(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getTitle()));
        openairePatent.setDescription(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getDescription()));

        ExportMultilingualContentConverter.setFieldFromPriorityContent(
            exportDocument.getKeywords().stream(),
            content -> List.of(content.split("\n")),
            openairePatent::setKeywords
        );

        if (!exportDocument.getLanguageTags().isEmpty()) {
            openairePatent.setLanguage(
                exportDocument.getLanguageTags().getFirst()); // is this ok?
        }

        openairePatent.setUrl(exportDocument.getUris());
        openairePatent.setAccess("OPEN"); // is this ok?

        openairePatent.setCreators(new ArrayList<>());
        exportDocument.getAuthors().forEach(contribution -> {
            openairePatent.getCreators().add(new PersonAttributes(contribution.getDisplayName(),
                ExportPersonConverter.toOpenaireModel(contribution.getPerson())));
        });

        return openairePatent;
    }
}
