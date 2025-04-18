package rs.teslaris.exporter.model.converter;

import com.google.common.base.Functions;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import rs.teslaris.core.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.patent.Patent;
import rs.teslaris.exporter.model.common.ExportContribution;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;

public class ExportPatentConverter extends ExportConverterBase {

    public static Patent toOpenaireModel(ExportDocument exportDocument) {
        var openairePatent = new Patent();
        openairePatent.setOldId("Patents/(TESLARIS)" + exportDocument.getDatabaseId() + ")");
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
            (Objects.nonNull(exportDocument.getOpenAccess()) && exportDocument.getOpenAccess()) ?
                "http://purl.org/coar/access_right/c_abf2" :
                "http://purl.org/coar/access_right/c_14cb");

        openairePatent.setInventor(new ArrayList<>());
        exportDocument.getAuthors()
            .forEach(contribution -> {
                var personAttributes = new PersonAttributes();
                personAttributes.setDisplayName(contribution.getDisplayName());

                if (Objects.nonNull(contribution.getPerson())) {
                    personAttributes.setPerson(
                        ExportPersonConverter.toOpenaireModel(contribution.getPerson()));
                }

                openairePatent.getInventor().add(personAttributes);
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
            (Objects.nonNull(exportDocument.getOpenAccess()) && exportDocument.getOpenAccess()) ?
                "info:eu-repo/semantics/openAccess" :
                "info:eu-repo/semantics/metadataOnlyAccess");
        dcPatent.getRights().add("http://creativecommons.org/publicdomain/zero/1.0/");

        return dcPatent;
    }
}
