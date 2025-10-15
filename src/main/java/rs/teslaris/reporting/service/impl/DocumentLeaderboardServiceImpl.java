package rs.teslaris.reporting.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.interfaces.DocumentLeaderboardService;
import rs.teslaris.reporting.utility.QueryUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentLeaderboardServiceImpl implements DocumentLeaderboardService {

    private final ElasticsearchClient elasticsearchClient;

    private final OrganisationUnitService organisationUnitService;


    @Override
    public List<Pair<DocumentPublicationIndex, Long>> getPublicationsWithMostCitations(
        Integer institutionId, Integer fromYear, Integer toYear) {
        var yearRange = QueryUtil.constructYearRange(fromYear, toYear);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(institutionId);

        var eligibleOUIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId);

        try {
            SearchResponse<DocumentPublicationIndex> publicationResponse =
                elasticsearchClient.search(s -> s
                        .index("document_publication")
                        .size(0)
                        .query(q -> q
                            .bool(b -> b
                                .must(m -> m.term(t -> t.field("is_approved").value(true)))
                                .must(QueryUtil.organisationUnitMatchQuery(eligibleOUIds,
                                    searchFields))
                                .must(m -> m.range(
                                    r -> r.field("year").gte(JsonData.of(yearRange.a))
                                        .lte(JsonData.of(yearRange.b))))
                                .mustNot(m -> m.term(t -> t.field("type").value("PROCEEDINGS")))
                            )
                        )
                        .aggregations("by_citation_count", a -> a.terms(
                            t -> t
                                .field("total_citations")
                                .size(10)
                                .order(List.of(
                                    NamedValue.of("total_citations", SortOrder.Desc)
                                )))
                        ),
                    DocumentPublicationIndex.class
                );

            return publicationResponse.hits().hits().stream()
                .map(hit -> {
                    var publication = hit.source();
                    var citationCount = (Objects.nonNull(publication) &&
                        Objects.nonNull(publication.getTotalCitations()))
                        ? publication.getTotalCitations()
                        : 0L;
                    return new Pair<>(publication, citationCount);
                })
                .filter(hit -> Objects.nonNull(hit.a))
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error while fetching OU publication count leaderboard. Reason: {}",
                e.getMessage());
            return Collections.emptyList();
        }
    }
}
