package rs.teslaris.exporter.model.converter;

import com.google.common.base.Functions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import rs.teslaris.core.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.product.Product;
import rs.teslaris.exporter.model.common.ExportContribution;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportPublicationType;

public class ExportProductConverter extends ExportConverterBase {

    public static Product toOpenaireModel(
        ExportDocument exportDocument, boolean supportLegacyIdentifiers) {
        var openaireProduct = new Product();

        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            openaireProduct.setOldId("Products/" + legacyIdentifierPrefix +
                exportDocument.getOldIds().stream().findFirst().get());
        } else {
            openaireProduct.setOldId("Products/(TESLARIS)" + exportDocument.getDatabaseId());
        }

        openaireProduct.setName(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getTitle()));
        openaireProduct.setDescription(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getDescription()));

        openaireProduct.setType(inferPublicationCOARType(exportDocument.getType()));

        openaireProduct.setKeywords(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getKeywords()));

        if (!exportDocument.getLanguageTags().isEmpty()) {
            openaireProduct.setLanguage(exportDocument.getLanguageTags().getFirst());
        }

        // Validator complains where there are more than 1 urls
        if (!exportDocument.getUris().isEmpty()) {
            openaireProduct.setUrl(List.of(exportDocument.getUris().getFirst()));
        }

        openaireProduct.setAccess(
            (Objects.nonNull(exportDocument.getOpenAccess()) && exportDocument.getOpenAccess()) ?
                "http://purl.org/coar/access_right/c_abf2" :
                "http://purl.org/coar/access_right/c_14cb");

        openaireProduct.setCreators(new ArrayList<>());
        exportDocument.getAuthors()
            .forEach(contribution -> {
                var personAttributes = new PersonAttributes();
                personAttributes.setDisplayName(contribution.getDisplayName());

                if (Objects.nonNull(contribution.getPerson())) {
                    personAttributes.setPerson(
                        ExportPersonConverter.toOpenaireModel(contribution.getPerson(),
                            supportLegacyIdentifiers));
                }

                openaireProduct.getCreators().add(personAttributes);
            });

        return openaireProduct;
    }

    public static DC toDCModel(ExportDocument exportDocument, boolean supportLegacyIdentifiers) {
        var dcProduct = new DC();
        dcProduct.getType().add(
            exportDocument.getType().equals(ExportPublicationType.DATASET) ? "dataset" :
                "software");
        dcProduct.getSource().add(repositoryName);

        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            dcProduct.getIdentifier().add(legacyIdentifierPrefix + "(" +
                exportDocument.getOldIds().stream().findFirst().get() + ")");
        } else {

            dcProduct.getIdentifier().add("TESLARIS(" + exportDocument.getDatabaseId() + ")");
        }

        clientLanguages.forEach(lang -> dcProduct.getIdentifier().add(
            baseFrontendUrl + lang + "/scientific-result/" +
                (exportDocument.getType().equals(ExportPublicationType.DATASET) ? "dataset" :
                    "software") + "/" + exportDocument.getDatabaseId()));

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
            (Objects.nonNull(exportDocument.getOpenAccess()) && exportDocument.getOpenAccess()) ?
                "info:eu-repo/semantics/openAccess" :
                "info:eu-repo/semantics/metadataOnlyAccess");
        dcProduct.getRights().add("http://creativecommons.org/publicdomain/zero/1.0/");

        return dcProduct;
    }
}