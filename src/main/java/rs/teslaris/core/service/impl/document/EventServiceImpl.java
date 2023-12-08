package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
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
import rs.teslaris.core.util.language.LanguageAbbreviations;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl extends JPAServiceImpl<Event> implements EventService {

    protected final EventIndexRepository eventIndexRepository;

    protected final MultilingualContentService multilingualContentService;

    private final EventRepository eventRepository;

    private final PersonContributionService personContributionService;

    private final SearchService<EventIndex> searchService;

    private final EmailUtil emailUtil;


    @Override
    public Event findEventById(Integer eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with given ID does not exist."));
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

        if (Objects.nonNull(eventDTO.getContributions())) {
            personContributionService.setPersonEventContributionForEvent(event, eventDTO);
        } else {
            event.setContributions(new HashSet<>());
        }
    }

    @Override
    public void clearEventCommonFields(Event event) {
        event.getName().clear();
        event.getNameAbbreviation().clear();
        event.getState().clear();
        event.getPlace().clear();
        event.getContributions().clear();
        event.getDescription().clear();
        event.getKeywords().clear();
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

    private Query buildSimpleSearchQuery(List<String> tokens, EventType eventType) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    tokens.forEach(token -> {
                        eq.should(sb -> sb.match(
                            m -> m.field("name_sr").query(token)));
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
                    });
                    return eq;
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
        indexMultilingualContent(index, event, Event::getDescription, EventIndex::setDescriptionSr,
            EventIndex::setDescriptionOther);
        indexMultilingualContent(index, event, Event::getKeywords, EventIndex::setKeywordsSr,
            EventIndex::setKeywordsOther);
        indexMultilingualContent(index, event, Event::getState, EventIndex::setStateSr,
            EventIndex::setStateOther);
        indexMultilingualContent(index, event, Event::getPlace, EventIndex::setPlaceSr,
            EventIndex::setPlaceOther);
    }

    private void indexMultilingualContent(EventIndex index, Event event,
                                          Function<Event, Set<MultiLingualContent>> contentExtractor,
                                          BiConsumer<EventIndex, String> srSetter,
                                          BiConsumer<EventIndex, String> otherSetter) {
        Set<MultiLingualContent> contentList = contentExtractor.apply(event);

        var otherContent = new StringBuilder();
        contentList.forEach(content -> {
            if (content.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                srSetter.accept(index, content.getContent());
            } else {
                otherContent.append(content.getContent()).append(" | ");
            }
        });

        otherSetter.accept(index, otherContent.toString());
    }

    protected void notifyAboutBasicCreation(Integer eventId) {
        emailUtil.notifyInstitutionalEditor(eventId, "event");
    }
}
