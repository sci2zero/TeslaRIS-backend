package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.commontypes.IdentifierConfiguration;
import rs.teslaris.core.util.persistence.IdentifierConfigurationCatalogue;

public class IdentifierConfigurationCatalogueTest {

    @Test
    void shouldCoverPersonOrganisationUnitAndDocument() {
        var catalogue = IdentifierConfigurationCatalogue.configurableIdentifiers();

        assertEquals(3, catalogue.size());
        assertEquals(11, catalogue.get(ApplicableEntityType.PERSON).size());
        assertEquals(11, catalogue.get(ApplicableEntityType.ORGANISATION_UNIT).size());
        assertEquals(9, catalogue.get(ApplicableEntityType.DOCUMENT).size());
    }

    @Test
    void shouldScopeIdentifiersToTheirEntityType() {
        assertTrue(IdentifierConfigurationCatalogue.isConfigurable(ApplicableEntityType.PERSON,
            "orcid"));
        assertTrue(IdentifierConfigurationCatalogue.isConfigurable(ApplicableEntityType.DOCUMENT,
            "doi"));

        assertFalse(IdentifierConfigurationCatalogue.isConfigurable(ApplicableEntityType.DOCUMENT,
            "orcid"));
        assertFalse(IdentifierConfigurationCatalogue.isConfigurable(ApplicableEntityType.PERSON,
            "doi"));
        assertFalse(IdentifierConfigurationCatalogue.isConfigurable(ApplicableEntityType.PERSON,
            "madeUpIdentifier"));

        assertFalse(IdentifierConfigurationCatalogue.isConfigurable(ApplicableEntityType.EVENT,
            "openAlexId"));
    }

    @Test
    void shouldShareNoDefaultPolicyInstanceBetweenIdentifiers() {
        var defaults = IdentifierConfigurationCatalogue.defaults();

        defaults.get(ApplicableEntityType.PERSON).get("orcid").setFeatured(true);

        assertFalse(defaults.get(ApplicableEntityType.PERSON).get("apvnt").getFeatured());
        assertFalse(defaults.get(ApplicableEntityType.DOCUMENT).get("doi").getFeatured());
    }

    @Test
    void shouldDefaultToTodaysBehaviour() {
        var policy = IdentifierConfigurationCatalogue.defaults()
            .get(ApplicableEntityType.PERSON).get("orcid");

        assertTrue(policy.getHideWhenNotSet());
        assertFalse(policy.getFeatured());
        assertTrue(policy.getCanBeUpdated());
    }

    @Test
    void shouldCompleteAStoredConfigurationWithoutTouchingItsOverrides() {
        var stored = new HashMap<ApplicableEntityType, Map<String, IdentifierConfiguration>>();
        stored.put(ApplicableEntityType.PERSON,
            new HashMap<>(Map.of("orcid", new IdentifierConfiguration(false, true, false))));

        var completed = IdentifierConfigurationCatalogue.withDefaults(stored);

        var orcid = completed.get(ApplicableEntityType.PERSON).get("orcid");
        assertFalse(orcid.getHideWhenNotSet());
        assertTrue(orcid.getFeatured());
        assertFalse(orcid.getCanBeUpdated());

        assertTrue(completed.get(ApplicableEntityType.PERSON).get("apvnt").getHideWhenNotSet());
        assertEquals(9, completed.get(ApplicableEntityType.DOCUMENT).size());
    }

    @Test
    void shouldDropStoredIdentifiersAndEntityTypesOutsideTheCatalogue() {
        var stored = new HashMap<ApplicableEntityType, Map<String, IdentifierConfiguration>>();
        stored.put(ApplicableEntityType.PERSON,
            new HashMap<>(Map.of("identifierThatLeftTheCatalogue",
                new IdentifierConfiguration(false, true, false))));
        stored.put(ApplicableEntityType.JOURNAL,
            new HashMap<>(Map.of("eissn", new IdentifierConfiguration(false, true, false))));

        var completed = IdentifierConfigurationCatalogue.withDefaults(stored);

        assertEquals(3, completed.size());
        assertFalse(completed.containsKey(ApplicableEntityType.JOURNAL));
        assertFalse(completed.get(ApplicableEntityType.PERSON)
            .containsKey("identifierThatLeftTheCatalogue"));
    }

    @Test
    void shouldAnswerFullDefaultsForAnAbsentConfiguration() {
        var completed = IdentifierConfigurationCatalogue.withDefaults(null);

        assertEquals(3, completed.size());
        assertNotNull(completed.get(ApplicableEntityType.ORGANISATION_UNIT).get("ror"));
    }

    @Test
    void shouldNotAllowTheCatalogueToBeEdited() {
        assertThrows(UnsupportedOperationException.class,
            () -> IdentifierConfigurationCatalogue.configurableIdentifiers()
                .put(ApplicableEntityType.EVENT, List.of("openAlexId")));
    }
}
