package rs.teslaris.exporter.util.skgif;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class OrganisationFilteringUtil {

    public static void addOrganisationFilters(SKGIFFilterCriteria criteria, Query query) {
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
                            Criteria.where("scopus_afid").is(value),
                            Criteria.where("open_alex").is(value)
                        )
                    );
                    break;
                case "country":
                    query.addCriteria(Criteria.where("country").is(value));
                    break;
                case "short_name":
                    query.addCriteria(Criteria.where("name_abbreviation").is(value));
                    break;
                case "name", "other_names":
                    query.addCriteria(
                        Criteria.where("name").elemMatch(Criteria.where("content").is(value)));
                    break;
                case "website":
                    query.addCriteria(Criteria.where("uris").in(value));
                    break;
            }
        });
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
