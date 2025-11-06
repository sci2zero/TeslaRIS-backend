package rs.teslaris.exporter.util.skgif;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.core.util.persistence.IdentifierUtil;

public class PersonFilteringUtil {

    public static void addPersonFilters(SKGIFFilterCriteria criteria, Query query) {
        criteria.getFilters().forEach((key, value) -> {
            switch (key) {
                case "identifiers.scheme":
                    var fieldName = getIdentifierFieldName(value);
                    query.addCriteria(Criteria.where(fieldName).exists(true));
                    break;
                case "identifiers.value":
                    query.addCriteria(
                        new Criteria().orOperator(
                            Criteria.where("orcid").is(value),
                            Criteria.where("scopus_id").is(value)
                        )
                    );
                    break;
                case "given_name":
                    query.addCriteria(Criteria.where("name.firstName").is(value));
                    break;
                case "family_name":
                    query.addCriteria(Criteria.where("name.lastName").is(value));
                    break;
                case "name": {
                    var parts = value.trim().split("\\s+");

                    if (parts.length == 1) {
                        query.addCriteria(
                            new Criteria().orOperator(
                                Criteria.where("name.firstName").regex("^" + parts[0], "i"),
                                Criteria.where("name.lastName").regex("^" + parts[0], "i")
                            )
                        );
                    } else {
                        var first = parts[0];
                        var last = parts[1];

                        query.addCriteria(
                            new Criteria().andOperator(
                                Criteria.where("name.firstName").regex("^" + first, "i"),
                                Criteria.where("name.lastName").regex("^" + last, "i")
                            )
                        );
                    }
                    break;
                }
                case "affiliations.affiliation":
                    query.addCriteria(
                        Criteria.where("employments")
                            .elemMatch(
                                Criteria.where("employment_institution.database_id")
                                    .is(Integer.parseInt(IdentifierUtil.removeCommonPrefix(value)))
                            ));
                    break;
                case "affiliations.role":
                    query.addCriteria(Criteria.where("employments")
                        .elemMatch(Criteria.where("role").regex(value, "i")));
                    break;
                case "affiliations.period.start":
                    addDateQuery(query, value, "from");
                    break;
                case "affiliations.period.end":
                    addDateQuery(query, value, "to");
                    break;
            }
        });
    }

    private static String getIdentifierFieldName(String skgifFieldName) {
        return switch (skgifFieldName) {
            case "orcid" -> "orcid";
            case "url" -> "scopus_id";
            default -> "IGNORED";
        };
    }

    private static void addDateQuery(Query query, String dateString, String field) {
        if (!dateString.contains("T")) {
            dateString += "T00:00:00";
        }

        var start = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .toLocalDate();
        var end = start.plusDays(1);

        query.addCriteria(
            Criteria.where("employments").elemMatch(Criteria.where(field).gte(start).lt(end)));
    }
}
