package rs.teslaris.core.exporter.model.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;
import rs.teslaris.core.exporter.model.common.BaseExportEntity;
import rs.teslaris.core.exporter.model.common.ExportPublicationType;
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

    protected static String inferPublicationCOARType(ExportPublicationType type) {
        return switch (type) {
            case JOURNAL_PUBLICATION -> "http://purl.org/coar/resource_type/c_2df8fbb1";
            case PROCEEDINGS -> "http://purl.org/coar/resource_type/c_f744";
            case PROCEEDINGS_PUBLICATION -> "http://purl.org/coar/resource_type/c_5794";
            case MONOGRAPH -> "http://purl.org/coar/resource_type/c_2f33"; // book
            case PATENT -> "http://purl.org/coar/resource_type/c_15cd";
            case SOFTWARE -> "http://purl.org/coar/resource_type/c_5ce6";
            case DATASET -> "http://purl.org/coar/resource_type/c_ddb1";
            case JOURNAL -> "http://purl.org/coar/resource_type/c_0640";
            case MONOGRAPH_PUBLICATION -> "http://purl.org/coar/resource_type/c_3248"; // book part
        };
    }
}
