package rs.teslaris.core.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.service.PersonIndexService;

@Service
@RequiredArgsConstructor
public class PersonIndexServiceImpl implements PersonIndexService {

    private final ElasticsearchOperations elasticsearchTemplate;

    private final PersonIndexRepository personIndexRepository;

    @Override
    public Page<PersonIndex> findPeopleByNameAndEmployment(List<String> tokens, Pageable pageable) {
        var query = buildNameAndEmploymentQuery(tokens);

        var searchQueryBuilder = new NativeQueryBuilder()
            .withQuery(query)
            .withPageable(pageable);

        var searchQuery = searchQueryBuilder.build();

        var searchHits = elasticsearchTemplate
            .search(searchQuery, PersonIndex.class, IndexCoordinates.of("person"));

        var searchHitsPaged = SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());

        return (Page<PersonIndex>) SearchHitSupport.unwrapSearchHits(searchHitsPaged);
    }

    @Override
    public Page<PersonIndex> findPeopleForOrganisationUnit(
        Integer employmentInstitutionId,
        Pageable pageable) {
        return personIndexRepository.findByEmploymentInstitutionsIdIn(pageable,
            List.of(employmentInstitutionId));
    }

    private Query buildNameAndEmploymentQuery(List<String> tokens) {
        return BoolQuery.of(q -> q
            .must(mb -> mb.bool(b -> {
                    tokens.forEach(
                        token -> {
                            b.should(sb -> sb.match(m -> m.field("name").query(token)));
                            b.should(sb -> sb.match(m -> m.field("employments").query(token)));
                            b.should(sb -> sb.match(m -> m.field("employments_srp").query(token)));
                        });
                    return b;
                }
            ))
        )._toQuery();
    }
}
