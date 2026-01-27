package rs.teslaris.thesislibrary.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.configuration.PublicReviewConfigurationLoader;
import rs.teslaris.core.util.session.SessionUtil;
import rs.teslaris.thesislibrary.dto.ThesisPublicReviewResponseDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryDissertationReportService;

@Service
@RequiredArgsConstructor
public class ThesisLibraryDissertationReportServiceImpl implements
    ThesisLibraryDissertationReportService {

    private final OrganisationUnitService organisationUnitService;

    private final SearchService<DocumentPublicationIndex> searchService;


    @Override
    public Page<ThesisPublicReviewResponseDTO> fetchPublicReviewDissertations(
        Integer institutionId,
        Integer year,
        Boolean notDefendedOnly,
        Integer userInstitutionId,
        Pageable pageable) {

        Set<Integer> institutionIds = getInstitutionIds(institutionId);

        List<Query> mustQueries = new ArrayList<>();

        if (!SessionUtil.isUserLoggedIn()) {
            mustQueries.add(
                TermQuery.of(t -> t
                    .field("is_approved")
                    .value(true)
                )._toQuery()
            );
        }

        mustQueries.add(buildTypeQuery());
        mustQueries.add(buildPublicationTypeClause());

        mustQueries.addAll(buildDateAndReviewStatusQueries(year, notDefendedOnly));

        if (!institutionIds.isEmpty()) {
            mustQueries.add(buildInstitutionQuery(institutionIds));
        } else if (Objects.nonNull(userInstitutionId) && userInstitutionId > 0) {
            var userInstitution =
                organisationUnitService.findOrganisationUnitById(userInstitutionId);
            if (userInstitution.getLegalEntity()) {
                mustQueries.add(buildInstitutionQuery(getInstitutionIds(userInstitution.getId())));
            } else {
                for (int superOuId : organisationUnitService.getSuperOUsHierarchyRecursive(
                    userInstitutionId)) {
                    var institution = organisationUnitService.findOrganisationUnitById(superOuId);
                    if (institution.getLegalEntity()) {
                        mustQueries.add(buildInstitutionQuery(Set.of(institution.getId())));
                    }
                }
            }
        }

        if (Objects.nonNull(notDefendedOnly) && notDefendedOnly) {
            mustQueries.add(buildNotDefendedQuery());
        }

        Query finalQuery = BoolQuery.of(b -> b.must(mustQueries))._toQuery();

        PageRequest pageRequest = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "latest_public_review_start_date")
        );

        return searchService.runQuery(finalQuery, pageRequest,
                DocumentPublicationIndex.class, "document_publication")
            .map(index -> new ThesisPublicReviewResponseDTO(
                index.getThesisAuthorName(),
                index.getTitleSr(),
                index.getTitleOther(),
                index.getThesisInstitutionNameSr(),
                index.getThesisInstitutionNameOther(),
                index.getScientificFieldSr(),
                index.getScientificFieldOther(),
                index.getLatestPublicReviewStartDate().toString(),
                index.getLatestPublicReviewStartDate()
                    .plusDays(PublicReviewConfigurationLoader.getLengthInDays(false)).toString(),
                index.getDatabaseId()
            ));
    }

    private Set<Integer> getInstitutionIds(Integer institutionId) {
        if (Objects.nonNull(institutionId)) {
            return new HashSet<>(
                organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId));
        }
        return Collections.emptySet();
    }

    private Query buildTypeQuery() {
        return TermQuery.of(t -> t
            .field("type")
            .value("THESIS")
        )._toQuery();
    }

    private Query buildPublicationTypeClause() {
        return BoolQuery.of(b -> b
            .should(TermQuery.of(t -> t.field("publication_type").value("PHD"))._toQuery())
            .should(
                TermQuery.of(t -> t.field("publication_type").value("PHD_ART_PROJECT"))._toQuery())
        )._toQuery();
    }

    private List<Query> buildDateAndReviewStatusQueries(Integer year, Boolean notDefendedOnly) {
        List<Query> queries = new ArrayList<>();

        if (Objects.nonNull(notDefendedOnly) && notDefendedOnly) {
            queries.add(RangeQuery.of(r -> r
                .field("public_review_start_dates")
                .lt(JsonData.of(LocalDate.now()
                    .minusDays(PublicReviewConfigurationLoader.getLengthInDays(false)).toString()))
            )._toQuery());

            queries.add(TermQuery.of(t -> t
                .field("is_on_public_review")
                .value("false")
            )._toQuery());

            queries.add(TermQuery.of(t -> t
                .field("is_public_review_completed")
                .value("true")
            )._toQuery());
        } else if (Objects.nonNull(year)) {
            queries.add(RangeQuery.of(r -> r
                .field("public_review_start_dates")
                .gte(JsonData.of(LocalDate.of(year, 1, 1).toString()))
                .lte(JsonData.of(LocalDate.of(year, 12, 31).toString()))
            )._toQuery());

            queries.add(TermQuery.of(t -> t
                .field("is_on_public_review")
                .value("false")
            )._toQuery());

            queries.add(TermQuery.of(t -> t
                .field("is_public_review_completed")
                .value("true")
            )._toQuery());

            queries.add(ExistsQuery.of(e -> e.field("thesis_defence_date"))._toQuery());
        } else {
            queries.add(RangeQuery.of(r -> r
                .field("public_review_start_dates")
                .gte(JsonData.of(LocalDate.now()
                    .minusDays(PublicReviewConfigurationLoader.getLengthInDays(false)).toString()))
                .lte(JsonData.of(LocalDate.now().toString()))
            )._toQuery());

            queries.add(TermQuery.of(t -> t
                .field("is_on_public_review")
                .value("true")
            )._toQuery());

            queries.add(TermQuery.of(t -> t
                .field("is_public_review_completed")
                .value("false")
            )._toQuery());
        }

        return queries;
    }

    private Query buildNotDefendedQuery() {
        return BoolQuery.of(b -> b.mustNot(
            mbb -> mbb.bool(bq -> bq.should(
                sh -> sh.exists(e -> e.field("thesis_defence_date"))
            ))
        ))._toQuery();
    }

    private Query buildInstitutionQuery(Set<Integer> institutionIds) {
        return TermsQuery.of(t -> t
            .field("thesis_institution_id")
            .terms(ts -> ts.value(
                institutionIds.stream().map(FieldValue::of).toList()
            ))
        )._toQuery();
    }
}
