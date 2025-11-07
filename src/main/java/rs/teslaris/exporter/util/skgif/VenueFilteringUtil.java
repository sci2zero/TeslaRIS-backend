package rs.teslaris.exporter.util.skgif;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.exporter.model.common.ExportPublicationType;

public class VenueFilteringUtil {

    public static final List<String> SUPPORTED_FILTERS = Arrays.asList(
        "identifiers.scheme", "identifiers.value", "title", "acronym", "type", "series",
        "access_rights.status", "creation_date", "contributions.by", "contributions.role"
    );


    public static void addQueryFilters(SKGIFFilterCriteria criteria, Query query) {
        criteria.getFilters().forEach((key, value) -> {
            switch (key) {
                case "identifiers.scheme":
                    if (value.equals("isbn")) {
                        query.addCriteria(
                            new Criteria().orOperator(
                                Criteria.where("e_isbn").exists(true),
                                Criteria.where("print_isbn").exists(true)
                            )
                        );
                    } else {
                        var fieldName = getIdentifierFieldName(value);
                        query.addCriteria(Criteria.where(fieldName).exists(true));
                    }
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
                case "title":
                    query.addCriteria(
                        Criteria.where("title").elemMatch(Criteria.where("content").is(value)));
                    break;
                case "acronym":
                    query.addCriteria(Criteria.where("name_abbreviation")
                        .elemMatch(Criteria.where("content").is(value)));
                    break;
                case "type":
                    query.addCriteria(Criteria.where("type").is(getEntityType(value)));
                    break;
                case "series":
                    query.addCriteria(
                        new Criteria().orOperator(
                            Criteria.where("event.name")
                                .elemMatch(Criteria.where("content").regex(value, "i")),
                            Criteria.where("publishers").elemMatch(Criteria.where("name")
                                .elemMatch(Criteria.where("content").regex(value, "i"))
                            )
                        ));
                    break;
                case "access_rights.status":
                    query.addCriteria(Criteria.where("open_access").is(getIsOpenAccess(value)));
                    break;
                case "creation_date":
                    addDateQuery(query, value);
                    break;
                case "contributions.by":
                    query.addCriteria(
                        new Criteria().orOperator(
                            Criteria.where("editors").elemMatch(Criteria.where("person.database_id")
                                .is(Integer.parseInt(IdentifierUtil.removeCommonPrefix(value)))),
                            Criteria.where("board_members")
                                .elemMatch(Criteria.where("person.database_id")
                                    .is(Integer.parseInt(IdentifierUtil.removeCommonPrefix(value))))
                        ));
                    break;
                case "contributions.role":
                    query.addCriteria(Criteria.where(getContributionField(value)).not().size(0));
                    break;
            }
        });
    }

    private static String getIdentifierFieldName(String skgifFieldName) {
        return switch (skgifFieldName) {
            case "doi" -> "doi";
            case "url" -> "scopus_id";
            case "openalex" -> "open_alex";
            case "eissn" -> "e_issn";
            case "issn" -> "print_issn";
            default -> "IGNORED";
        };
    }

    private static ExportPublicationType getEntityType(String type) {
        return switch (type) {
            case "conference" -> ExportPublicationType.PROCEEDINGS;
            case "book" -> ExportPublicationType.MONOGRAPH;
            case "journal" -> ExportPublicationType.JOURNAL;
            default -> throw new IllegalArgumentException("No local type for venue: " + type);
        };
    }

    private static boolean getIsOpenAccess(String accessRights) {
        accessRights = accessRights.toLowerCase();

        if (!List.of("open", "closed", "embargoed", "retricted", "unavailable")
            .contains(accessRights)) {
            throw new IllegalArgumentException("Non-recognised access rights: " + accessRights);
        }

        return accessRights.equals("open");
    }

    private static void addDateQuery(Query query, String dateString) {
        if (!dateString.contains("T")) {
            dateString += "T00:00:00";
        }

        var start = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .toLocalDate();
        var end = start.plusDays(1);

        query.addCriteria(Criteria.where("document_date").gte(start).lt(end));
    }

    private static String getContributionField(String contributionType) {
        contributionType = contributionType.toLowerCase();

        if (!List.of("editor", "scientific board member").contains(contributionType)) {
            throw new IllegalArgumentException(
                "Non-recognised contribution type: " + contributionType);
        }

        return contributionType.equals("editor") ? "editors" : "board_members";
    }
}
