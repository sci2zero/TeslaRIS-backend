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

    private static final Map<ApplicableEntityType, List<String>> CONFIGURABLE_IDENTIFIERS =
        Collections.unmodifiableMap(new LinkedHashMap<>() {{
            put(ApplicableEntityType.PERSON, List.of(
                "apvnt",
                "eCrisId",
                "eNaukaId",
                "nationalScienceId",
                "scholarId",
                "authenticusId",
                "lattesId",
                "orcid",
                "scopusAuthorId",
                "openAlexId",
                "webOfScienceResearcherId"
            ));
            put(ApplicableEntityType.ORGANISATION_UNIT, List.of(
                "scopusAfid",
                "openAlexId",
                "ror",
                "ringgold",
                "fundref",
                "isni",
                "fctId",
                "grid",
                "wikidata",
                "nationalId",
                "taxNumber"
            ));
            put(ApplicableEntityType.DOCUMENT, List.of(
                "doi",
                "scopusId",
                "openAlexId",
                "webOfScienceId",
                "handleId",
                "arxivId",
                "pubmedId",
                "ssrnId",
                "nationalId"
            ));
        }});


    private IdentifierConfigurationCatalogue() {
    }

    public static Map<ApplicableEntityType, List<String>> configurableIdentifiers() {
        return CONFIGURABLE_IDENTIFIERS;
    }

    public static boolean isConfigurable(ApplicableEntityType entityType, String identifier) {
        return CONFIGURABLE_IDENTIFIERS.getOrDefault(entityType, List.of()).contains(identifier);
    }

    public static Map<ApplicableEntityType, Map<String, IdentifierConfiguration>> defaults() {
        var defaults =
            new LinkedHashMap<ApplicableEntityType, Map<String, IdentifierConfiguration>>();

        CONFIGURABLE_IDENTIFIERS.forEach((entityType, identifiers) -> {
            var perIdentifier = new LinkedHashMap<String, IdentifierConfiguration>();
            identifiers.forEach(identifier -> perIdentifier.put(identifier,
                new IdentifierConfiguration()));
            defaults.put(entityType, perIdentifier);
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
