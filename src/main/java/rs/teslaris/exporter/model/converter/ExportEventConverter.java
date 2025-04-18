package rs.teslaris.exporter.model.converter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;

@Component
public class ExportEventConverter extends ExportConverterBase {

    private static EventRepository eventRepository;

    @Autowired
    public ExportEventConverter(
        EventRepository eventRepository) {
        ExportEventConverter.eventRepository = eventRepository;
    }

    public static ExportEvent toCommonExportModel(Conference event, boolean computeRelations) {
        var commonExportEvent = new ExportEvent();

        setBaseFields(commonExportEvent, event);
        if (commonExportEvent.getDeleted()) {
            return commonExportEvent;
        }

        setCommonFields(commonExportEvent, event, computeRelations);
        commonExportEvent.setConfId(event.getConfId());

        return commonExportEvent;
    }

    public static ExportEvent toCommonExportModel(Event event, boolean computeRelations) {
        var commonExportEvent = new ExportEvent();

        setBaseFields(commonExportEvent, event);
        if (commonExportEvent.getDeleted()) {
            return commonExportEvent;
        }

        setCommonFields(commonExportEvent, event, computeRelations);

        return commonExportEvent;
    }

    private static void setCommonFields(ExportEvent commonExportEvent, Event event,
                                        boolean computeRelations) {
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
        commonExportEvent.setPlace(
            ExportMultilingualContentConverter.toCommonExportModel(event.getPlace()));

        if (Objects.nonNull(event.getCountry())) {
            commonExportEvent.setState(
                ExportMultilingualContentConverter.toCommonExportModel(
                    event.getCountry().getName()));
        }

        if (computeRelations) {
            var relations = getRelatedInstitutions(event);
            commonExportEvent.getRelatedInstitutionIds().addAll(relations);
            commonExportEvent.getActivelyRelatedInstitutionIds().addAll(relations);
        }

        commonExportEvent.setOldId(event.getOldId());
    }

    private static Set<Integer> getRelatedInstitutions(Event event) {
        var relations = new HashSet<Integer>();
        event.getContributions().forEach(contribution -> {
            contribution.getInstitutions().forEach(institution -> {
                relations.add(institution.getId());
            });
        });
        relations.addAll(eventRepository.findInstitutionIdsByEventIdAndAuthorContribution(
            event.getId()));
        return relations;
    }

    public static rs.teslaris.core.model.oaipmh.event.Event toOpenaireModel(
        ExportEvent exportEvent) {
        var openaireEvent = new rs.teslaris.core.model.oaipmh.event.Event();
        openaireEvent.setOldId("Events/(TESLARIS)" + exportEvent.getDatabaseId());
        openaireEvent.setEventName(
            ExportMultilingualContentConverter.toOpenaireModel(exportEvent.getName()));

        ExportMultilingualContentConverter.setFieldFromPriorityContent(
            exportEvent.getPlace().stream(),
            Function.identity(),
            openaireEvent::setPlace
        );

        ExportMultilingualContentConverter.setFieldFromPriorityContent(
            exportEvent.getState().stream(),
            Function.identity(),
            openaireEvent::setCountry
        );

        exportEvent.getDescription().stream()
            .min(Comparator.comparingInt(ExportMultilingualContent::getPriority))
            .map(mc -> new MultilingualContent(mc.getLanguageTag(), mc.getContent())).ifPresent(
                openaireEvent::setDescription);

        openaireEvent.setKeywords(new ArrayList<>());
        exportEvent.getKeywords().forEach(mc -> {
            openaireEvent.getKeywords()
                .add(new MultilingualContent(mc.getLanguageTag(),
                    mc.getContent().replace("\n", ";")));
        });

        openaireEvent.setStartDate(DateTimeFormatter.ISO_LOCAL_DATE.format(
            exportEvent.getDateFrom().atStartOfDay(ZoneId.systemDefault()).toLocalDate()));
        openaireEvent.setEndDate(DateTimeFormatter.ISO_LOCAL_DATE.format(
            exportEvent.getDateTo().atStartOfDay(ZoneId.systemDefault()).toLocalDate()));


        return openaireEvent;
    }

    public static DC toDCModel(ExportEvent exportEvent) {
        var dcEvent = new DC();
        dcEvent.getSource().add(repositoryName);
        dcEvent.getType().add("event");
        dcEvent.getCoverage()
            .add(exportEvent.getDateFrom().toString() + "-" + exportEvent.getDateTo().toString());
        dcEvent.getIdentifier().add("TESLARIS(" + exportEvent.getDatabaseId() + ")");
        dcEvent.getIdentifier().add(exportEvent.getConfId());

        clientLanguages.forEach(lang -> {
            dcEvent.getIdentifier()
                .add(baseFrontendUrl + lang + "/events/conference/" + exportEvent.getDatabaseId());
        });

        addContentToList(
            exportEvent.getName(),
            ExportMultilingualContent::getContent,
            content -> dcEvent.getTitle().add(content)
        );

        addContentToList(
            exportEvent.getNameAbbreviation(),
            ExportMultilingualContent::getContent,
            content -> dcEvent.getTitle().add(content)
        );

        addContentToList(
            exportEvent.getDescription(),
            ExportMultilingualContent::getContent,
            content -> dcEvent.getDescription().add(content)
        );

        addContentToList(
            exportEvent.getKeywords(),
            keywordSet -> keywordSet.getContent().replace("\n", "; "),
            content -> dcEvent.getSubject().add(content)
        );

        addContentToList(
            exportEvent.getPlace(),
            ExportMultilingualContent::getContent,
            content -> dcEvent.getCoverage().add(content)
        );

        addContentToList(
            exportEvent.getState(),
            ExportMultilingualContent::getContent,
            content -> dcEvent.getCoverage().add(content)
        );

        return dcEvent;
    }
}
