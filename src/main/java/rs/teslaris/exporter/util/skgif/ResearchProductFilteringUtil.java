package rs.teslaris.exporter.util.skgif;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportPublicationType;

public class ResearchProductFilteringUtil {

    public static final List<String> SUPPORTED_FILTERS = Arrays.asList(
        "identifiers.scheme", "identifiers.value", "titles", "abstracts", "product_type",
        "contributions.by", "contributions.declared_affiliations", "contributions.role",
        "contributions.contribution_types", "manifestations.type.class",
        "manifestations.dates.modified", "manifestations.dates.distribution",
        "manifestations.license", "biblio.issue", "biblio.volume", "biblio.pages.first",
        "biblio.pages.last", "biblio.in", "contributions.by.identifiers.value",
        "relevant_organisations"
    );


    public static void addQueryFilters(SKGIFFilterCriteria criteria, Query query) {
        var criteriaList = new ArrayList<Criteria>();

        criteria.getFilters().forEach((key, value) -> {
            switch (key) {
                case "identifiers.scheme":
                    if (value.equals("isbn")) {
                        criteriaList.add(
                            new Criteria().orOperator(
                                Criteria.where("e_isbn").exists(true),
                                Criteria.where("print_isbn").exists(true)
                            )
                        );
                    } else {
                        var fieldName = getIdentifierFieldName(value);
                        criteriaList.add(Criteria.where(fieldName).exists(true));
                    }
                    break;
                case "identifiers.value":
                    value = StringUtil.normalizeIdentifier(value);
                    criteriaList.add(
                        new Criteria().orOperator(
                            Criteria.where("orcid").is(value),
                            Criteria.where("scopus_afid").is(value),
                            Criteria.where("open_alex").is(value)
                        )
                    );
                    break;
                case "titles":
                    criteriaList.add(
                        Criteria.where("title").elemMatch(Criteria.where("content").is(value)));
                    break;
                case "abstracts":
                    criteriaList.add(
                        Criteria.where("description")
                            .elemMatch(Criteria.where("content").regex(value, "i")));
                    break;
                case "product_type":
                    addProductTypeQuery(criteriaList, value);
                    break;
                case "contributions.by":
                    criteriaList.add(
                        new Criteria().orOperator(
                            Criteria.where("authors").elemMatch(Criteria.where("person.database_id")
                                .is(Integer.parseInt(IdentifierUtil.removeCommonPrefix(value)))),
                            Criteria.where("editors").elemMatch(Criteria.where("person.database_id")
                                .is(Integer.parseInt(IdentifierUtil.removeCommonPrefix(value)))),
                            Criteria.where("advisor")
                                .elemMatch(Criteria.where("person.database_id")
                                    .is(Integer.parseInt(IdentifierUtil.removeCommonPrefix(value))))
                        ));
                    break;
                case "contributions.declared_affiliations":
                    criteriaList.add(
                        new Criteria().orOperator(
                            Criteria.where("authors").elemMatch(
                                Criteria.where("declared_contributions").in(Integer.parseInt(
                                    IdentifierUtil.removeCommonPrefix(value)))),
                            Criteria.where("editors").elemMatch(
                                Criteria.where("declared_contributions").in(Integer.parseInt(
                                    IdentifierUtil.removeCommonPrefix(value)))),
                            Criteria.where("advisor").elemMatch(
                                Criteria.where("declared_contributions")
                                    .in(Integer.parseInt(IdentifierUtil.removeCommonPrefix(value))))
                        ));
                    break;
                case "contributions.role":
                    criteriaList.add(Criteria.where(getContributionField(value)).not().size(0));
                    break;
                case "contributions.contribution_types":
                    addContributionTypeQuery(criteriaList, value);
                    break;
                case "manifestations.type.class":
                    criteriaList.add(
                        Criteria.where("document_files")
                            .elemMatch(Criteria.where("type").is(getResourceType(value))));
                    break;
                case "manifestations.dates.modified":
                    addManifestationDateQuery(criteriaList, value, "last_updated");
                    break;
                case "manifestations.dates.distribution":
                    addManifestationDateQuery(criteriaList, value, "creation_date");
                    break;
                case "manifestations.license":
                    var license = value.replace("https://creativecommons.org/licenses/", "")
                        .replace("/4.0/legalcode.en", "").replace("-", "_").toUpperCase();
                    criteriaList.add(
                        Criteria.where("document_files")
                            .elemMatch(Criteria.where("license").is(License.valueOf(license))));
                    break;
                case "biblio.issue":
                    criteriaList.add(Criteria.where("issue").is(value));
                    break;
                case "biblio.volume":
                    criteriaList.add(Criteria.where("volume").is(value));
                    break;
                case "biblio.pages.first":
                    criteriaList.add(Criteria.where("start_page").is(value));
                    break;
                case "biblio.pages.last":
                    criteriaList.add(Criteria.where("end_page").is(value));
                    break;
                case "biblio.in":
                    criteriaList.add(
                        new Criteria().orOperator(
                            Criteria.where("journal.title")
                                .elemMatch(Criteria.where("content").regex(value, "i")),
                            Criteria.where("proceedings.title")
                                .elemMatch(Criteria.where("content").regex(value, "i")),
                            Criteria.where("monograph.title")
                                .elemMatch(Criteria.where("content").regex(value, "i"))
                        )
                    );
                    break;
                case "contributions.by.identifiers.value":
                    criteriaList.add(
                        new Criteria().orOperator(
                            Criteria.where("authors")
                                .elemMatch(Criteria.where("person.orcid").is(value)),
                            Criteria.where("editors")
                                .elemMatch(Criteria.where("person.orcid").is(value)),
                            Criteria.where("advisor")
                                .elemMatch(Criteria.where("person.orcid").is(value)),
                            Criteria.where("authors")
                                .elemMatch(Criteria.where("person.scopus_id").is(value)),
                            Criteria.where("editors")
                                .elemMatch(Criteria.where("person.scopus_id").is(value)),
                            Criteria.where("advisor")
                                .elemMatch(Criteria.where("person.scopus_id").is(value)),
                            Criteria.where("authors")
                                .elemMatch(Criteria.where("person.open_alex").is(value)),
                            Criteria.where("editors")
                                .elemMatch(Criteria.where("person.open_alex").is(value)),
                            Criteria.where("advisor")
                                .elemMatch(Criteria.where("person.open_alex").is(value))
                        ));
                    break;
                case "relevant_organisations":
                    criteriaList.add(Criteria.where("related_institution_ids")
                        .in(IdentifierUtil.removeCommonPrefix(value)));
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
            case "doi" -> "doi";
            case "url" -> "scopus_id";
            case "openalex" -> "open_alex";
            case "eissn" -> "e_issn";
            case "issn" -> "print_issn";
            default -> "IGNORED";
        };
    }

    private static void addProductTypeQuery(List<Criteria> criteriaList, String productType) {
        productType = productType.toLowerCase();

        if (!List.of("literature", "research data", "research software").contains(productType)) {
            throw new IllegalArgumentException("Non-recognised product type: " + productType);
        }

        switch (productType) {
            case "literature" -> criteriaList.add(
                new Criteria().orOperator(
                    Criteria.where("type").is(ExportPublicationType.JOURNAL_PUBLICATION),
                    Criteria.where("type").is(ExportPublicationType.PROCEEDINGS_PUBLICATION),
                    Criteria.where("type").is(ExportPublicationType.MONOGRAPH_PUBLICATION),
                    Criteria.where("type").is(ExportPublicationType.THESIS)
                )
            );
            case "research software" -> criteriaList.add(
                Criteria.where("type").is(ExportPublicationType.INTANGIBLE_PRODUCT));
            case "research data" ->
                criteriaList.add(Criteria.where("type").is(ExportPublicationType.DATASET));
            default ->
                throw new IllegalArgumentException("No RP entity type type for: " + productType);
        }
    }

    private static String getContributionField(String contributionType) {
        contributionType = contributionType.toLowerCase();

        if (!List.of("author", "editor", "advisor").contains(contributionType)) {
            throw new IllegalArgumentException(
                "Non-recognised contribution type: " + contributionType);
        }

        return contributionType.replace(" ", "_") + "s";
    }

    private static void addContributionTypeQuery(List<Criteria> criteriaList,
                                                 String contributionType) {
        contributionType = contributionType.toLowerCase();

        if (!List.of("writing - original draft", "conceptualization", "writing - review & editing",
            "supervision", "validation").contains(contributionType)) {
            throw new IllegalArgumentException(
                "Non-recognised contribution type: " + contributionType);
        }

        switch (contributionType) {
            case "writing - original draft", "conceptualization" ->
                criteriaList.add(Criteria.where("authors").not().size(0));
            case "writing - review & editing" ->
                criteriaList.add(Criteria.where("editors").not().size(0));
            case "supervision", "validation" ->
                criteriaList.add(Criteria.where("advisors").not().size(0));
            default -> throw new IllegalArgumentException(
                "No RP contribution type for: " + contributionType);
        }
    }

    public static ResourceType getResourceType(String fabioType) {
        return switch (fabioType) {
            case "http://purl.org/spar/fabio/Preprint" -> ResourceType.PREPRINT;
            case "http://purl.org/spar/fabio/Article" -> ResourceType.OFFICIAL_PUBLICATION;
            case "http://purl.org/spar/fabio/SupplementaryFile" -> ResourceType.SUPPLEMENT;
            case "http://purl.org/spar/fabio/Proof" -> ResourceType.PROOF;
            case "http://purl.org/spar/fabio/Image" -> ResourceType.IMAGE;
            case "http://purl.org/spar/fabio/Statement" -> ResourceType.STATEMENT;
            case "http://purl.org/spar/fabio/ConflictOfInterestStatement" ->
                ResourceType.ADVISOR_CONFLICT_OF_INTEREST;
            default -> throw new IllegalArgumentException("No resource type for: " + fabioType);
        };
    }

    private static void addManifestationDateQuery(List<Criteria> criteriaList, String dateString,
                                                  String field) {
        if (!dateString.contains("T")) {
            dateString += "T00:00:00";
        }

        var start = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .toLocalDate();
        var end = start.plusDays(1);

        criteriaList.add(
            Criteria.where("document_files")
                .elemMatch(Criteria.where(field).gte(start).lt(end)));
    }
}
