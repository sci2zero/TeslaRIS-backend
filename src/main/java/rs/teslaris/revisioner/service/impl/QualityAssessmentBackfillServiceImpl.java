package rs.teslaris.revisioner.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexmodel.PublisherIndex;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.model.QualityAssessmentTarget;
import rs.teslaris.revisioner.service.interfaces.DataQualityService;
import rs.teslaris.revisioner.service.interfaces.QualityAssessmentBackfillService;
import rs.teslaris.revisioner.service.interfaces.RevisionService;

@Service
@RequiredArgsConstructor
@Slf4j
public class QualityAssessmentBackfillServiceImpl implements QualityAssessmentBackfillService {

    private static final int PAGE_SIZE = 100;

    private static final List<String> DOCUMENT_PERSON_FIELDS = List.of(
        "author_ids", "editor_ids", "reviewer_ids", "board_member_ids", "advisor_ids",
        "presenter_ids", "translator_ids", "assistant_staff_ids", "arguer_ids", "owner_ids",
        "associated_editor_ids", "invited_editor_ids"
    );

    private final RevisionService revisionService;

    private final DataQualityService dataQualityService;

    private final SearchService<PersonIndex> personSearchService;

    private final SearchService<OrganisationUnitIndex> organisationUnitSearchService;

    private final SearchService<EventIndex> eventSearchService;

    private final SearchService<DocumentPublicationIndex> documentSearchService;

    private final SearchService<JournalIndex> journalSearchService;

    private final SearchService<BookSeriesIndex> bookSeriesSearchService;

    private final SearchService<PublisherIndex> publisherSearchService;


    @Override
    public void performBackfill(List<QualityAssessmentTarget> targets, List<Integer> personIds,
                                List<Integer> organisationUnitIds, String profileName,
                                boolean rewriteExistingAssessments) {
        if (Objects.isNull(targets) || targets.isEmpty()) {
            log.warn("No target index specified, nothing to backfill.");
            return;
        }

        targets.forEach(target -> {
            log.info("Backfilling quality assessments for {} records.", target);

            switch (target) {
                case PERSON -> scan("person", PersonIndex.class, personSearchService,
                    List.of("databaseId"), List.of("employment_institutions_id"),
                    personIds, organisationUnitIds,
                    index -> new Pair<>(EntityType.PERSON.name(), index.getDatabaseId()),
                    profileName, rewriteExistingAssessments);
                case ORGANISATION_UNIT -> scan("organisation_unit", OrganisationUnitIndex.class,
                    organisationUnitSearchService,
                    List.of(), List.of("databaseId"),
                    personIds, organisationUnitIds,
                    index -> new Pair<>(EntityType.ORGANISATION_UNIT.name(),
                        index.getDatabaseId()),
                    profileName, rewriteExistingAssessments);
                case EVENT -> scan("events", EventIndex.class, eventSearchService,
                    List.of("related_person_ids"), List.of("related_institution_ids"),
                    personIds, organisationUnitIds,
                    index -> new Pair<>(
                        Objects.nonNull(index.getEventType()) ? index.getEventType().name() : null,
                        index.getDatabaseId()),
                    profileName, rewriteExistingAssessments);
                case DOCUMENT -> scan("document_publication", DocumentPublicationIndex.class,
                    documentSearchService,
                    DOCUMENT_PERSON_FIELDS, List.of("organisation_unit_ids"),
                    personIds, organisationUnitIds,
                    index -> new Pair<>(index.getType(), index.getDatabaseId()),
                    profileName, rewriteExistingAssessments);
                case JOURNAL -> scan("journal", JournalIndex.class, journalSearchService,
                    List.of(), List.of("related_institution_ids"),
                    personIds, organisationUnitIds,
                    index -> new Pair<>(EntityType.JOURNAL.name(), index.getDatabaseId()),
                    profileName, rewriteExistingAssessments);
                case BOOK_SERIES -> scan("book_series", BookSeriesIndex.class,
                    bookSeriesSearchService,
                    List.of(), List.of(),
                    personIds, organisationUnitIds,
                    index -> new Pair<>(EntityType.BOOK_SERIES.name(), index.getDatabaseId()),
                    profileName, rewriteExistingAssessments);
                case PUBLISHER -> scan("publisher", PublisherIndex.class, publisherSearchService,
                    List.of(), List.of(),
                    personIds, organisationUnitIds,
                    index -> new Pair<>(EntityType.PUBLISHER.name(), index.getDatabaseId()),
                    profileName, rewriteExistingAssessments);
            }
        });

        log.info("Finished backfilling quality assessments.");
    }

    /**
     * Indexes without an attribute linking them to the requested persons or organisation units are
     * scanned in full, as there is no way to narrow them down.
     * <p>
     * Paging walks a {@code databaseId} watermark rather than an increasing page number, so a full
     * scan never pays the cost of deep pagination.
     */
    private <T> void scan(String indexName, Class<T> indexClass, SearchService<T> searchService,
                          List<String> personFields, List<String> organisationUnitFields,
                          List<Integer> personIds, List<Integer> organisationUnitIds,
                          Function<T, Pair<String, Integer>> entityResolver, String profileName,
                          boolean rewriteExistingAssessments) {
        var pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "databaseId"));
        Integer lastSeenId = null;

        while (true) {
            var query = buildQuery(personFields, organisationUnitFields, personIds,
                organisationUnitIds, lastSeenId);

            var content = searchService.runQuery(query, pageable, indexClass, indexName)
                .getContent();

            if (content.isEmpty()) {
                break;
            }

            for (var indexEntry : content) {
                var entity = entityResolver.apply(indexEntry);

                if (Objects.isNull(entity.b)) {
                    continue;
                }

                lastSeenId = entity.b;

                if (Objects.nonNull(entity.a)) {
                    process(entity.a, entity.b, profileName, rewriteExistingAssessments);
                }
            }

            if (content.size() < PAGE_SIZE || Objects.isNull(lastSeenId)) {
                break;
            }
        }
    }

    private Query buildQuery(List<String> personFields, List<String> organisationUnitFields,
                             List<Integer> personIds, List<Integer> organisationUnitIds,
                             Integer lastSeenId) {
        var idFilters = new ArrayList<Query>();

        if (Objects.nonNull(personIds) && !personIds.isEmpty()) {
            personFields.forEach(field -> idFilters.add(termsQuery(field, personIds)));
        }

        if (Objects.nonNull(organisationUnitIds) && !organisationUnitIds.isEmpty()) {
            organisationUnitFields.forEach(
                field -> idFilters.add(termsQuery(field, organisationUnitIds)));
        }

        var clauses = new ArrayList<Query>();

        if (!idFilters.isEmpty()) {
            clauses.add(
                BoolQuery.of(b -> b.should(idFilters).minimumShouldMatch("1"))._toQuery());
        }

        if (Objects.nonNull(lastSeenId)) {
            clauses.add(RangeQuery.of(r -> r
                .field("databaseId")
                .gt(JsonData.of(lastSeenId))
            )._toQuery());
        }

        if (clauses.isEmpty()) {
            return MatchAllQuery.of(matchAll -> matchAll)._toQuery();
        }

        return BoolQuery.of(b -> b.must(clauses))._toQuery();
    }

    private Query termsQuery(String field, List<Integer> ids) {
        return TermsQuery.of(terms -> terms
            .field(field)
            .terms(values -> values.value(ids.stream().map(FieldValue::of).toList()))
        )._toQuery();
    }

    private void process(String entityType, Integer entityId, String profileName,
                         boolean rewriteExistingAssessments) {
        try {
            if (revisionService.createRevisionFromCurrentState(entityType, entityId, profileName)) {
                return;
            }

            if (rewriteExistingAssessments) {
                dataQualityService.reassessLatestRevision(entityType, entityId, profileName);
            }
        } catch (Exception e) {
            log.warn("Unable to backfill quality assessment for entity '{}' (ID={}). Reason: {}",
                entityType, entityId, e.getMessage());
        }
    }
}
