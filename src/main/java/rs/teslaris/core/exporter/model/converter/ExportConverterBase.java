package rs.teslaris.core.exporter.model.converter;

import rs.teslaris.core.exporter.model.common.BaseExportEntity;
import rs.teslaris.core.model.commontypes.BaseEntity;

public class ExportConverterBase {

    protected static void setBaseFields(BaseExportEntity baseExportEntity, BaseEntity baseEntity) {
        baseExportEntity.setDatabaseId(baseEntity.getId());
        baseExportEntity.setLastUpdated(baseEntity.getLastModification());
        baseExportEntity.setDeleted(baseEntity.getDeleted());
    }
}
