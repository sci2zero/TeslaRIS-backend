package rs.teslaris.core.util.persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.commontypes.IdentifierConfiguration;

/**
 * JSON storage enforces no schema, so this catalogue is what the stored identifier configuration
 * is validated and completed against.
 */
public class IdentifierConfigurationCatalogue {

    private static final Map<ApplicableEntityType, Map<String, IdentifierConfiguration>>
        CONFIGURABLE_IDENTIFIERS =
        Collections.unmodifiableMap(
            new LinkedHashMap<ApplicableEntityType, Map<String, IdentifierConfiguration>>() {{
                put(ApplicableEntityType.PERSON, new LinkedHashMap<>() {{
                    put("apvnt", standard());
                    put("eCrisId", standard());
                    put("eNaukaId", standard());
                    put("nationalScienceId", standard());
                    put("scholarId", standard());
                    put("authenticusId", standard());
                    put("lattesId", standard());
                    put("orcid", featured());
                    put("scopusAuthorId", featured());
                    put("openAlexId", featured());
                    put("webOfScienceResearcherId", featured());
                }});
                put(ApplicableEntityType.ORGANISATION_UNIT, new LinkedHashMap<>() {{
                    put("scopusAfid", standard());
                    put("openAlexId", standard());
                    put("ror", standard());
                    put("ringgold", standard());
                    put("fundref", standard());
                    put("isni", standard());
                    put("fctId", standard());
                    put("grid", standard());
                    put("wikidata", standard());
                    put("nationalId", standard());
                    put("taxNumber", standard());
                }});
                put(ApplicableEntityType.PROJECT, new LinkedHashMap<>() {{
                    put("doi", featured());
                    put("raid", featured());
                    put("nationalId", featured());
                }});
                put(ApplicableEntityType.DOCUMENT, new LinkedHashMap<>() {{
                    put("doi", featured());
                    put("scopusId", standard());
                    put("openAlexId", standard());
                    put("webOfScienceId", standard());
                    put("handleId", standard());
                    put("arxivId", standard());
                    put("pubmedId", standard());
                    put("ssrnId", standard());
                    put("nationalId", standard());
                }});
            }});


    private IdentifierConfigurationCatalogue() {
    }

    private static IdentifierConfiguration standard() {
        return new IdentifierConfiguration(true, false, true);
    }

    /**
     * Featured by default, so that an identifier a landing page header used to hardcode keeps its
     * place until a deployment says otherwise.
     */
    private static IdentifierConfiguration featured() {
        return new IdentifierConfiguration(true, true, true);
    }

    public static Map<ApplicableEntityType, List<String>> configurableIdentifiers() {
        var names = new LinkedHashMap<ApplicableEntityType, List<String>>();
        CONFIGURABLE_IDENTIFIERS.forEach(
            (entityType, perIdentifier) -> names.put(entityType,
                List.copyOf(perIdentifier.keySet())));
        return Collections.unmodifiableMap(names);
    }

    public static boolean isConfigurable(ApplicableEntityType entityType, String identifier) {
        return CONFIGURABLE_IDENTIFIERS.getOrDefault(entityType, Map.of()).containsKey(identifier);
    }

    public static Map<ApplicableEntityType, Map<String, IdentifierConfiguration>> defaults() {
        var defaults =
            new LinkedHashMap<ApplicableEntityType, Map<String, IdentifierConfiguration>>();

        CONFIGURABLE_IDENTIFIERS.forEach((entityType, perIdentifier) -> {
            var copies = new LinkedHashMap<String, IdentifierConfiguration>();
            perIdentifier.forEach((identifier, policy) -> copies.put(identifier,
                new IdentifierConfiguration(policy.getHideWhenNotSet(), policy.getFeatured(),
                    policy.getCanBeUpdated())));
            defaults.put(entityType, copies);
        });

        return defaults;
    }

    public static Map<ApplicableEntityType, Map<String, IdentifierConfiguration>> withDefaults(
        Map<ApplicableEntityType, Map<String, IdentifierConfiguration>> stored) {
        var completed = defaults();

        if (Objects.isNull(stored)) {
            return completed;
        }

        completed.forEach((entityType, perIdentifier) -> {
            var storedPerIdentifier = stored.get(entityType);

            if (Objects.isNull(storedPerIdentifier)) {
                return;
            }

            perIdentifier.replaceAll((identifier, defaultConfiguration) ->
                Objects.requireNonNullElse(storedPerIdentifier.get(identifier),
                    defaultConfiguration));
        });

        return completed;
    }
}
