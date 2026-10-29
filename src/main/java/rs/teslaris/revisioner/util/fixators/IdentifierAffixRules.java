package rs.teslaris.revisioner.util.fixators;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Every identifier shape the fixators know, with the resolver prefixes its value is routinely
 * pasted with.
 */
public final class IdentifierAffixRules {

    public static final Rule DOI =
        rule("10\\.\\d{4,9}/[-,._;()/:A-Za-z0-9]+", Casing.AS_IS, "https://doi.org/",
            "http://doi.org/", "https://dx.doi.org/", "http://dx.doi.org/", "doi:");

    public static final Rule DOCUMENT_SCOPUS_ID =
        rule("\\d{6,12}", Casing.AS_IS, "https://www.scopus.com/inward/record.uri?eid=2-s2.0-",
            "2-s2.0-");

    public static final Rule DOCUMENT_OPEN_ALEX_ID =
        rule("W\\d{4,10}", Casing.UPPER, "https://openalex.org/",
            "https://api.openalex.org/works/");

    public static final Rule WEB_OF_SCIENCE_ID = rule("\\d{15}", Casing.AS_IS, "WOS:");

    public static final Rule HANDLE_ID =
        rule("\\d{2}\\.\\d{3,}\\.\\d+/\\S+", Casing.AS_IS, "https://hdl.handle.net/",
            "http://hdl.handle.net/", "hdl:");

    public static final Rule ARXIV_ID =
        rule("(?:\\d{4}\\.\\d{4,5}|[a-z\\-]+/\\d{7})", Casing.AS_IS, "https://arxiv.org/abs/",
            "http://arxiv.org/abs/", "arxiv:");

    public static final Rule PUBMED_ID =
        rule("\\d{1,8}", Casing.AS_IS, "https://pubmed.ncbi.nlm.nih.gov/", "pmid:");

    public static final Rule SSRN_ID =
        rule("\\d+", Casing.AS_IS, "https://papers.ssrn.com/sol3/papers.cfm?abstract_id=",
            "https://ssrn.com/abstract=");

    public static final Rule ORCID =
        rule("\\d{4}-\\d{4}-\\d{4}-[\\dX]{4}", Casing.UPPER, "https://orcid.org/",
            "http://orcid.org/", "orcid:");

    public static final Rule SCOPUS_AUTHOR_ID =
        rule("\\d+", Casing.AS_IS, "https://www.scopus.com/authid/detail.uri?authorId=");

    public static final Rule PERSON_OPEN_ALEX_ID =
        rule("A\\d{4,10}", Casing.UPPER, "https://openalex.org/",
            "https://api.openalex.org/authors/");

    public static final Rule WEB_OF_SCIENCE_RESEARCHER_ID =
        rule("[A-Z]{1,3}-\\d{4}-\\d{4}", Casing.UPPER,
            "https://www.webofscience.com/wos/author/record/");

    public static final Rule SCHOLAR_ID =
        rule("[a-zA-Z0-9_-]{12,}", Casing.AS_IS, "https://scholar.google.com/citations?user=");

    public static final Rule AUTHENTICUS_ID =
        rule("A-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}", Casing.UPPER, "https://www.authenticus.pt/");

    public static final Rule LATTES_ID =
        rule("\\d{16}", Casing.AS_IS, "http://lattes.cnpq.br/", "https://lattes.cnpq.br/");

    public static final Rule SCOPUS_AFID =
        rule("\\d+", Casing.AS_IS, "https://www.scopus.com/affil/profile.uri?afid=");

    public static final Rule ORGANISATION_UNIT_OPEN_ALEX_ID =
        rule("I\\d{4,10}", Casing.UPPER, "https://openalex.org/",
            "https://api.openalex.org/institutions/");

    public static final Rule ROR =
        rule("0[0-9a-hj-km-np-z]{8}", Casing.LOWER, "https://ror.org/", "http://ror.org/");

    public static final Rule RINGGOLD = rule("[0-9]+", Casing.AS_IS);

    public static final Rule FUNDREF =
        rule("[0-9]+", Casing.AS_IS, "https://doi.org/10.13039/", "10.13039/");

    public static final Rule ISNI = rule("[0-9X]{16}", Casing.UPPER, "https://isni.org/isni/");

    public static final Rule GRID =
        rule("grid\\.\\d{4,6}\\.[0-9a-f]{1,2}", Casing.LOWER, "https://grid.ac/institutes/");

    public static final Rule WIKIDATA =
        rule("Q[1-9]\\d*", Casing.UPPER, "https://www.wikidata.org/wiki/",
            "https://www.wikidata.org/entity/");

    public static final Rule ISSN =
        rule("(?:\\d{4}-\\d{4}|\\d{4}-\\d{3}[\\dX]?)", Casing.UPPER,
            "https://portal.issn.org/resource/ISSN/", "issn:");

    public static final Rule PUBLICATION_SERIES_OPEN_ALEX_ID =
        rule("[SV]\\d{4,10}", Casing.UPPER, "https://openalex.org/",
            "https://api.openalex.org/sources/");

    public static final Rule CONFERENCE_OPEN_ALEX_ID =
        rule("S\\d{4,10}", Casing.UPPER, "https://openalex.org/",
            "https://api.openalex.org/sources/");

    public static final Rule CONF_ID = rule(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        Casing.LOWER);

    private static final Pattern LEADING_AFFIX = Pattern.compile("^[\\s\"'“‘(\\[{<]+");

    private static final Pattern TRAILING_AFFIX = Pattern.compile("[\\s\"'”’)\\]}>.,;:]+$");

    private static final Pattern TRAILING_SLASH = Pattern.compile("/+$");


    private IdentifierAffixRules() {
    }

    /**
     * The stored value with its resolver prefix and leftover punctuation gone, or the value
     * untouched when stripping those does not produce something the entity would accept.
     */
    public static String clean(Rule rule, String storedValue) {
        if (Objects.isNull(storedValue) || storedValue.isBlank()) {
            return storedValue;
        }

        var candidate = trimAffixes(stripResolverPrefix(rule, storedValue));
        return rule.value().matcher(candidate).matches() ? rule.casing().apply(candidate) :
            storedValue;
    }

    /**
     * The first identifier of this shape carried by one of the given texts, or null when none of
     * them spells one out behind a resolver prefix.
     */
    @Nullable
    public static String harvest(Rule rule, Collection<String> texts) {
        if (Objects.isNull(rule.search()) || Objects.isNull(texts)) {
            return null;
        }

        for (var text : texts) {
            if (Objects.isNull(text) || text.isBlank()) {
                continue;
            }

            var matcher = rule.search().matcher(text);
            while (matcher.find()) {
                var candidate =
                    trimAffixes(TRAILING_SLASH.matcher(matcher.group(1)).replaceAll(""));

                if (rule.value().matcher(candidate).matches()) {
                    return rule.casing().apply(candidate);
                }
            }
        }

        return null;
    }

    private static String stripResolverPrefix(Rule rule, String value) {
        var trimmed = value.strip();

        for (var prefix : rule.prefixes()) {
            if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return trimmed.substring(prefix.length());
            }
        }

        return trimmed;
    }

    private static String trimAffixes(String value) {
        return TRAILING_AFFIX.matcher(LEADING_AFFIX.matcher(value).replaceAll("")).replaceAll("");
    }

    private static Rule rule(String body, Casing casing, String... prefixes) {
        var prefixList = List.of(prefixes);
        var search = prefixList.isEmpty() ? null : Pattern.compile(
            "(?:" + prefixList.stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")("
                + body + ")", Pattern.CASE_INSENSITIVE);

        return new Rule(Pattern.compile("^" + body + "$", Pattern.CASE_INSENSITIVE), prefixList,
            search, casing);
    }

    public enum Casing {
        AS_IS,
        UPPER,
        LOWER;

        String apply(String value) {
            return switch (this) {
                case UPPER -> value.toUpperCase(Locale.ROOT);
                case LOWER -> value.toLowerCase(Locale.ROOT);
                default -> value;
            };
        }
    }

    public record Rule(Pattern value, List<String> prefixes, Pattern search, Casing casing) {
    }
}
