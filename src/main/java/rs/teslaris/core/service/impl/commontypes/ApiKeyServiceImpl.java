package rs.teslaris.core.service.impl.commontypes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.ApiKeyRequest;
import rs.teslaris.core.dto.commontypes.ApiKeyResponse;
import rs.teslaris.core.model.commontypes.ApiKey;
import rs.teslaris.core.model.commontypes.ApiKeyType;
import rs.teslaris.core.repository.commontypes.ApiKeyRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.ApiKeyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;

@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl extends JPAServiceImpl<ApiKey> implements ApiKeyService {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final ApiKeyRepository apiKeyRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<ApiKey, Integer> getEntityRepository() {
        return apiKeyRepository;
    }

    @Override
    public ApiKeyResponse createApiKey(ApiKeyRequest apiKeyRequest) {
        var keyValue = UUID.randomUUID().toString().replace("-", "");

        var newApiKey = new ApiKey();
        newApiKey.setName(multilingualContentService.getMultilingualContent(apiKeyRequest.name()));
        newApiKey.setUsageType(apiKeyRequest.type());
        newApiKey.setValue(encoder.encode(keyValue));
        newApiKey.setLookupHash(truncatedLookupHash(keyValue));

        var savedApiKey = apiKeyRepository.save(newApiKey);

        return new ApiKeyResponse(
            MultilingualContentConverter.getMultilingualContentDTO(savedApiKey.getName()),
            keyValue,  // Return only once
            savedApiKey.getUsageType()
        );
    }

    @Override
    public Page<ApiKeyResponse> listAllApiKeys(Pageable pageable) {
        return findAll(pageable).map(apiKey -> new ApiKeyResponse(
            MultilingualContentConverter.getMultilingualContentDTO(apiKey.getName()), null,
            apiKey.getUsageType()));
    }

    @Override
    public void deleteApiKey(Integer apiKeyId) {
        var apiKeyToDelete = findOne(apiKeyId);

        // Physical delete
        apiKeyRepository.delete(apiKeyToDelete);
    }

    @Override
    public boolean validateApiKey(String apiKeyValue, ApiKeyType apiKeyType) {
        var lookupHash = truncatedLookupHash(apiKeyValue);

        var matchedApiKeys = apiKeyRepository.findByLookupHashAndType(lookupHash, apiKeyType);
        return matchedApiKeys.stream()
            .anyMatch(apiKey -> encoder.matches(apiKeyValue, apiKey.getValue()));
    }

    private String truncatedLookupHash(String key) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e); // should never happen
        }
    }
}
