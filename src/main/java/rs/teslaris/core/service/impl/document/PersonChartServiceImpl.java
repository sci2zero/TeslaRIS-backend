package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.document.PersonChartService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonChartServiceImpl implements PersonChartService {

    private final ElasticsearchClient elasticsearchClient;


    @Override
    public List<YearlyCounts> getPublicationCountsForPerson(Integer personId, Integer startYear,
                                                            Integer endYear) {
        var yearlyCounts = new ArrayList<YearlyCounts>();
        for (int i = startYear; i <= endYear; i++) {
            try {
                yearlyCounts.add(
                    new YearlyCounts(i, getPublicationCountsByTypeForAuthorAndYear(personId, i)));
            } catch (IOException e) {
                log.warn("Unable to fetch person publication counts for {}. Adding all zeros.", i);
            }
        }

        return yearlyCounts;
    }

    public Map<String, Long> getPublicationCountsByTypeForAuthorAndYear(Integer authorId,
                                                                        Integer year)
        throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index("document_publication")
                .size(0) // no hits, only aggregations
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("year").value(year)))
                        .must(m -> m.term(t -> t.field("author_ids").value(authorId)))
                    )
                )
                .aggregations("by_type", a -> a
                    .terms(t -> t.field("type").size(50)) // increase size if you expect many types
                ),
            Void.class
        );

        return response.aggregations()
            .get("by_type")
            .sterms()
            .buckets()
            .array()
            .stream()
            .collect(Collectors.toMap(
                b -> b.key().stringValue(),
                MultiBucketBase::docCount
            ));
    }

    public record YearlyCounts(
        Integer year,
        Map<String, Long> countsByCategory
    ) {
    }
}
