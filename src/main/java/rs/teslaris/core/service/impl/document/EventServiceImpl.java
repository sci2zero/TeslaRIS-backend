package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import jakarta.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl extends JPAServiceImpl<Event> implements EventService {

    protected final EventIndexRepository eventIndexRepository;

    protected final MultilingualContentService multilingualContentService;

    protected final PersonContributionService personContributionService;

    private final EventRepository eventRepository;

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

        event.setDateFrom(eventDTO.getDateFrom());
        event.setDateTo(eventDTO.getDateTo());
        event.setSerialEvent(eventDTO.getSerialEvent());
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
                                         EventType eventType) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens, eventType),
            pageable, EventIndex.class, "events");
    }

    @Override
    public Page<EventIndex> searchEventsImport(List<String> names, String dateFrom, String dateTo) {
        return searchService.runQuery(buildEventImportSearchQuery(names, dateFrom, dateTo),
            Pageable.ofSize(5), EventIndex.class, "events");
    }

    private Query buildEventImportSearchQuery(List<String> names, String dateFrom, String dateTo) {
        var namesQuery = names.stream()
            .flatMap(name -> Stream.of(
                    QueryBuilders.matchPhrase().field("name_sr").query(name).build()._toQuery(),
                    QueryBuilders.matchPhrase().field("name_other").query(name).build()._toQuery()
                )
            )
            .reduce((q1, q2) -> BoolQuery.of(b -> b.should(q1).should(q2))._toQuery())
            .orElse(null);

        var datesQuery = BoolQuery.of(b -> b
            .must(QueryBuilders.match().field("date_from_to").query(dateFrom).build()._toQuery())
            .must(QueryBuilders.match().field("date_from_to").query(dateTo).build()._toQuery())
        )._toQuery();

        var combinedQuery = BoolQuery.of(b -> b
            .must(namesQuery)
            .must(datesQuery)
        )._toQuery();

        var fallbackQuery = names.stream()
            .flatMap(name -> Stream.of(
                    QueryBuilders.matchPhrase().field("name_sr").query(name).build()._toQuery(),
                    QueryBuilders.matchPhrase().field("name_other").query(name).build()._toQuery()
                )
            )
            .reduce((q1, q2) -> BoolQuery.of(b -> b.should(q1).should(q2))._toQuery())
            .orElse(null);

        return BoolQuery.of(b -> b
            .should(combinedQuery)
            .should(fallbackQuery)
            .must(QueryBuilders.match().field("type").query(EventType.CONFERENCE.name()).build()
                ._toQuery())
        )._toQuery();
    }

    private Query buildSimpleSearchQuery(List<String> tokens, EventType eventType) {
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
            b.must(sb -> sb.match(
                m -> m.field("event_type").query(eventType.name())));
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

        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
        index.setDateFromTo(
            event.getDateFrom().format(formatter) + " - " + event.getDateTo().format(formatter));
        index.setDateSortable(event.getDateFrom());
    }

    private void indexMultilingualContent(EventIndex index, Event event,
                                          Function<Event, Set<MultiLingualContent>> contentExtractor,
                                          BiConsumer<EventIndex, String> srSetter,
                                          BiConsumer<EventIndex, String> otherSetter) {
        Set<MultiLingualContent> contentList = contentExtractor.apply(event);

        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();
        multilingualContentService.buildLanguageStrings(srContent, otherContent, contentList);

        StringUtil.removeTrailingPipeDelimiter(srContent, otherContent);
        srSetter.accept(index,
            srContent.length() > 0 ? srContent.toString() : otherContent.toString());
        otherSetter.accept(index,
            otherContent.length() > 0 ? otherContent.toString() : srContent.toString());
    }

    protected void notifyAboutBasicCreation(Integer eventId) {
        emailUtil.notifyInstitutionalEditor(eventId, "event");
    }
}
