package rs.teslaris.core.exporter.model.converter;

import java.util.ArrayList;
import java.util.List;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.importer.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.importer.model.oaipmh.product.Product;

public class ExportProductConverter extends ExportConverterBase {

    public static Product toOpenaireModel(
        ExportDocument exportDocument) {
        var openaireProduct = new Product();
        openaireProduct.setOldId("TESLARIS(" + exportDocument.getDatabaseId() + ")");
        openaireProduct.setName(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getTitle()));
        openaireProduct.setDescription(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getDescription()));

        openaireProduct.setType(inferPublicationCOARType(exportDocument.getType()));

        ExportMultilingualContentConverter.setFieldFromPriorityContent(
            exportDocument.getKeywords().stream(),
            content -> List.of(content.split("\n")),
            openaireProduct::setKeywords
        );

        if (!exportDocument.getLanguageTags().isEmpty()) {
            openaireProduct.setLanguage(exportDocument.getLanguageTags().getFirst());
        }

        openaireProduct.setUrl(exportDocument.getUris());
        openaireProduct.setAccess(
            exportDocument.getOpenAccess() ? "http://purl.org/coar/access_right/c_abf2" :
                "http://purl.org/coar/access_right/c_14cb");

        openaireProduct.setCreators(new ArrayList<>());
        exportDocument.getAuthors().forEach(contribution -> {
            openaireProduct.getCreators().add(new PersonAttributes(contribution.getDisplayName(),
                ExportPersonConverter.toOpenaireModel(contribution.getPerson())));
        });

        return openaireProduct;
    }
}
