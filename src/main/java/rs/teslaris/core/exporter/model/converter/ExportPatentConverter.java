package rs.teslaris.core.exporter.model.converter;

import java.util.ArrayList;
import java.util.function.Function;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.importer.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.importer.model.oaipmh.patent.Patent;

public class ExportPatentConverter extends ExportConverterBase {

    public static Patent toOpenaireModel(ExportDocument exportDocument) {
        var openairePatent = new Patent();
        openairePatent.setOldId("TESLARIS(" + exportDocument.getDatabaseId() + ")");
        openairePatent.setTitle(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getTitle()));

        ExportMultilingualContentConverter.setFieldFromPriorityContent(
            exportDocument.getDescription().stream(),
            Function.identity(),
            openairePatent::set_abstract
        );

        setDocumentDate(exportDocument.getDocumentDate(), openairePatent::setApprovalDate);
        openairePatent.setPatentNumber(exportDocument.getNumber());
        openairePatent.setAccess("OPEN"); // is this ok?

        openairePatent.setInventor(new ArrayList<>());
        exportDocument.getAuthors().forEach(contribution -> {
            openairePatent.getInventor().add(new PersonAttributes(contribution.getDisplayName(),
                ExportPersonConverter.toOpenaireModel(contribution.getPerson())));
        });

        return openairePatent;
    }
}
