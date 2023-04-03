package rs.teslaris.core.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DummyIndex;
import rs.teslaris.core.indexrepository.DummyIndexRepository;

@RestController
@RequestMapping("/api/dummy")
@RequiredArgsConstructor
public class DummyController {

    private final ElasticsearchClient elasticsearchClient;

    private final ElasticsearchOperations template;

    private final DummyIndexRepository repo;

    @GetMapping
    public String returnDummyData() {
        return "TEST";
    }

    @GetMapping("/all")
    public List<DummyIndex> getAll() {
        return StreamSupport
            .stream(repo.findAll().spliterator(), false)
            .collect(Collectors.toList());
    }

    @PostMapping
    public DummyIndex testIndex() throws IOException {
        var indexingUnit = new DummyIndex();
        indexingUnit.setTestText("TEST TEST TEST");

        elasticsearchClient.index(i -> i
            .index("dummy")
            .id(indexingUnit.getId())
            .document(indexingUnit)
        );

//        repo.save(indexingUnit);

        return indexingUnit;
    }

    @GetMapping("/search/client")
    public Page<DummyIndex> testRetrievalNewer(Pageable pageable, String text) throws IOException {
        var query = buildQuery(text);

        var response = elasticsearchClient.search(s -> s
                .index("dummy")
                .query(q -> q.bool(b -> b.must(query))
                )
                .from((int) pageable.getOffset())
                .size(pageable.getPageSize()),
            DummyIndex.class
        );

        var content = new ArrayList<DummyIndex>();
        for (var hit : response.hits().hits()) {
            content.add(hit.source());
        }

        var totalElements = response.hits().total().value();

        return new PageImpl<>(content, pageable, totalElements);
    }

    @GetMapping("/search")
    public Page<DummyIndex> testRetrieval(Pageable pageable, String text) throws IOException {
        var queryBuilder = buildQuery(text);

        var searchQueryBuilder = new NativeQueryBuilder()
            .withQuery(queryBuilder)
            .withPageable(pageable);

        var searchQuery = searchQueryBuilder.build();

        var searchHits = template
            .search(searchQuery, DummyIndex.class, IndexCoordinates.of("dummy"));

        var searchHitsPaged = SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());

        return (Page<DummyIndex>) SearchHitSupport.unwrapSearchHits(searchHitsPaged);
    }

    private Query buildQuery(String text) {
        return BoolQuery.of(b -> b
            .should(s -> s.match(m -> m.field("test_text").query(text)))
        )._toQuery();
    }
}
