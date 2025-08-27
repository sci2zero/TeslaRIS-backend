package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.service.interfaces.document.PersonVisualizationDataService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.Pair;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonVisualizationDataServiceImpl implements PersonVisualizationDataService {

    private final ElasticsearchClient elasticsearchClient;

    private final PersonService personService;


    @Override
    public List<YearlyCounts> getPublicationCountsForPerson(Integer personId, Integer startYear,
                                                            Integer endYear) {
        if (Objects.isNull(startYear) || Objects.isNull(endYear)) {
            var yearRange = findPublicationYearRange(personId);
            if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
                return Collections.emptyList();
            }

            startYear = yearRange.a;
            endYear = yearRange.b;
        }

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

    @Override
    public List<StatisticsByCountry> getByCountryStatisticsForPerson(Integer personId,
                                                                     StatisticsType statisticsType) {
        var person = personService.findOne(personId);
        var allMergedPersonIds = new ArrayList<>(person.getMergedIds());
        allMergedPersonIds.add(personId);

        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("person_id").terms(
                                v -> v.value(allMergedPersonIds.stream()
                                    .map(FieldValue::of)
                                    .toList())
                            )))
                            .must(m -> m.term(t -> t.field("type").value(statisticsType.name())))
                        )
                    )
                    .aggregations("by_country", a -> a
                        .terms(t -> t.field("country_code")
                            .size(
                                195)) // 195 countries exist at the moment, we can lower this if you need arises
                        .aggregations("country_name", sub -> sub
                            .terms(t -> t.field("country_name").size(1))
                        )
                    ),
                Void.class
            );
        } catch (IOException e) {
            return Collections.emptyList();
        }

        List<StatisticsByCountry> result = new ArrayList<>();

        response.aggregations()
            .get("by_country").sterms().buckets().array()
            .forEach(bucket -> {
                String countryCode = bucket.key().stringValue();
                long views = bucket.docCount();

                String countryName = bucket.aggregations()
                    .get("country_name").sterms().buckets().array()
                    .stream()
                    .findFirst()
                    .map(StringTermsBucket::key)
                    .map(FieldValue::stringValue)
                    .orElse(countryCode); // fallback, should never happen

                result.add(new StatisticsByCountry(countryCode, countryName, views));
            });

        return result;
    }

    private Map<String, Long> getPublicationCountsByTypeForAuthorAndYear(Integer authorId,
                                                                         Integer year)
        throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index("document_publication")
                .size(0) // no hits, only aggregations
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("year").value(year)))
                        .must(m -> m.term(t -> t.field("author_ids").value(authorId)))
                        .mustNot(m -> m.term(t -> t.field("type").value("PROCEEDINGS")))
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

    private Pair<Integer, Integer> findPublicationYearRange(Integer authorId) {
        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("author_ids").value(authorId)))
                            .must(m -> m.range(t -> t.field("year").gt(JsonData.of(0))))
                        )
                    )
                    .aggregations("earliestYear", a -> a.min(m -> m.field("year")))
                    .aggregations("latestYear", a -> a.max(m -> m.field("year"))),
                Void.class
            );
        } catch (IOException e) {
            log.error("Error finding publication year range for Person with ID {}.", authorId);
            return new Pair<>(null, null);
        }

        double min = response.aggregations().get("earliestYear").min().value();
        double max = response.aggregations().get("latestYear").max().value();

        return new Pair<>((int) min, (int) max);
    }

    public record YearlyCounts(
        Integer year,
        Map<String, Long> countsByCategory
    ) {
    }

    public record StatisticsByCountry(
        String countryCode,
        String countryName,
        long value
    ) {
    }
}
