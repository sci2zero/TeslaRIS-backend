package rs.teslaris.core.controller;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DummyIndex;
import rs.teslaris.core.indexrepository.DummyIndexRepository;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.service.LanguageService;
import rs.teslaris.core.util.email.EmailUtil;


@RestController
@RequestMapping("/api/dummy")
@RequiredArgsConstructor
public class DummyController {

    private final ElasticsearchOperations template;

    private final DummyIndexRepository repo;

    private final EmailUtil emailUtil;

    //    TEMP SERVICE FOR TESTING
    //    TODO: REMOVE
    private final LanguageService languageService;


    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_READ')")
    public String returnDummyData() {
        return "TEST";
    }

    @GetMapping("/all")
    public List<DummyIndex> getAll() {
        return StreamSupport
            .stream(repo.findAll().spliterator(), false)
            .collect(Collectors.toList());
    }

    //    TEMP MAPPING
    //    TODO: REMOVE
    @GetMapping("/all2")
    public List<Language> getAll2() {
        return languageService.findAll();
    }

    //    TEMP MAPPING
    //    TODO: REMOVE
    @GetMapping("/allPage")
    public Page<Language> getAllPage(Pageable pageable) {
        return languageService.findAll(pageable);
    }

    @PostMapping
    public DummyIndex testIndex() throws IOException {
        var indexingUnit = new DummyIndex();
        indexingUnit.setTestText("TEST TEST TEST");

        repo.save(indexingUnit);

        return indexingUnit;
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

    @PostMapping("/email")
    public void testEmail() {
        emailUtil.sendSimpleEmail("email@email.com", "SUBJECT", "TEXT");
    }
}
