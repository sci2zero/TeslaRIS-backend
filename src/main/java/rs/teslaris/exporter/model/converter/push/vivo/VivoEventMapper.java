package rs.teslaris.exporter.model.converter.push.vivo;

import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.BIBO;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.EVENT;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.OBO;
import static rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.VIVO;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.model.document.EventsRelationType;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.RdfMapper;

@Component
@RequiredArgsConstructor
public class VivoEventMapper implements RdfMapper<ExportEvent> {

    private final VivoTeslarisLinks teslarisLinks;


    // A serial event in TeslaRIS is the series itself, not one occurrence.
    static String vivoClass(ExportEvent snapshot) {
        var eventType = snapshot.getEventType();

        if (Boolean.TRUE.equals(snapshot.getSerialEvent())) {
            return eventType == EventType.CONFERENCE ? VIVO + "ConferenceSeries" :
                VIVO + "EventSeries";
        }

        if (Objects.isNull(eventType)) {
            return EVENT + "Event";
        }

        return switch (eventType) {
            case CONFERENCE -> BIBO + "Conference";
            case EXHIBITION -> VIVO + "Exhibit";
            case COURSE -> VIVO + "Course";
            case OTHER_EVENT -> otherEventClass(snapshot);
        };
    }

    private static String otherEventClass(ExportEvent snapshot) {
        if (Objects.isNull(snapshot.getOtherEventType())) {
            return EVENT + "Event";
        }

        return switch (snapshot.getOtherEventType()) {
            case WEBINAR, LECTURE -> VIVO + "Presentation";
            case PERFORMANCE, PLAY -> BIBO + "Performance";
            case TRIAL -> BIBO + "Hearing";
            case CEREMONY, THESIS_DEFENCE -> EVENT + "Event";
        };
    }

    // The frontend has one landing page per event type.
    private static String frontendPath(EventType eventType) {
        if (Objects.isNull(eventType)) {
            return null;
        }

        return switch (eventType) {
            case CONFERENCE -> "events/conference";
            case EXHIBITION -> "events/exhibition";
            case COURSE -> "events/course";
            case OTHER_EVENT -> "events/other-event";
        };
    }

    @Override
    public String profile() {
        return VivoRdfSupport.PROFILE;
    }

    @Override
    public Class<ExportEvent> sourceType() {
        return ExportEvent.class;
    }

    @Override
    public Model toRdf(ExportEvent snapshot, PushTarget target) {
        var model = ModelFactory.createDefaultModel();
        var uri = VivoRdfSupport.eventUri(target, snapshot.getDatabaseId());
        var event = model.createResource(uri);

        event.addProperty(RDF.type, model.createResource(vivoClass(snapshot)));
        VivoRdfSupport.addLiterals(model, event, RDFS.label, snapshot.getName(), target);
        VivoRdfSupport.addLiterals(model, event, model.createProperty(VIVO + "abbreviation"),
            snapshot.getNameAbbreviation(), target);
        VivoRdfSupport.addLiterals(model, event, model.createProperty(VIVO + "description"),
            snapshot.getDescription(), target);
        VivoRdfSupport.addKeywords(model, event, snapshot.getKeywords(), target);

        VivoRdfSupport.addDateTimeInterval(model, event, uri, snapshot.getDateFrom(),
            snapshot.getDateTo());
        VivoRdfSupport.addCountry(model, event, snapshot.getCountryCode());
        addRelations(model, event, target, snapshot);

        // Old snapshots have no event type, so they get no TeslaRIS link.
        var path = frontendPath(snapshot.getEventType());
        if (Objects.nonNull(path)) {
            var vcard = VivoRdfSupport.addVcard(model, event, uri, "Kind");
            VivoRdfSupport.addUrls(model, vcard, uri,
                List.of(teslarisLinks.profileLink(path, snapshot.getDatabaseId())));
        }

        return model;
    }

    // VIVO shows BFO_0000050 / BFO_0000051 as "occurs within event" / "includes event" between
    // two events, and as "in event series" / "events in this series" when the target is an
    // EventSeries. The source owns both triples, because both contain its URI.
    private void addRelations(Model model, Resource event, PushTarget target,
                              ExportEvent snapshot) {
        if (Objects.isNull(snapshot.getRelations())) {
            return;
        }

        var partOf = model.createProperty(OBO + "BFO_0000050");
        var hasPart = model.createProperty(OBO + "BFO_0000051");

        snapshot.getRelations().stream()
            .filter(relation -> Objects.nonNull(relation.getTargetEventId()))
            // VIVO has no property for COLLOCATED_WITH.
            .filter(relation -> relation.getType() == EventsRelationType.PART_OF ||
                relation.getType() == EventsRelationType.BELONGS_TO_SERIES)
            .forEach(relation -> {
                var targetEvent = model.createResource(
                    VivoRdfSupport.eventUri(target, relation.getTargetEventId()));
                event.addProperty(partOf, targetEvent);
                targetEvent.addProperty(hasPart, event);
            });
    }
}
