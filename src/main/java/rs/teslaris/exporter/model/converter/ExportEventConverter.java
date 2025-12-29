package rs.teslaris.exporter.model.converter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.dublincore.DCMultilingualContent;
import rs.teslaris.core.model.oaipmh.dublincore.DCType;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;

@Component
public class ExportEventConverter extends ExportConverterBase {

    private static EventRepository eventRepository;

    private static EventIndexRepository eventIndexRepository;


    @Autowired
    public ExportEventConverter(
        EventRepository eventRepository,
        EventIndexRepository eventIndexRepository) {
        ExportEventConverter.eventRepository = eventRepository;
        ExportEventConverter.eventIndexRepository = eventIndexRepository;
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

        commonExportEvent.getOldIds().addAll(event.getOldIds());
    }

    private static Set<Integer> getRelatedInstitutions(Event event) {
        var relations = new HashSet<Integer>();

        eventIndexRepository.findByDatabaseId(event.getId())
            .ifPresent(eventIndex -> relations.addAll(eventIndex.getRelatedInstitutionIds()));

        return relations;
    }

    public static rs.teslaris.core.model.oaipmh.event.Event toOpenaireModel(
        ExportEvent exportEvent, boolean supportLegacyIdentifiers) {
        var openaireEvent = new rs.teslaris.core.model.oaipmh.event.Event();

        if (supportLegacyIdentifiers && Objects.nonNull(exportEvent.getOldIds()) &&
            !exportEvent.getOldIds().isEmpty()) {
            openaireEvent.setOldId("Events/" + legacyIdentifierPrefix +
                exportEvent.getOldIds().stream().findFirst().get());
        } else {
            openaireEvent.setOldId(
                "Events/" + IdentifierUtil.identifierPrefix + exportEvent.getDatabaseId());
        }

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

        // TODO: Is specification explicitly asking for one?
        exportEvent.getDescription().stream()
            .min(Comparator.comparingInt(ExportMultilingualContent::getPriority))
            .map(mc -> List.of(new MultilingualContent(mc.getLanguageTag(), mc.getContent())))
            .ifPresent(openaireEvent::setDescription);

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

    public static DC toDCModel(ExportEvent exportEvent, boolean supportLegacyIdentifiers,
                               List<String> supportedLanguages) {
        var dcEvent = new DC();
        dcEvent.getSource().add(repositoryName);
        dcEvent.getType().add(new DCType("EVENT", null, null));
        dcEvent.getCoverage()
            .add(exportEvent.getDateFrom().toString() + "-" + exportEvent.getDateTo().toString());

        if (supportLegacyIdentifiers && Objects.nonNull(exportEvent.getOldIds()) &&
            !exportEvent.getOldIds().isEmpty()) {
            dcEvent.getIdentifier().add(
                legacyIdentifierPrefix + exportEvent.getOldIds().stream().findFirst().get());
        }

        dcEvent.getIdentifier().add(identifierPrefix + exportEvent.getDatabaseId());

        if (StringUtil.valueExists(exportEvent.getConfId())) {
            dcEvent.getIdentifier().add("confid:" + exportEvent.getConfId());
        }

        CollectionOperations.getIntersection(clientLanguages, supportedLanguages).forEach(lang -> {
            dcEvent.getIdentifier()
                .add(baseFrontendUrl + lang + "/events/conference/" + exportEvent.getDatabaseId());
        });

        addContentToList(
            exportEvent.getName(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcEvent.getTitle()
                .add(new DCMultilingualContent(content, languageTag))
        );

        addContentToList(
            exportEvent.getNameAbbreviation(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcEvent.getTitle()
                .add(new DCMultilingualContent(content, languageTag))
        );

        addContentToList(
            exportEvent.getDescription(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcEvent.getDescription()
                .add(new DCMultilingualContent(content, languageTag))
        );

        addContentToList(
            exportEvent.getKeywords(),
            keywordSet -> keywordSet.getContent().replace("\n", "; "),
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcEvent.getSubject()
                .add(new DCMultilingualContent(content, languageTag))
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
