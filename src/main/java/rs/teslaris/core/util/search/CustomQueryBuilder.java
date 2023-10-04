package rs.teslaris.core.util.search;

import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import lombok.Getter;
import org.elasticsearch.common.unit.Fuzziness;

@Getter
public class CustomQueryBuilder {

    private static final int MAX_EDITS = 1;

    public static Query buildQuery(SearchType queryType, String field, String value) {
        validateInput(field, value);

        String fuzzinessLevel = Fuzziness.fromEdits(MAX_EDITS).asString();

        switch (queryType) {
            case regular:
                return MatchQuery.of(m -> m
                    .field(field)
                    .query(value)
                    .fuzziness(fuzzinessLevel)
                )._toQuery();
            case fuzzy:
                return FuzzyQuery.of(m -> m
                    .field(field)
                    .value(value)
                    .fuzziness(fuzzinessLevel)
                )._toQuery();
            case prefix:
                return PrefixQuery.of(m -> m
                    .field(field)
                    .value(value)
                )._toQuery();
            case range:
                String[] values = value.split(" ");
                return RangeQuery.of(m -> m
                    .field(field)
                    .from(values[0])
                    .to(values[1])
                )._toQuery();
            default:
                return MatchPhraseQuery.of(m -> m
                    .field(field)
                    .query(value)
                )._toQuery();
        }
    }

    private static void validateInput(String field, String value) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Field not specified");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value not specified");
        }
    }

}
