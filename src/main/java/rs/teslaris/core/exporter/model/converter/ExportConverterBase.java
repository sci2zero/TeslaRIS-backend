package rs.teslaris.core.exporter.model.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;
import rs.teslaris.core.exporter.model.common.BaseExportEntity;
import rs.teslaris.core.model.commontypes.BaseEntity;

public class ExportConverterBase {

    // Inherited classes should include
    // these static methods:
    // static T toCommonExportModel(D modelEntity)
    // static R toOpenaireModel(T commonExportEntity);

    protected static void setBaseFields(BaseExportEntity baseExportEntity, BaseEntity baseEntity) {
        baseExportEntity.setDatabaseId(baseEntity.getId());
        baseExportEntity.setLastUpdated(baseEntity.getLastModification());
        baseExportEntity.setDeleted(baseEntity.getDeleted());
    }

    protected static void setDocumentDate(String documentDate, Consumer<Date> setter) {
        if (Objects.nonNull(documentDate) && !documentDate.isBlank()) {
            SimpleDateFormat[] formatters = {
                new SimpleDateFormat("yyyy"),
                new SimpleDateFormat("yyyy-MM-dd"),
                new SimpleDateFormat("dd-MM-yyyy"),
                new SimpleDateFormat("dd/MM/yyyy"),
                new SimpleDateFormat("MM/dd/yyyy"),
                new SimpleDateFormat("dd.MM.yyyy"),
                new SimpleDateFormat("dd.MM.yyyy.")
            };

            for (var formatter : formatters) {
                try {
                    setter.accept(formatter.parse(documentDate));
                    break;
                } catch (ParseException e) {
                    // Parsing failed, try the next formatter
                }
            }
        }
    }
}
