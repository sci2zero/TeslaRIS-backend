package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingOutputExtractor;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
public class HydratorFundingOutputExtractorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private MigrationLog migrationLog;

    @InjectMocks
    private HydratorFundingOutputExtractor extractor;


    private HydratorCVModel.Curriculum curriculum(String groups) throws Exception {
        return MAPPER.readValue("""
            {"id": "cv-1", "fullName": "John Doe",
             "curriculum": {"language": "en", "groups": %s}}
            """.formatted(groups), HydratorCVModel.Curriculum.class);
    }

    @Test
    void shouldMapPairToFundingAndOutputKeysWhenBothIdsArePresent() throws Exception {
        // given
        var record = curriculum("""
            {"total": 1, "fundingOutputs": {"fundingOutput": [
              {"funding": {"id": "4667", "title": "Test Project"},
               "output": {"id": "42004", "title": "Test Article"}}]}}
            """);

        // when
        var result = extractor.extract(record);

        // then
        assertEquals(1, result.size());
        var dto = result.getFirst();
        assertEquals("cv-1#funding-output#4667-42004", dto.sourceKey());
        assertEquals("cv-1#funding#4667", dto.fundingKey());
        assertEquals("cv-1#output#42004", dto.outputKey());
    }

    @Test
    void shouldLogPairWhenAnIdIsMissing() throws Exception {
        // given
        var record = curriculum("""
            {"total": 1, "fundingOutputs": {"fundingOutput": [
              {"funding": {"id": "4667", "title": "Test Project"},
               "output": {"title": "Test Article"}}]}}
            """);

        // when
        var result = extractor.extract(record);

        // then
        assertTrue(result.isEmpty());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT_DOCUMENT"), eq("cv-1"),
            eq("CW-001-NO-RULE"), anyString());
    }

    @Test
    void shouldReturnNothingWhenCurriculumHasNoGroups() throws Exception {
        // given
        var record = MAPPER.readValue("""
            {"id": "cv-1", "curriculum": {"language": "en"}}
            """, HydratorCVModel.Curriculum.class);

        // when
        var result = extractor.extract(record);

        // then
        assertTrue(result.isEmpty());
    }
}
