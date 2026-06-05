package rs.teslaris.exporter.util.skgif;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.core.util.search.StringUtil;

public class OrganisationFilteringUtil {

    public static final List<String> SUPPORTED_FILTERS = Arrays.asList(
        "identifiers.scheme", "identifiers.value", "name", "short_name", "other_names",
        "country", "website"
    );


    public static void addQueryFilters(SKGIFFilterCriteria criteria, Query query) {
        var criteriaList = new ArrayList<Criteria>();

        criteria.getFilters().forEach((key, value) -> {
            switch (key) {
                case "identifiers.scheme":
                    var fieldName = getIdentifierFieldName(value);
                    criteriaList.add(Criteria.where(fieldName).exists(true));
                    break;
                case "identifiers.value":
                    value = StringUtil.normalizeIdentifier(value);
                    criteriaList.add(
                        new Criteria().orOperator(
                            Criteria.where("orcid").is(value),
                            Criteria.where("scopus_afid").is(value),
                            Criteria.where("open_alex").is(value),
                            Criteria.where("ror").is(value)
                        )
                    );
                    break;
                case "country":
                    criteriaList.add(Criteria.where("country").is(value));
                    break;
                case "short_name":
                    criteriaList.add(Criteria.where("name_abbreviation")
                        .elemMatch(Criteria.where("content").is(value)));
                    break;
                case "name", "other_names":
                    criteriaList.add(
                        Criteria.where("name").elemMatch(Criteria.where("content").is(value)));
                    break;
                case "website":
                    criteriaList.add(Criteria.where("uris").in(value));
                    break;
            }
        });

        if (!criteriaList.isEmpty()) {
            query.addCriteria(
                new Criteria().andOperator(criteriaList.toArray(new Criteria[0]))
            );
        }
    }

    private static String getIdentifierFieldName(String skgifFieldName) {
        return switch (skgifFieldName) {
            case "orcid" -> "orcid";
            case "url" -> "scopus_afid";
            case "openalex" -> "open_alex";
            default -> "IGNORED";
        };
    }
}
