package rs.teslaris.core.exporter.model.converter;

import com.google.common.base.Functions;
import java.util.ArrayList;
import java.util.List;
import rs.teslaris.core.exporter.model.common.ExportContribution;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.core.exporter.model.common.ExportPublicationType;
import rs.teslaris.core.importer.model.oaipmh.common.DC;
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

    public static DC toDCModel(ExportDocument exportDocument) {
        var dcProduct = new DC();
        dcProduct.getType().add(
            exportDocument.getType().equals(ExportPublicationType.DATASET) ? "dataset" :
                "software");
        dcProduct.getSource().add(repositoryName);
        dcProduct.getIdentifier().add("TESLARIS(" + exportDocument.getDatabaseId() + ")");

        clientLanguages.forEach(lang -> {
            dcProduct.getIdentifier()
                .add(baseFrontendUrl + lang + "/scientific-result/" +
                    (exportDocument.getType().equals(ExportPublicationType.DATASET) ? "dataset" :
                        "software") + "/" +
                    exportDocument.getDatabaseId());
        });

        addContentToList(
            exportDocument.getTitle(),
            ExportMultilingualContent::getContent,
            content -> dcProduct.getTitle().add(content)
        );

        addContentToList(
            exportDocument.getAuthors(),
            ExportContribution::getDisplayName,
            content -> dcProduct.getCreator().add(content)
        );

        addContentToList(
            exportDocument.getDescription(),
            ExportMultilingualContent::getContent,
            content -> dcProduct.getDescription().add(content)
        );

        addContentToList(
            exportDocument.getKeywords(),
            ExportMultilingualContent::getContent,
            content -> dcProduct.getSubject().add(content.replace("\n", "; "))
        );

        addContentToList(
            exportDocument.getFileFormats(),
            Functions.identity(),
            content -> dcProduct.getFormat().add(content)
        );

        dcProduct.getRights().add(
            exportDocument.getOpenAccess() ? "info:eu-repo/semantics/openAccess" :
                "info:eu-repo/semantics/metadataOnlyAccess");
        dcProduct.getRights().add("http://creativecommons.org/publicdomain/zero/1.0/");

        return dcProduct;
    }
}