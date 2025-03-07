package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.ApiKeyRequest;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.ApiKey;
import rs.teslaris.core.model.commontypes.ApiKeyType;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.ApiKeyRepository;
import rs.teslaris.core.service.impl.commontypes.ApiKeyServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;

@SpringBootTest
public class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;


    @Test
    public void shouldCreateApiKey() {
        // Given
        var request = new ApiKeyRequest(List.of(new MultilingualContentDTO(1, "SR", "Content", 1)),
            ApiKeyType.M_SERVICE);
        var generatedKey = "randomapikey123456";
        var encodedKey = "$2a$10$hashedValue";
        var lookupHash = "truncatedArgon2Hash";

        var apiKey = new ApiKey();
        apiKey.setValue(encodedKey);
        apiKey.setLookupHash(lookupHash);
        apiKey.setUsageType(ApiKeyType.M_SERVICE);

        when(apiKeyRepository.save(any())).thenReturn(apiKey);
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));

        // When
        var response = apiKeyService.createApiKey(request);

        // Then
        assertNotNull(response);
        assertEquals(ApiKeyType.M_SERVICE, response.type());
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    public void shouldValidateApiKey() {
        // Given
        var providedKey = "randomapikey123456";
        var storedHash = "$2a$10$GTv77A7ILED1/O511ofIR.vRMAwoewLLJr.TSsHGJ8lnEEowxfD2W";
        var lookupHash = "fyBoCLUNPBKhmFk9";

        var apiKey = new ApiKey();
        apiKey.setValue(storedHash);
        apiKey.setLookupHash(lookupHash);
        apiKey.setUsageType(ApiKeyType.M_SERVICE);

        when(apiKeyRepository.findByLookupHashAndType(eq(lookupHash), eq(ApiKeyType.M_SERVICE)))
            .thenReturn(List.of(apiKey));

        // When
        boolean isValid = apiKeyService.validateApiKey(providedKey, ApiKeyType.M_SERVICE);

        // Then
        assertTrue(isValid);
        verify(apiKeyRepository).findByLookupHashAndType(eq(lookupHash), eq(ApiKeyType.M_SERVICE));
    }
}
