package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rs.teslaris.core.export.ProjectExportSource;
import rs.teslaris.core.model.commontypes.Currency;
import rs.teslaris.core.model.commontypes.MonetaryAmount;
import rs.teslaris.core.model.funding.FundingType;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.exporter.model.converter.ExportFundingConverter;
import rs.teslaris.exporter.model.converter.ExportProjectConverter;

public class ExportProjectFundingConverterTest {

    private static ProjectExportSource.ProjectRecord.ProjectRecordBuilder project() {
        return ProjectExportSource.ProjectRecord.builder()
            .id(1)
            .deleted(false)
            .oldIds(Set.of("123", "legacy-456"))
            .raid("raid-1")
            .nationalId("national-1")
            .identifiers(List.of(
                new ProjectExportSource.IdentifierRecord("CODE", "value", "https://x/{id}")))
            .name(Set.of())
            .description(Set.of())
            .nameAbbreviation(Set.of())
            .keywords(Set.of())
            .uris(Set.of("https://project.example"))
            .persons(List.of(new ProjectExportSource.PersonContributionRecord(null, "Jane Doe", 2,
                PersonProjectContributionType.PRINCIPLE_INVESTIGATOR, null, List.of(7), null,
                null)))
            .organisations(List.of())
            .institutionIds(Set.of(7, 8));
    }

    private static ProjectExportSource.FundingRecord.FundingRecordBuilder funding() {
        var currency = new Currency();
        currency.setCode("EUR");

        return ProjectExportSource.FundingRecord.builder()
            .id(2)
            .deleted(false)
            .oldIds(Set.of())
            .grantAgreementId("101095129")
            .identifiers(List.of())
            .name(Set.of())
            .description(Set.of())
            .nameAbbreviation(Set.of())
            .keywords(Set.of())
            .displayFunder(Set.of())
            .displayCall(Set.of())
            .displayProgram(Set.of())
            .fundingTypes(Set.of(FundingType.GRANT))
            .amount(new MonetaryAmount(1000.5, currency))
            .dateFrom(LocalDate.of(2023, 1, 1))
            .institutionIds(Set.of(7, 8, 9));
    }

    @Test
    void shouldConvertProject() {
        // When
        var result = ExportProjectConverter.toCommonExportModel(project().build(), true);

        // Then
        assertEquals(1, result.getDatabaseId());
        assertFalse(result.getDeleted());
        assertEquals("raid-1", result.getRaid());
        assertEquals("national-1", result.getNationalId());
        assertEquals(Set.of(123), result.getOldIds());
        assertEquals("CODE", result.getIdentifiers().getFirst().getCode());
        assertEquals(List.of("https://project.example"), result.getUris());
        assertEquals("Jane Doe", result.getPersons().getFirst().getDisplayName());
        assertEquals(2, result.getPersons().getFirst().getOrderNumber());
        assertEquals(List.of(7), result.getPersons().getFirst().getDeclaredContributions());
        assertNull(result.getPersons().getFirst().getPerson());
        assertEquals(Set.of(7, 8), result.getRelatedInstitutionIds());
        assertEquals(Set.of(7, 8), result.getActivelyRelatedInstitutionIds());
    }

    @Test
    void shouldLeaveOutRelationsWhenNotComputingThem() {
        // When
        var result = ExportProjectConverter.toCommonExportModel(project().build(), false);

        // Then
        assertTrue(result.getRelatedInstitutionIds().isEmpty());
        assertTrue(result.getActivelyRelatedInstitutionIds().isEmpty());
    }

    @Test
    void shouldOnlyKeepBaseFieldsForDeletedProject() {
        // Given
        var deleted = ProjectExportSource.ProjectRecord.builder().id(1).deleted(true).build();

        // When
        var result = ExportProjectConverter.toCommonExportModel(deleted, true);

        // Then
        assertEquals(1, result.getDatabaseId());
        assertTrue(result.getDeleted());
        assertTrue(result.getName().isEmpty());
    }

    @Test
    void shouldConvertFundingWithEmbeddedProject() {
        // Given
        var record = funding().project(project().build()).build();

        // When
        var result = ExportFundingConverter.toCommonExportModel(record, true);

        // Then
        assertEquals(2, result.getDatabaseId());
        assertEquals("101095129", result.getGrantAgreementId());
        assertEquals(1000.5, result.getAmount().getAmount());
        assertEquals("EUR", result.getAmount().getCurrencyCode());
        assertEquals(Set.of(FundingType.GRANT), result.getFundingTypes());
        assertEquals(LocalDate.of(2023, 1, 1), result.getDateFrom());
        assertEquals(Set.of(7, 8, 9), result.getRelatedInstitutionIds());
        assertEquals(1, result.getProject().getDatabaseId());
        assertEquals("raid-1", result.getProject().getRaid());
        assertTrue(result.getProject().getRelatedInstitutionIds().isEmpty());
    }

    @Test
    void shouldConvertFundingWithoutProject() {
        // When
        var result = ExportFundingConverter.toCommonExportModel(funding().build(), true);

        // Then
        assertNull(result.getProject());
        assertNull(result.getFunder());
        assertTrue(result.getFundingCallName().isEmpty());
        assertTrue(result.getFundingProgramName().isEmpty());
    }

    @Test
    void shouldOnlyKeepBaseFieldsForDeletedFunding() {
        // Given
        var deleted = ProjectExportSource.FundingRecord.builder().id(2).deleted(true).build();

        // When
        var result = ExportFundingConverter.toCommonExportModel(deleted, true);

        // Then
        assertEquals(2, result.getDatabaseId());
        assertTrue(result.getDeleted());
        assertNull(result.getAmount());
    }
}
