package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.document.EventsRelationConverter;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.EventsRelationDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.EventsRelation;
import rs.teslaris.core.model.document.EventsRelationType;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl extends JPAServiceImpl<Event> implements EventService {

    protected final EventIndexRepository eventIndexRepository;

    protected final MultilingualContentService multilingualContentService;

    protected final PersonContributionService personContributionService;

    protected final EventRepository eventRepository;

    private final EventsRelationRepository eventsRelationRepository;

    private final SearchService<EventIndex> searchService;

    private final EmailUtil emailUtil;


    @Override
    public Event findEventById(Integer eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with given ID does not exist."));
    }

    @Override
    @Nullable
    public Event findEventByOldId(Integer eventId) {
        return eventRepository.findEventByOldId(eventId).orElse(null);
    }

    @Override
    public void setEventCommonFields(Event event, EventDTO eventDTO) {
        event.setName(multilingualContentService.getMultilingualContent(eventDTO.getName()));
        event.setNameAbbreviation(
            multilingualContentService.getMultilingualContent(eventDTO.getNameAbbreviation()));
        event.setDescription(
            multilingualContentService.getMultilingualContent(eventDTO.getDescription()));
        event.setKeywords(
            multilingualContentService.getMultilingualContent(eventDTO.getKeywords()));
        event.setState(multilingualContentService.getMultilingualContent(eventDTO.getState()));
        event.setPlace(multilingualContentService.getMultilingualContent(eventDTO.getPlace()));

        event.setSerialEvent(
            Objects.nonNull(eventDTO.getSerialEvent()) ? eventDTO.getSerialEvent() : false);

        if (!event.getSerialEvent()) {
            if (Objects.isNull(eventDTO.getDateFrom()) || Objects.isNull(eventDTO.getDateTo())) {
                throw new MissingDataException(
                    "You have to provide start and end dates for a non-serial event.");
            }

            event.setDateFrom(eventDTO.getDateFrom());
            event.setDateTo(eventDTO.getDateTo());
        }

        event.setOldId(eventDTO.getOldId());

        if (Objects.nonNull(eventDTO.getContributions())) {
            personContributionService.setPersonEventContributionForEvent(event, eventDTO);
        }
    }

    @Override
    public void clearEventCommonFields(Event event) {
        event.getName().clear();
        event.getNameAbbreviation().clear();
        event.getState().clear();
        event.getPlace().clear();
        event.getDescription().clear();
        event.getKeywords().clear();

        event.getContributions().forEach(
            contribution -> personContributionService.deleteContribution(contribution.getId()));
        event.getContributions().clear();
    }

    protected void clearEventIndexCommonFields(EventIndex index) {
        index.setNameSr("");
        index.setNameOther("");
        index.setDescriptionSr("");
        index.setDescriptionOther("");
        index.setKeywordsSr("");
        index.setKeywordsOther("");
        index.setStateSr("");
        index.setStateOther("");
        index.setPlaceSr("");
        index.setPlaceOther("");
    }

    @Override
    public Boolean hasCommonUsage(Integer eventId) {
        return eventRepository.hasProceedings(eventId);
    }

    @Override
    public Page<EventIndex> searchEvents(List<String> tokens, Pageable pageable,
                                         EventType eventType, Boolean returnOnlyNonSerialEvents) {
        return searchService.runQuery(
            buildSimpleSearchQuery(tokens, eventType, returnOnlyNonSerialEvents),
            pageable, EventIndex.class, "events");
    }

    @Override
    public Page<EventIndex> searchEventsImport(List<String> names, String dateFrom, String dateTo) {
        return searchService.runQuery(buildEventImportSearchQuery(names, dateFrom, dateTo),
            Pageable.ofSize(5), EventIndex.class, "events");
    }

    @Override
    public List<EventsRelationDTO> readEventRelations(Integer eventId) {
        var event = findOne(eventId);

        if (event.getSerialEvent()) {
            throw new NotFoundException("One time event with this ID does not exist.");
        }

        var relations = new ArrayList<>(
            eventsRelationRepository.getRelationsForOneTimeEvent(eventId).stream()
                .map(EventsRelationConverter::toDTO).toList());

        var reverseRelations =
            eventsRelationRepository.getRelationsForEvent(eventId).stream()
                .map(EventsRelationConverter::toDTO).toList();

        relations.addAll(reverseRelations);

        return relations;
    }

    @Override
    public List<EventsRelationDTO> readSerialEventRelations(Integer serialEventId) {
        var serialEvent = findOne(serialEventId);

        if (!serialEvent.getSerialEvent()) {
            throw new NotFoundException("Serial event with this ID does not exist.");
        }

        return eventsRelationRepository.getRelationsForEvent(serialEventId).stream()
            .map(EventsRelationConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    public void addEventsRelation(EventsRelationDTO eventsRelationDTO) {
        if (eventsRelationDTO.getSourceId().equals(eventsRelationDTO.getTargetId())) {
            throw new SelfRelationException("selfRelationEventError");
        }

        if (eventsRelationRepository.relationExists(eventsRelationDTO.getSourceId(),
            eventsRelationDTO.getTargetId())) {
            throw new ConferenceReferenceConstraintViolationException(
                "relationAlreadyExistsError");
        }

        var sourceEvent = findOne(eventsRelationDTO.getSourceId());

        if (sourceEvent.getSerialEvent()) {
            throw new ConferenceReferenceConstraintViolationException(
                "Source event cannot be serial.");
        }

        var targetEvent = findOne(eventsRelationDTO.getTargetId());

        if (eventsRelationDTO.getEventsRelationType()
            .equals(EventsRelationType.BELONGS_TO_SERIES) && !targetEvent.getSerialEvent()) {
            throw new ConferenceReferenceConstraintViolationException(
                "targetEventNotSerialError");
        }

        var newRelation = new EventsRelation();
        newRelation.setSource(sourceEvent);
        newRelation.setTarget(targetEvent);
        newRelation.setEventsRelationType(eventsRelationDTO.getEventsRelationType());

        eventsRelationRepository.save(newRelation);
    }

    @Override
    public void deleteEventsRelation(Integer relationId) {
        var relationToDelete = eventsRelationRepository.findById(relationId);

        if (relationToDelete.isEmpty()) {
            throw new NotFoundException("Relation does not exist.");
        }

        eventsRelationRepository.delete(relationToDelete.get());
    }

    private Query buildEventImportSearchQuery(List<String> names, String dateFrom, String dateTo) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    names.forEach(name -> {
                        eq.should(sb -> sb.matchPhrase(m -> m.field("name_sr").query(name)));
                        eq.should(sb -> sb.matchPhrase(m -> m.field("name_other").query(name)));
                    });
                    eq.should(sb -> sb.wildcard(
                        m -> m.field("date_from_to").value(dateFrom)));
                    eq.should(sb -> sb.wildcard(
                        m -> m.field("date_from_to").value(dateTo)));
                    eq.should(sb -> sb.match(
                        m -> m.field("date_sortable").query(dateFrom)));
                    return eq;
                });
                return bq;
            });
            b.must(sb -> {
                sb.match(m -> m.field("event_type").query(EventType.CONFERENCE.name()));
                sb.match(m -> m.field("is_serial_event").query(false));
                return sb;
            });
            return b;
        })))._toQuery();
    }

    private Query buildSimpleSearchQuery(List<String> tokens, EventType eventType,
                                         Boolean returnOnlyNonSerialEvents) {
        var minShouldMatch = (int) Math.ceil(tokens.size() * 0.8);

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    tokens.forEach(token -> {
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("name_sr").value("*" + token + "*")
                                .caseInsensitive(true)));
                        eq.should(sb -> sb.match(
                            m -> m.field("name_sr").query(token)));
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("name_other").value("*" + token + "*")
                                .caseInsensitive(true)));
                        eq.should(sb -> sb.match(
                            m -> m.field("name_other").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("description_sr").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("description_other").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("keywords_sr").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("keywords_other").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("state_sr").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("state_other").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("place_sr").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("place_other").query(token)));
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("date_from_to").value(token)));
                    });
                    return eq.minimumShouldMatch(Integer.toString(minShouldMatch));
                });
                return bq;
            });
            b.must(sb -> {
                sb.match(m -> m.field("event_type").query(eventType.name()));

                if (returnOnlyNonSerialEvents) {
                    sb.match(m -> m.field("is_serial_event").query(false));
                }

                return sb;
            });
            return b;
        })))._toQuery();
    }

    @Override
    protected JpaRepository<Event, Integer> getEntityRepository() {
        return eventRepository;
    }

    protected void indexEventCommonFields(EventIndex index, Event event) {
        indexMultilingualContent(index, event, Event::getName, EventIndex::setNameSr,
            EventIndex::setNameOther);
        index.setNameSrSortable(index.getNameSr());
        index.setNameOtherSortable(index.getNameOther());
        indexMultilingualContent(index, event, Event::getDescription, EventIndex::setDescriptionSr,
            EventIndex::setDescriptionOther);
        indexMultilingualContent(index, event, Event::getKeywords, EventIndex::setKeywordsSr,
            EventIndex::setKeywordsOther);
        indexMultilingualContent(index, event, Event::getState, EventIndex::setStateSr,
            EventIndex::setStateOther);
        index.setStateSrSortable(index.getStateSr());
        index.setStateOtherSortable(index.getStateOther());
        indexMultilingualContent(index, event, Event::getPlace, EventIndex::setPlaceSr,
            EventIndex::setPlaceOther);

        if (Objects.nonNull(event.getDateFrom()) && Objects.nonNull(event.getDateTo())) {
            var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
            index.setDateFromTo(
                event.getDateFrom().format(formatter) + " - " +
                    event.getDateTo().format(formatter));
            index.setDateSortable(event.getDateFrom());
        } else {
            index.setDateSortable(LocalDate.of(1, 1, 1)); // lowest date ES will parse
        }

        index.setSerialEvent(event.getSerialEvent());
    }

    private void indexMultilingualContent(EventIndex index, Event event,
                                          Function<Event, Set<MultiLingualContent>> contentExtractor,
                                          BiConsumer<EventIndex, String> srSetter,
                                          BiConsumer<EventIndex, String> otherSetter) {
        Set<MultiLingualContent> contentList = contentExtractor.apply(event);

        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();
        multilingualContentService.buildLanguageStrings(srContent, otherContent, contentList, true);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        srSetter.accept(index,
            !srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        otherSetter.accept(index,
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
    }

    protected void notifyAboutBasicCreation(Integer eventId) {
        emailUtil.notifyInstitutionalEditor(eventId, "event");
    }
}
