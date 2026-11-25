package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.exporter.model.converter.ExportResearchAreaConverter;

public class ExportResearchAreaConverterTest {

    private static ResearchArea researchArea(Integer id) {
        var researchArea = new ResearchArea();
        researchArea.setId(id);
        return researchArea;
    }

    private static MultiLingualContent english(String content) {
        var language = new LanguageTag();
        language.setLanguageTag("EN");
        var multiLingualContent = new MultiLingualContent();
        multiLingualContent.setLanguage(language);
        multiLingualContent.setContent(content);
        multiLingualContent.setPriority(1);
        return multiLingualContent;
    }

    @Test
    public void shouldConvertResearchArea() {
        // Given
        var researchArea = researchArea(7);
        researchArea.setName(new HashSet<>(Set.of(english("Computer science"))));
        researchArea.setDescription(new HashSet<>(Set.of(english("Study of computation"))));
        researchArea.setSuperResearchArea(researchArea(3));
        researchArea.setSchemeName("ERC");

        // When
        var result = ExportResearchAreaConverter.toCommonExportModel(researchArea, true);

        // Then
        assertEquals(7, result.getDatabaseId());
        assertEquals("Computer science", result.getName().getFirst().getContent());
        assertEquals("Study of computation", result.getDescription().getFirst().getContent());
        assertEquals(3, result.getSuperResearchAreaId());
        assertEquals("ERC", result.getSchemeName());
    }

    @Test
    public void shouldKeepOnlyBaseFieldsOfDeletedResearchArea() {
        // Given
        var researchArea = researchArea(7);
        researchArea.setDeleted(true);
        researchArea.setName(new HashSet<>(Set.of(english("Computer science"))));
        researchArea.setSchemeName("ERC");

        // When
        var result = ExportResearchAreaConverter.toCommonExportModel(researchArea, true);

        // Then
        assertTrue(result.getDeleted());
        assertTrue(result.getName().isEmpty());
        assertNull(result.getSchemeName());
    }

    @Test
    public void shouldGiveSortedDistinctResearchAreaIds() {
        // Given
        var researchAreas = new ArrayList<>(Arrays.asList(researchArea(9), researchArea(4),
            null, researchArea(null), researchArea(9)));

        // When / Then
        assertEquals(List.of(4, 9), ExportResearchAreaConverter.researchAreaIds(researchAreas));
        assertEquals(List.of(), ExportResearchAreaConverter.researchAreaIds(null));
    }
}
