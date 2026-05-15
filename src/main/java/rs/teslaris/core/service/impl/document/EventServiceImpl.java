package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.Nullable;
import jakarta.validation.ValidationException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.EventsRelationConverter;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.EventsRelationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.EventsRelation;
import rs.teslaris.core.model.document.EventsRelationType;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Primary
@RequiredArgsConstructor
@Traceable
public class EventServiceImpl extends JPAServiceImpl<Event> implements EventService {

    protected final EventIndexRepository eventIndexRepository;

    protected final MultilingualContentService multilingualContentService;

    protected final PersonContributionService personContributionService;

    protected final EventRepository eventRepository;

    protected final IndexBulkUpdateService indexBulkUpdateService;

    protected final CommissionRepository commissionRepository;

    protected final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final EventsRelationRepository eventsRelationRepository;

    private final SearchService<EventIndex> searchService;

    private final CountryService countryService;

    private final OrganisationUnitService organisationUnitService;

    private final ResearchAreaService researchAreaService;


    @Override
    @Nullable
    @Transactional(readOnly = true)
    public Event findEventByOldId(Integer eventId) {
        return eventRepository.findEventByOldIdsContains(eventId).orElse(null);
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
        event.setPlace(multilingualContentService.getMultilingualContent(eventDTO.getPlace()));
        event.setDisplayOrganizer(
            multilingualContentService.getMultilingualContent(eventDTO.getDisplayOrganizer()));

        if (Objects.nonNull(eventDTO.getCountryId())) {
            event.setCountry(countryService.findOne(eventDTO.getCountryId()));
        }

        event.setSerialEvent(
            Objects.nonNull(eventDTO.getSerialEvent()) ? eventDTO.getSerialEvent() : false);

        if (!event.getSerialEvent()) {
            if (Objects.isNull(eventDTO.getDateFrom()) || Objects.isNull(eventDTO.getDateTo())) {
                throw new MissingDataException(
                    "You have to provide start and end dates for a non-serial event.");
            }

            if (eventDTO.getDateTo().isBefore(eventDTO.getDateFrom())) {
                throw new ValidationException("End date cannot be before start date.");
            }
            event.setDateFrom(eventDTO.getDateFrom());
            event.setDateTo(eventDTO.getDateTo());
        }

        if (Objects.nonNull(eventDTO.getOldId())) {
            event.getOldIds().add(eventDTO.getOldId());
        }

        IdentifierUtil.setUris(event.getUris(), eventDTO.getUris());

        if (Objects.nonNull(eventDTO.getContributions())) {
            personContributionService.setPersonEventContributionForEvent(event, eventDTO);
        }

        if (CollectionOperations.containsValues(eventDTO.getResearchAreasId())) {
            var researchAreas = researchAreaService.getResearchAreasByIds(
                eventDTO.getResearchAreasId().stream().toList());
            event.setResearchAreas(new HashSet<>(researchAreas));
        }
    }

    @Override
    public void clearEventCommonFields(Event event) {
        event.getName().clear();
        event.getNameAbbreviation().clear();
        event.getPlace().clear();
        event.getDescription().clear();
        event.getKeywords().clear();
        event.getResearchAreas().clear();
        event.setCountry(null);

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
    @Transactional(readOnly = true)
    public Boolean hasCommonUsage(Integer eventId) {
        return eventRepository.hasProceedings(eventId);
    }

    @Override
    public Page<EventIndex> searchEvents(List<String> tokens, Pageable pageable,
                                         List<EventType> eventTypes,
                                         Boolean returnOnlyNonSerialEvents,
                                         Boolean returnOnlySerialEvents,
                                         Integer commissionInstitutionId,
                                         Integer commissionId, Boolean emptyEventsOnly,
                                         Boolean noContributionEventsOnly) {
        return searchService.runQuery(
            buildSimpleSearchQuery(tokens, eventTypes, returnOnlyNonSerialEvents,
                returnOnlySerialEvents, commissionInstitutionId, commissionId,
                emptyEventsOnly, noContributionEventsOnly),
            pageable, EventIndex.class, "events");
    }

    @Override
    public Page<EventIndex> searchEventsImport(List<String> names, String dateFrom, String dateTo) {
        return searchService.runQuery(buildEventImportSearchQuery(
                names,
                Math.max(
                    Integer.parseInt(dateFrom.substring(0, 4)),
                    Integer.parseInt(dateTo.substring(0, 4))
                )
            ),
            Pageable.ofSize(5),
            EventIndex.class, "events"
        );
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<EventsRelationDTO> readSerialEventRelations(Integer serialEventId) {
        var serialEvent = findOne(serialEventId);

        if (!serialEvent.getSerialEvent()) {
            throw new NotFoundException("Serial event with this ID does not exist.");
        }

        return eventsRelationRepository.getRelationsForEvent(serialEventId).stream()
            .map(EventsRelationConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
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
    @Transactional
    public void deleteEventsRelation(Integer relationId) {
        var relationToDelete = eventsRelationRepository.findById(relationId);

        if (relationToDelete.isEmpty()) {
            throw new NotFoundException("Relation does not exist.");
        }

        eventsRelationRepository.delete(relationToDelete.get());
    }

    @Override
    public Pair<Long, Long> getEventCountsBelongingToInstitution(Integer institutionId) {
        return new Pair<>(eventIndexRepository.count(),
            eventIndexRepository.countByRelatedInstitutionIds(institutionId));
    }

    @Override
    public Pair<Long, Long> getClassifiedEventCountsForCommission(Integer institutionId,
                                                                  Integer commissionId) {
        return new Pair<>(eventIndexRepository.countByClassifiedBy(commissionId),
            eventIndexRepository.countByRelatedInstitutionIdsAndClassifiedBy(institutionId,
                commissionId));
    }

    @Override
    @Transactional
    public void enrichEventInformationFromExternalSource(Integer oldId, LocalDate startDate,
                                                         LocalDate endDate) {
        eventRepository.findEventByOldIdsContains(oldId).ifPresent(event -> {
            event.setDateFrom(startDate);
            event.setDateTo(endDate);

            save(event);
            eventIndexRepository.findByDatabaseId(event.getId()).ifPresent(index -> {
                setIndexDate(event, index);
                eventIndexRepository.save(index);
            });
        });
    }

    private Query buildEventImportSearchQuery(List<String> names, int publicationYear) {
        String from = (publicationYear - 1) + "-01-01";
        String to = publicationYear + "-12-31";

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {

            b.must(bq -> {
                bq.bool(eq -> {

                    names.forEach(name -> {
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("name_sr").query(name)
                        ));

                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("name_other").query(name)
                        ));
                    });

                    eq.minimumShouldMatch("1");

                    eq.must(sb -> sb.range(r -> r
                        .field("date_sortable")
                        .gte(JsonData.of(from))
                        .lte(JsonData.of(to))
                    ));

                    return eq;
                });

                return bq;
            });

            b.must(sb -> sb.match(
                m -> m.field("event_type")
                    .query(EventType.CONFERENCE.name())
            ));

            b.must(sb -> sb.match(
                m -> m.field("is_serial_event")
                    .query(false)
            ));

            return b;
        })))._toQuery();
    }

    private Query buildSimpleSearchQuery(List<String> tokens, List<EventType> eventTypes,
                                         Boolean returnOnlyNonSerialEvents,
                                         Boolean returnOnlySerialEvents,
                                         Integer commissionInstitutionId,
                                         Integer commissionId, Boolean emptyEventsOnly,
                                         Boolean noContributionEventsOnly) {
        boolean onlyYearTokens =
            tokens.stream().allMatch(token -> token.matches("\\d{4}"));

        // If only searching by years, disable minimum_should_match, otherwise set it
        var minShouldMatch = onlyYearTokens ? 1 : (int) Math.ceil(tokens.size() * 0.8);

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    tokens.forEach(token -> {
                        if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                            b.must(mp ->
                                mp.bool(m -> {
                                    {
                                        m.should(sb -> sb.matchPhrase(
                                            mq -> mq.field("name_sr")
                                                .query(token.replace("\\\"", ""))));
                                        m.should(sb -> sb.matchPhrase(
                                            mq -> mq.field("name_other")
                                                .query(token.replace("\\\"", ""))));
                                    }
                                    return m;
                                }));
                        } else if (token.endsWith("\\*")) {
                            var wildcard = token.replace("\\*", "").replace(".", "");
                            eq.should(mp -> mp.bool(m -> m
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("name_sr").value(
                                        StringUtil.performSimpleLatinPreprocessing(wildcard) +
                                            "*")))
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("name_other").value(wildcard + "*")))
                            ));
                        } else {
                            var wildcard = token + "*";
                            eq.should(mp -> mp.bool(m -> m
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("name_sr").value(
                                            StringUtil.performSimpleLatinPreprocessing(token) + "*")
                                        .caseInsensitive(true)))
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("name_other").value(wildcard)
                                        .caseInsensitive(true)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("name_sr").query(wildcard)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("name_other").query(wildcard)))
                            ));
                        }

                        eq.should(sb -> sb.match(
                            m -> m.field("description_sr").query(token).boost(0.7f)));
                        eq.should(sb -> sb.match(
                            m -> m.field("description_other").query(token).boost(0.7f)));
                        eq.should(sb -> sb.term(
                            m -> m.field("keywords_sr").value(token)));
                        eq.should(sb -> sb.term(
                            m -> m.field("keywords_other").value(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("state_sr").query(token).boost(0.7f)));
                        eq.should(sb -> sb.match(
                            m -> m.field("state_other").query(token).boost(0.7f)));
                        eq.should(sb -> sb.match(
                            m -> m.field("place_sr").query(token).boost(0.5f)));
                        eq.should(sb -> sb.match(
                            m -> m.field("place_other").query(token).boost(0.5f)));

                        eq.should(sb -> sb.wildcard(
                            m -> m.field("date_from_to").value("*" + token + "*")));
                    });
                    return eq.minimumShouldMatch(Integer.toString(minShouldMatch));
                });
                return bq;
            });

            if (CollectionOperations.containsValues(eventTypes)) {
                b.must(createTypeTermsQuery(eventTypes));
            }

            if (Boolean.TRUE.equals(returnOnlyNonSerialEvents)) {
                b.must(sb -> sb.term(m -> m
                    .field("is_serial_event")
                    .value(false)
                ));
            }

            if (Boolean.TRUE.equals(returnOnlySerialEvents)) {
                b.must(sb -> sb.term(m -> m
                    .field("is_serial_event")
                    .value(true)
                ));
            }

            if (Objects.nonNull(commissionInstitutionId) && commissionInstitutionId > 0) {
                b.must(sb -> sb.term(
                    m -> m.field("related_employment_institution_ids")
                        .value(commissionInstitutionId)));
            }

            if (Objects.nonNull(emptyEventsOnly) && emptyEventsOnly) {
                b.must(sb -> sb.match(m -> m.field("has_proceedings").query(false)));
            }

            if (Objects.nonNull(noContributionEventsOnly) && noContributionEventsOnly) {
                b.mustNot(mn -> mn.exists(e -> e.field("related_person_ids")));
            }

            if (Objects.nonNull(commissionId)) {
                b.mustNot(mnb -> {
                    mnb.term(m -> m.field("classified_by").value(commissionId));
                    return mnb;
                });
            }

            return b;
        })))._toQuery();
    }

    private Query createTypeTermsQuery(List<EventType> values) {
        return TermsQuery.of(t -> t
            .field("event_type")
            .terms(v -> v.value(values.stream()
                .map(EventType::name)
                .map(FieldValue::of)
                .toList()))
        )._toQuery();
    }

    @Override
    protected JpaRepository<Event, Integer> getEntityRepository() {
        return eventRepository;
    }

    protected void indexEventCommonFields(EventIndex index, Event event) {
        indexMultilingualContent(index, event, Event::getName, EventIndex::setNameSr,
            EventIndex::setNameOther, false);
        var abbreviationSr =
            StringUtil.getStringContent(event.getNameAbbreviation(), LanguageAbbreviations.SERBIAN);

        if (StringUtil.valueExists(abbreviationSr)) {
            index.setNameSr(index.getNameSr() + " (" + abbreviationSr + ")");
        }

        var abbreviationOther =
            StringUtil.getStringContent(event.getNameAbbreviation(), LanguageAbbreviations.ENGLISH);
        if (StringUtil.valueExists(abbreviationOther)) {
            index.setNameOther(index.getNameOther() + " (" + abbreviationOther + ")");
        }

        index.setNameSrSortable(index.getNameSr());
        index.setNameOtherSortable(index.getNameOther());

        indexMultilingualContent(index, event, Event::getDescription, EventIndex::setDescriptionSr,
            EventIndex::setDescriptionOther, true);
        indexMultilingualContent(index, event, Event::getKeywords, EventIndex::setKeywordsSr,
            EventIndex::setKeywordsOther, false);
        indexMultilingualContent(index, event, Event::getPlace, EventIndex::setPlaceSr,
            EventIndex::setPlaceOther, false);

        if (Objects.nonNull(event.getCountry())) {
            indexMultilingualContent(index, event, t -> event.getCountry().getName(),
                EventIndex::setStateSr,
                EventIndex::setStateOther, false);
            index.setStateSrSortable(index.getStateSr());
            index.setStateOtherSortable(index.getStateOther());
        }

        if (Objects.nonNull(event.getDateFrom()) && Objects.nonNull(event.getDateTo())) {
            setIndexDate(event, index);
        } else {
            index.setDateSortable(LocalDate.of(1, 1, 1)); // lowest date ES will parse
        }

        index.setSerialEvent(event.getSerialEvent());

        index.getRelatedPersonIds().clear();
        index.getRelatedPersonIds()
            .addAll(eventRepository.findPersonIdsByEventIdAndEventContribution(event.getId()));

        index.getRelatedInstitutionIds().clear();
        index.getRelatedInstitutionIds().addAll(
            eventRepository.findInstitutionIdsByEventIdAndEventContribution(event.getId())
        );

        indexActiveEmploymentRelations(index, event.getId());

        index.setClassifiedBy(
            commissionRepository.findCommissionsThatClassifiedEvent(event.getId()));
        index.setHasProceedings(Objects.nonNull(event.getId()) &&
            (documentPublicationIndexRepository.countByEventId(event.getId()) > 0));
    }

    @Override
    public void reindexProceedingsStatus(Integer eventId) {
        eventIndexRepository.findByDatabaseId(eventId).ifPresent(index -> {
            index.setHasProceedings(Objects.nonNull(eventId) &&
                (documentPublicationIndexRepository.countByEventId(eventId) > 0));
            eventIndexRepository.save(index);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public void indexActiveEmploymentRelations(EventIndex index, Integer eventId) {
        var shouldSave = false;
        if (Objects.isNull(index)) {
            var optionalIndex = eventIndexRepository.findByDatabaseId(eventId);
            if (optionalIndex.isEmpty()) {
                return;
            }

            index = optionalIndex.get();
            shouldSave = true;

            index.getRelatedInstitutionIds().clear();
            index.getRelatedInstitutionIds().addAll(
                eventRepository.findInstitutionIdsByEventIdAndEventContribution(eventId)
            );
        }

        if (index.getEventType().equals(EventType.CONFERENCE)) {
            index.getRelatedEmploymentInstitutionIds().clear();

            var relatedEmploymentInstitutionIds = new HashSet<Integer>();
            var relatedFirstOrderEmploymentInstitutionIds =
                eventRepository.findEmploymentInstitutionIdsByEventIdAndAuthorContribution(eventId);
            index.getRelatedInstitutionIds().addAll(relatedEmploymentInstitutionIds);

            relatedFirstOrderEmploymentInstitutionIds.forEach(institutionId -> {
                relatedEmploymentInstitutionIds.add(institutionId);
                relatedEmploymentInstitutionIds.addAll(
                    organisationUnitService.getSuperOUsHierarchyRecursive(institutionId));
            });

            index.getRelatedEmploymentInstitutionIds().addAll(
                relatedEmploymentInstitutionIds.stream().toList()
            );
        }

        if (shouldSave) {
            eventIndexRepository.save(index);
        }
    }

    @Override
    public void deleteIndexes() {
        eventIndexRepository.deleteAll();
    }

    private void indexMultilingualContent(EventIndex index, Event event,
                                          Function<Event, Set<MultiLingualContent>> contentExtractor,
                                          BiConsumer<EventIndex, String> srSetter,
                                          BiConsumer<EventIndex, String> otherSetter,
                                          boolean isHTML) {
        Set<MultiLingualContent> contentList = contentExtractor.apply(event);

        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        if (isHTML) {
            multilingualContentService.buildLanguageStringsFromHTMLMC(srContent, otherContent,
                contentList, true);
        } else {
            multilingualContentService.buildLanguageStrings(srContent, otherContent, contentList,
                true);
        }

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        srSetter.accept(index,
            !srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        otherSetter.accept(index,
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
    }

    private void setIndexDate(Event event, EventIndex index) {
        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

        if (event.getDateFrom().getMonth().equals(Month.JANUARY) &&
            event.getDateTo().getMonth().equals(Month.DECEMBER)) {
            index.setDateFromTo(String.valueOf(event.getDateFrom().getYear()));
        } else {
            index.setDateFromTo(
                event.getDateFrom().format(formatter) + " - " +
                    event.getDateTo().format(formatter));
        }

        index.setDateFrom(event.getDateFrom());
        index.setDateTo(event.getDateTo());
        index.setDateSortable(event.getDateFrom());
    }

    protected void reorderEventContributions(Integer eventId, Integer contributionId,
                                             Integer oldContributionOrderNumber,
                                             Integer newContributionOrderNumber) {
        var event = eventRepository.findById(eventId);

        if (event.isEmpty()) {
            return;
        }

        var contributions = event.get().getContributions().stream()
            .map(contribution -> (PersonContribution) contribution).collect(
                Collectors.toSet());

        personContributionService.reorderContributions(contributions, contributionId,
            oldContributionOrderNumber, newContributionOrderNumber);
    }

    protected void setEventCommonVolatileFields(EventIndex eventIndex) {
        eventIndex.getRelatedInstitutionIds().addAll(
            eventRepository.findInstitutionIdsByEventIdAndAuthorContribution(
                    eventIndex.getDatabaseId())
                .stream().toList()
        );

        eventIndex.setClassifiedBy(
            commissionRepository.findCommissionsThatClassifiedEvent(eventIndex.getDatabaseId()));

        eventIndex.getCommissionAssessments().clear();
        commissionRepository.findAssessmentClassificationBasicInfoForEventAndCommissions(
            eventIndex.getDatabaseId(), eventIndex.getClassifiedBy()
        ).forEach(assessment ->
            eventIndex.getCommissionAssessments().add(
                new Triple<>(assessment.commissionId(),
                    assessment.assessmentCode(),
                    assessment.manual())));

        indexActiveEmploymentRelations(eventIndex, eventIndex.getDatabaseId());

        eventIndexRepository.save(eventIndex);
    }

    protected void completeForceDeletion(Integer eventId) {
        var index = eventIndexRepository.findByDatabaseId(eventId);
        index.ifPresent(eventIndexRepository::delete);

        documentPublicationIndexRepository.deleteByEventIdAndType(eventId,
            DocumentPublicationType.PROCEEDINGS.name());

        indexBulkUpdateService.removeIdFromRecord(
            "document_publication",
            "event_id",
            eventId
        );
    }
}
