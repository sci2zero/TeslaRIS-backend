package rs.teslaris.core.exporter.model.converter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import rs.teslaris.core.exporter.model.common.ExportEvent;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Event;

public class ExportEventConverter extends ExportConverterBase {

    public static ExportEvent toCommonExportModel(Conference event) {
        var commonExportEvent = new ExportEvent();

        setBaseFields(commonExportEvent, event);
        if (commonExportEvent.getDeleted()) {
            return commonExportEvent;
        }

        setCommonFields(commonExportEvent, event);
        commonExportEvent.setConfId(event.getConfId());

        return commonExportEvent;
    }

    public static ExportEvent toCommonExportModel(Event event) {
        var commonExportEvent = new ExportEvent();

        setBaseFields(commonExportEvent, event);
        if (commonExportEvent.getDeleted()) {
            return commonExportEvent;
        }

        setCommonFields(commonExportEvent, event);

        return commonExportEvent;
    }

    private static void setCommonFields(ExportEvent commonExportEvent, Event event) {
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

        commonExportEvent.getRelatedInstitutionIds().addAll(getRelatedInstitutions(event));
        commonExportEvent.setOldId(event.getOldId());
    }

    private static Set<Integer> getRelatedInstitutions(Event event) {
        var relations = new HashSet<Integer>();
        event.getContributions().forEach(contribution -> {
            if (Objects.nonNull(contribution.getPerson())) {
                relations.addAll(ExportPersonConverter.getRelatedEmploymentInstitutions(
                    contribution.getPerson()));
            }
        });
        return relations;
    }
}
