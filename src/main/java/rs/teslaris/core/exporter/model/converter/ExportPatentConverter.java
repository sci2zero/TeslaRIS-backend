package rs.teslaris.core.exporter.model.converter;

import com.google.common.base.Functions;
import java.util.ArrayList;
import java.util.function.Function;
import rs.teslaris.core.exporter.model.common.ExportContribution;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.core.importer.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.importer.model.oaipmh.dublincore.DC;
import rs.teslaris.core.importer.model.oaipmh.patent.Patent;

public class ExportPatentConverter extends ExportConverterBase {

    public static Patent toOpenaireModel(ExportDocument exportDocument) {
        var openairePatent = new Patent();
        openairePatent.setOldId("TESLARIS(" + exportDocument.getDatabaseId() + ")");
        openairePatent.setTitle(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getTitle()));

        openairePatent.setType(inferPublicationCOARType(exportDocument.getType()));

        ExportMultilingualContentConverter.setFieldFromPriorityContent(
            exportDocument.getDescription().stream(),
            Function.identity(),
            openairePatent::set_abstract
        );

        setDocumentDate(exportDocument.getDocumentDate(), openairePatent::setApprovalDate);
        openairePatent.setPatentNumber(exportDocument.getNumber());
        openairePatent.setAccess(
            exportDocument.getOpenAccess() ? "http://purl.org/coar/access_right/c_abf2" :
                "http://purl.org/coar/access_right/c_14cb");

        openairePatent.setInventor(new ArrayList<>());
        exportDocument.getAuthors().forEach(contribution -> {
            openairePatent.getInventor().add(new PersonAttributes(contribution.getDisplayName(),
                ExportPersonConverter.toOpenaireModel(contribution.getPerson())));
        });

        return openairePatent;
    }

    public static DC toDCModel(ExportDocument exportDocument) {
        var dcPatent = new DC();
        dcPatent.getType().add("model");
        dcPatent.getSource().add(repositoryName);
        dcPatent.getIdentifier().add("TESLARIS(" + exportDocument.getDatabaseId() + ")");

        clientLanguages.forEach(lang -> {
            dcPatent.getIdentifier()
                .add(baseFrontendUrl + lang + "/scientific-results/patent/" +
                    exportDocument.getDatabaseId());
        });

        addContentToList(
            exportDocument.getTitle(),
            ExportMultilingualContent::getContent,
            content -> dcPatent.getTitle().add(content)
        );

        addContentToList(
            exportDocument.getAuthors(),
            ExportContribution::getDisplayName,
            content -> dcPatent.getCreator().add(content)
        );

        addContentToList(
            exportDocument.getDescription(),
            ExportMultilingualContent::getContent,
            content -> dcPatent.getDescription().add(content)
        );

        addContentToList(
            exportDocument.getKeywords(),
            ExportMultilingualContent::getContent,
            content -> dcPatent.getSubject().add(content.replace("\n", "; "))
        );

        addContentToList(
            exportDocument.getFileFormats(),
            Functions.identity(),
            content -> dcPatent.getFormat().add(content)
        );

        dcPatent.getRights().add(
            exportDocument.getOpenAccess() ? "info:eu-repo/semantics/openAccess" :
                "info:eu-repo/semantics/metadataOnlyAccess");
        dcPatent.getRights().add("http://creativecommons.org/publicdomain/zero/1.0/");

        return dcPatent;
    }
}
