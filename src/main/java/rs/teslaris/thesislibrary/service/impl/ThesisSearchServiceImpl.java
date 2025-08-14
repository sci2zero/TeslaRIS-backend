package rs.teslaris.thesislibrary.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.tracing.SessionTrackingUtil;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisSearchService;

@Service
@Traceable
public class ThesisSearchServiceImpl implements ThesisSearchService {

    private final SearchFieldsLoader searchFieldsLoader;

    private final String configFileName = "thesisSearchFieldConfiguration.json";

    private final SearchService<DocumentPublicationIndex> searchService;

    private final ExpressionTransformer expressionTransformer;


    public ThesisSearchServiceImpl(SearchFieldsLoader searchFieldsLoader,
                                   SearchService<DocumentPublicationIndex> searchService,
                                   ExpressionTransformer expressionTransformer) throws IOException {
        this.searchFieldsLoader = searchFieldsLoader;
        this.searchService = searchService;
        this.expressionTransformer = expressionTransformer;
        this.searchFieldsLoader.loadConfiguration(configFileName);
    }

    @Override
    public Page<DocumentPublicationIndex> performSimpleThesisSearch(
        ThesisSearchRequestDTO searchRequest, Pageable pageable) {
        return performQuery(searchRequest, pageable, SearchRequestType.SIMPLE);
    }

    @Override
    public Page<DocumentPublicationIndex> performAdvancedThesisSearch(
        ThesisSearchRequestDTO searchRequest, Pageable pageable) {
        return performQuery(searchRequest, pageable, SearchRequestType.ADVANCED);
    }

    @Override
    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        Boolean onlyExportFields) {
        return searchFieldsLoader.getSearchFields(configFileName, onlyExportFields);
    }

    private Page<DocumentPublicationIndex> performQuery(ThesisSearchRequestDTO searchRequest,
                                                        Pageable pageable,
                                                        SearchRequestType queryType) {
        var queryAndIndex = fetchQueryAndIndexName(searchRequest, queryType);

        return searchService.runQuery(
            queryAndIndex.a,
            pageable,
            DocumentPublicationIndex.class, queryAndIndex.b);
    }

    @Override
    public List<Pair<String, Long>> performWordCloudSearch(ThesisSearchRequestDTO searchRequest,
                                                           SearchRequestType queryType,
                                                           boolean foreignLanguage) {
        var queryAndIndex = fetchQueryAndIndexName(searchRequest, queryType);
        return searchService.runWordCloudSearch(queryAndIndex.a, queryAndIndex.b, foreignLanguage);
    }

    private Pair<Query, String> fetchQueryAndIndexName(ThesisSearchRequestDTO searchRequest,
                                                       SearchRequestType queryType) {
        var query = buildSearchQuery(searchRequest.tokens(), searchRequest.facultyIds(),
            searchRequest.authorIds(), searchRequest.advisorIds(),
            searchRequest.boardMemberIds(), searchRequest.boardPresidentIds(),
            searchRequest.thesisTypes(), searchRequest.dateFrom(), searchRequest.dateTo(),
            queryType, searchRequest.showOnlyOpenAccess());
        var searchIndex = "document_publication";

        return new Pair<>(query, searchIndex);
    }

    public Query buildSearchQuery(List<String> tokens, List<Integer> facultyIds,
                                  List<Integer> authorIds, List<Integer> advisorIds,
                                  List<Integer> boardMemberIds,
                                  List<Integer> boardPresidentIds,
                                  List<ThesisType> allowedTypes, LocalDate startDate,
                                  LocalDate endDate, SearchRequestType searchRequestType,
                                  Boolean showOnlyOpenAccessTheses) {
        int minShouldMatch = (int) Math.ceil(tokens.size() * 0.8);
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(buildMetadataQuery(facultyIds, authorIds, advisorIds, boardMemberIds,
                boardPresidentIds, allowedTypes, startDate, endDate, showOnlyOpenAccessTheses));
            b.must(searchRequestType.equals(SearchRequestType.SIMPLE) ?
                buildTokenQuery(tokens, minShouldMatch) :
                expressionTransformer.parseAdvancedQuery(tokens));
            b.must(
                sb -> sb.match(m -> m.field("type").query(DocumentPublicationType.THESIS.name())));
            return b;
        })))._toQuery();
    }

    private Query buildMetadataQuery(List<Integer> facultyIds, List<Integer> authorIds,
                                     List<Integer> advisorIds, List<Integer> boardMemberIds,
                                     List<Integer> boardPresidentIds, List<ThesisType> allowedTypes,
                                     LocalDate startDate, LocalDate endDate,
                                     Boolean showOnlyOpenAccessTheses) {
        return BoolQuery.of(b -> {
            if (!facultyIds.isEmpty()) {
                b.must(createIdTermsQuery("organisation_unit_ids", facultyIds));
            }

            if (!authorIds.isEmpty()) {
                b.must(createIdTermsQuery("author_ids", authorIds));
            }

            if (!advisorIds.isEmpty()) {
                b.must(createIdTermsQuery("advisor_ids", advisorIds));
            }

            if (!boardMemberIds.isEmpty()) {
                b.must(createIdTermsQuery("board_member_ids", boardMemberIds));
            }

            if (!boardPresidentIds.isEmpty()) {
                b.must(createIdTermsQuery("board_president_id", boardPresidentIds));
            }

            if (Objects.nonNull(allowedTypes) && !allowedTypes.isEmpty()) {
                b.must(createThesisTypeTermsQuery(allowedTypes));
            }

            if (Objects.nonNull(startDate) && Objects.nonNull(endDate)) {
                b.must(m -> m.range(rq -> rq.field("thesis_defence_date")
                    .gte(JsonData.of(startDate))
                    .lte(JsonData.of(endDate))));
            }

            if (showOnlyOpenAccessTheses) {
                b.must(m -> m.term(tq -> tq.field("is_open_access").value(true)));
            }

            if (!SessionTrackingUtil.isUserLoggedIn()) {
                b.must(q -> q.term(t -> t.field("is_approved").value(true)));
            }

            b.must(m -> m.exists(e -> e.field("thesis_defence_date")));
            return b;
        })._toQuery();
    }

    private Query buildTokenQuery(List<String> tokens, int minShouldMatch) {
        return BoolQuery.of(eq -> {
            tokens.forEach(token -> {
                if (token.startsWith("\"") && token.endsWith("\"")) {
                    eq.must(mp -> mp.bool(m -> m
                        .should(sb -> sb.matchPhrase(
                            mq -> mq.field("title_sr").query(token.replace("\"", ""))))
                        .should(sb -> sb.matchPhrase(
                            mq -> mq.field("title_other").query(token.replace("\"", ""))))
                    ));
                }
                eq.should(sb -> sb.wildcard(
                        m -> m.field("title_sr")
                            .value(StringUtil.performSimpleLatinPreprocessing(token) + "*")
                            .caseInsensitive(true)))
                    .should(sb -> sb.match(m -> m.field("title_sr").query(token)))
                    .should(sb -> sb.wildcard(
                        m -> m.field("title_other").value(token + "*").caseInsensitive(true)))
                    .should(sb -> sb.match(m -> m.field("description_sr").query(token)))
                    .should(sb -> sb.match(m -> m.field("description_other").query(token)))
                    .should(sb -> sb.wildcard(m -> m.field("keywords_sr")
                        .value("*" + StringUtil.performSimpleLatinPreprocessing(token) + "*")))
                    .should(
                        sb -> sb.wildcard(m -> m.field("keywords_other").value("*" + token + "*")))
                    .should(sb -> sb.match(m -> m.field("full_text_sr").query(token)))
                    .should(sb -> sb.match(m -> m.field("full_text_other").query(token)))
                    .should(sb -> sb.match(m -> m.field("author_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("editor_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("reviewer_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("advisor_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("board_member_names").query(token)))
                    .should(sb -> sb.match(m -> m.field("doi").query(token)));
            });
            return eq.minimumShouldMatch(Integer.toString(minShouldMatch));
        })._toQuery();
    }

    private Query createThesisTypeTermsQuery(List<ThesisType> values) {
        return TermsQuery.of(t -> t
            .field("publication_type")
            .terms(v -> v.value(values.stream()
                .map(ThesisType::name)
                .map(FieldValue::of)
                .toList()))
        )._toQuery();
    }

    private Query createIdTermsQuery(String fieldName, List<Integer> values) {
        return TermsQuery.of(t -> t
            .field(fieldName)
            .terms(v -> v.value(values.stream()
                .map(FieldValue::of)
                .toList()))
        )._toQuery();
    }
}
