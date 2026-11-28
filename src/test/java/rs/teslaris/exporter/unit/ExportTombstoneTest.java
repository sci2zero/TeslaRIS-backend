package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.exporter.model.converter.ExportOrganisationUnitConverter;

public class ExportTombstoneTest {

    private static OrganisationUnit organisationUnit(Boolean deleted,
                                                     Boolean publicationCandidate) {
        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(4);
        organisationUnit.setDeleted(deleted);
        organisationUnit.setPublicationCandidate(publicationCandidate);
        organisationUnit.setName(new HashSet<>(Set.of(
            new MultiLingualContent(new LanguageTag("EN", "English"), "Faculty of Sciences",
                1))));
        return organisationUnit;
    }

    @Test
    public void shouldExportRecordThatIsAPublicationCandidate() {
        // When
        var snapshot = ExportOrganisationUnitConverter.toCommonExportModel(
            organisationUnit(false, true), false);

        // Then
        assertFalse(snapshot.getDeleted());
        assertFalse(snapshot.getName().isEmpty());
    }

    @Test
    public void shouldExportRecordThatWasNotAssessedYet() {
        // When
        var snapshot = ExportOrganisationUnitConverter.toCommonExportModel(
            organisationUnit(false, null), false);

        // Then
        assertFalse(snapshot.getDeleted());
        assertFalse(snapshot.getName().isEmpty());
    }

    @Test
    public void shouldExportRecordThatIsNoPublicationCandidateAsTombstone() {
        // When
        var snapshot = ExportOrganisationUnitConverter.toCommonExportModel(
            organisationUnit(false, false), false);

        // Then
        assertTrue(snapshot.getDeleted());
        assertTrue(snapshot.getName().isEmpty());
        assertEquals(4, snapshot.getDatabaseId());
    }

    @Test
    public void shouldExportDeletedRecordAsTombstone() {
        // When
        var snapshot = ExportOrganisationUnitConverter.toCommonExportModel(
            organisationUnit(true, true), false);

        // Then
        assertTrue(snapshot.getDeleted());
        assertTrue(snapshot.getName().isEmpty());
    }
}
