package rs.teslaris.core.exporter.model.converter;

import rs.teslaris.core.exporter.model.common.ExportEvent;
import rs.teslaris.core.model.document.Conference;

public class ExportEventConverter {

    public static ExportEvent toCommonExportModel(Conference event) {
        var commonExportEvent = new ExportEvent();
        commonExportEvent.setDatabaseId(event.getId());
        commonExportEvent.setLastUpdated(event.getLastModification());

        if (event.getDeleted()) {
            commonExportEvent.setDeleted(true);
            return commonExportEvent;
        }

        commonExportEvent.setName(
            ExportMultilingualContentConverter.toCommonExportModel(event.getName()));
        commonExportEvent.setNameAbbreviation(
            ExportMultilingualContentConverter.toCommonExportModel(event.getNameAbbreviation()));
        commonExportEvent.setDescription(
            ExportMultilingualContentConverter.toCommonExportModel(event.getDescription()));
        commonExportEvent.setKeywords(
            ExportMultilingualContentConverter.toCommonExportModel(event.getKeywords()));
        commonExportEvent.setSerialEvent(event.getSerialEvent());
        commonExportEvent.setDateFrom(event.getDateFrom());
        commonExportEvent.setDateTo(event.getDateTo());
        commonExportEvent.setState(
            ExportMultilingualContentConverter.toCommonExportModel(event.getState()));
        commonExportEvent.setPlace(
            ExportMultilingualContentConverter.toCommonExportModel(event.getPlace()));
        commonExportEvent.setConfId(event.getConfId());

        return commonExportEvent;
    }
}
