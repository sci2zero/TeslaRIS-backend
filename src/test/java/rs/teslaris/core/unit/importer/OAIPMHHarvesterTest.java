package rs.teslaris.core.unit.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.importer.service.impl.OAIPMHHarvesterImpl;
import rs.teslaris.importer.utility.oaipmh.OAIPMHHarvestConfigurationLoader;

@SpringBootTest
public class OAIPMHHarvesterTest {

    @InjectMocks
    private OAIPMHHarvesterImpl oaipmhHarvester;

    @Test
    void shouldReturnSourceNames() {
        try (MockedStatic<OAIPMHHarvestConfigurationLoader> mocked = mockStatic(
            // Given
            OAIPMHHarvestConfigurationLoader.class)) {
            List<String> mockSources = List.of("source1", "source2");
            mocked.when(OAIPMHHarvestConfigurationLoader::getAllSourceNames)
                .thenReturn(mockSources);

            // When
            var result = oaipmhHarvester.getSources();

            // Then
            assertEquals(mockSources, result);
            mocked.verify(OAIPMHHarvestConfigurationLoader::getAllSourceNames);
        }
    }
}
