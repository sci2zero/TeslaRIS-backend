package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ApiKeyRequest;
import rs.teslaris.core.dto.commontypes.ApiKeyResponse;
import rs.teslaris.core.model.commontypes.ApiKey;
import rs.teslaris.core.model.commontypes.ApiKeyType;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface ApiKeyService extends JPAService<ApiKey> {

    ApiKeyResponse createApiKey(ApiKeyRequest apiKeyRequest);

    void updateApiKey(Integer apiKeyId, ApiKeyRequest apiKeyRequest);

    Page<ApiKeyResponse> listAllApiKeys(Pageable pageable);

    void deleteApiKey(Integer apiKeyId);

    boolean validateApiKey(String apiKeyValue, ApiKeyType apiKeyType);
}
