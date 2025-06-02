package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ApiKeyRequest;
import rs.teslaris.core.dto.commontypes.ApiKeyResponse;
import rs.teslaris.core.service.interfaces.commontypes.ApiKeyService;

@RestController
@RequestMapping("/api/api-key")
@RequiredArgsConstructor
@Traceable
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_API_KEYS')")
    public Page<ApiKeyResponse> fetchAllApiKeys(Pageable pageable) {
        return apiKeyService.listAllApiKeys(pageable);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_API_KEYS')")
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyResponse createApiKey(@RequestBody @Valid ApiKeyRequest apiKeyRequest) {
        return apiKeyService.createApiKey(apiKeyRequest);
    }

    @PutMapping("/{apiKeyId}")
    @PreAuthorize("hasAuthority('MANAGE_API_KEYS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateApiKey(@PathVariable Integer apiKeyId,
                             @RequestBody @Valid ApiKeyRequest apiKeyRequest) {
        apiKeyService.updateApiKey(apiKeyId, apiKeyRequest);
    }

    @DeleteMapping("/{apiKeyId}")
    @PreAuthorize("hasAuthority('MANAGE_API_KEYS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApiKey(@PathVariable Integer apiKeyId) {
        apiKeyService.deleteApiKey(apiKeyId);
    }
}
