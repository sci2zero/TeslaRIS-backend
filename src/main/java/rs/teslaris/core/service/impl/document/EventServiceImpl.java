package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import java.util.Objects;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl extends JPAServiceImpl<Event> implements EventService {

    private final EventRepository eventRepository;

    private final PersonContributionService personContributionService;

    private final MultilingualContentService multilingualContentService;

    protected final EventIndexRepository eventIndexRepository;


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
    protected JpaRepository<Event, Integer> getEntityRepository() {
        return eventRepository;
    }

    protected void indexEventCommonFields(EventIndex index, Event event) {
        event.getName().forEach((name) -> {
            if (name.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                index.setNameSr(name.getContent());
            } else {
                index.setNameOther(index.getNameOther() + name.getContent() + " | ");
            }
        });

        event.getDescription().forEach((description) -> {
            if (description.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                index.setDescriptionSr(description.getContent());
            } else {
                index.setDescriptionOther(
                    index.getDescriptionOther() + description.getContent() + " | ");
            }
        });

        event.getKeywords().forEach((keywords) -> {
            if (keywords.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                index.setKeywordsSr(keywords.getContent());
            } else {
                index.setKeywordsOther(index.getKeywordsOther() + keywords.getContent() + " | ");
            }
        });

        event.getState().forEach((state) -> {
            if (state.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                index.setStateSr(state.getContent());
            } else {
                index.setStateOther(index.getStateOther() + state.getContent() + " | ");
            }
        });

        event.getPlace().forEach((place) -> {
            if (place.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                index.setPlaceSr(place.getContent());
            } else {
                index.setPlaceOther(index.getPlaceOther() + place.getContent() + " | ");
            }
        });
    }
}
