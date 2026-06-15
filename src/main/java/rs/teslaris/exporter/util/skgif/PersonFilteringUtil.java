package rs.teslaris.exporter.util.skgif;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;

public class PersonFilteringUtil {

    public static final List<String> SUPPORTED_FILTERS = Arrays.asList(
        "identifiers.scheme", "identifiers.value",
        "given_name", "family_name", "name", "affiliations.affiliation",
        "affiliations.role", "affiliations.period.start", "affiliations.period.end"
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
                            Criteria.where("scopus_id").is(value)
                        )
                    );
                    break;
                case "given_name":
                    criteriaList.add(Criteria.where("name.firstName").is(value));
                    break;
                case "family_name":
                    criteriaList.add(Criteria.where("name.lastName").is(value));
                    break;
                case "name": {
                    var parts = value.trim().split("\\s+");

                    if (parts.length == 1) {
                        criteriaList.add(
                            new Criteria().orOperator(
                                Criteria.where("name.firstName").regex("^" + parts[0], "i"),
                                Criteria.where("name.lastName").regex("^" + parts[0], "i")
                            )
                        );
                    } else {
                        var first = parts[0];
                        var last = parts[1];

                        criteriaList.add(
                            new Criteria().andOperator(
                                Criteria.where("name.firstName").regex("^" + first, "i"),
                                Criteria.where("name.lastName").regex("^" + last, "i")
                            )
                        );
                    }
                    break;
                }
                case "affiliations.affiliation":
                    criteriaList.add(
                        Criteria.where("employments")
                            .elemMatch(
                                Criteria.where("employment_institution.database_id")
                                    .is(Integer.parseInt(IdentifierUtil.removeCommonPrefix(value)))
                            ));
                    break;
                case "affiliations.role":
                    criteriaList.add(Criteria.where("employments")
                        .elemMatch(Criteria.where("role").regex(value, "i")));
                    break;
                case "affiliations.period.start":
                    addDateQuery(criteriaList, value, "from");
                    break;
                case "affiliations.period.end":
                    addDateQuery(criteriaList, value, "to");
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
            case "url" -> "scopus_id";
            default -> "IGNORED";
        };
    }

    private static void addDateQuery(List<Criteria> criteriaList, String dateString, String field) {
        if (!dateString.contains("T")) {
            dateString += "T00:00:00";
        }

        var start = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .toLocalDate();
        var end = start.plusDays(1);

        criteriaList.add(
            Criteria.where("employments").elemMatch(Criteria.where(field).gte(start).lt(end)));
    }
}
