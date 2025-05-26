package rs.teslaris.core.service.impl.commontypes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.ApiKeyRequest;
import rs.teslaris.core.dto.commontypes.ApiKeyResponse;
import rs.teslaris.core.model.commontypes.ApiKey;
import rs.teslaris.core.model.commontypes.ApiKeyType;
import rs.teslaris.core.repository.commontypes.ApiKeyRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.ApiKeyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.InvalidApiKeyException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@Traceable
public class ApiKeyServiceImpl extends JPAServiceImpl<ApiKey> implements ApiKeyService {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final ApiKeyRepository apiKeyRepository;

    private final MultilingualContentService multilingualContentService;

    private final EmailUtil emailUtil;

    private final MessageSource messageSource;

    private final LanguageService languageService;


    @Override
    protected JpaRepository<ApiKey, Integer> getEntityRepository() {
        return apiKeyRepository;
    }

    @Override
    public ApiKeyResponse createApiKey(ApiKeyRequest apiKeyRequest) {
        validateExpirationDate(apiKeyRequest);

        var locale = getClientLocale(apiKeyRequest.clientPreferredLanguageId());

        var keyValue = generateApiKeyValue();
        var newApiKey = buildApiKey(apiKeyRequest, keyValue, locale);
        var savedApiKey = apiKeyRepository.save(newApiKey);

        log.info("Created {} API key ({}) for {}.",
            savedApiKey.getUsageType().name(),
            savedApiKey.getId(),
            savedApiKey.getClientEmail());

        sendApiKeyEmail(savedApiKey, keyValue, locale);

        return buildApiKeyResponse(savedApiKey);
    }

    private String generateApiKeyValue() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ApiKey buildApiKey(ApiKeyRequest apiKeyRequest, String keyValue,
                               String preferredLanguage) {
        var apiKey = new ApiKey();
        apiKey.setName(multilingualContentService.getMultilingualContent(apiKeyRequest.name()));
        apiKey.setUsageType(apiKeyRequest.type());
        apiKey.setValue(encoder.encode(keyValue));
        apiKey.setLookupHash(truncatedLookupHash(keyValue));
        apiKey.setValidUntil(apiKeyRequest.validUntil());
        apiKey.setClientEmail(apiKeyRequest.clientEmail());
        apiKey.setPreferredLanguage(preferredLanguage);
        apiKey.setDailyRequests(apiKeyRequest.dailyRequests());
        return apiKey;
    }

    private void sendApiKeyEmail(ApiKey savedApiKey, String keyValue, String locale) {
        var subject =
            getMessage("apikey.email.subject", new Object[] {savedApiKey.getUsageType().name()},
                locale);
        var body = getMessage("apikey.email.body",
            new Object[] {savedApiKey.getUsageType().name(), keyValue}, locale);

        emailUtil.sendSimpleEmail(savedApiKey.getClientEmail(), subject, body);
    }

    private String getClientLocale(Integer preferredLanguageId) {
        return languageService.findOne(preferredLanguageId).getLanguageCode();
    }

    private String getMessage(String key, Object[] args, String locale) {
        try {
            return messageSource.getMessage(key, args, Locale.forLanguageTag(locale));
        } catch (NoSuchMessageException e) {
            return messageSource.getMessage(key, args, Locale.ENGLISH);
        }
    }

    private ApiKeyResponse buildApiKeyResponse(ApiKey savedApiKey) {
        return new ApiKeyResponse(
            savedApiKey.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(savedApiKey.getName()),
            savedApiKey.getUsageType(),
            savedApiKey.getValidUntil(),
            savedApiKey.getClientEmail(),
            savedApiKey.getDailyRequests()
        );
    }

    @Override
    public void updateApiKey(Integer apiKeyId, ApiKeyRequest apiKeyRequest) {
        validateExpirationDate(apiKeyRequest);

        var apiKeyToUpdate = findOne(apiKeyId);
        apiKeyToUpdate.setName(
            multilingualContentService.getMultilingualContent(apiKeyRequest.name()));
        apiKeyToUpdate.setValidUntil(apiKeyRequest.validUntil());
        apiKeyToUpdate.setDailyRequests(apiKeyRequest.dailyRequests());
        save(apiKeyToUpdate);

        log.info("Updated {} API key ({}) for {}.", apiKeyToUpdate.getUsageType().name(),
            apiKeyToUpdate.getId(),
            apiKeyToUpdate.getClientEmail());
    }

    private void validateExpirationDate(ApiKeyRequest apiKeyRequest) {
        if (apiKeyRequest.validUntil().isBefore(LocalDate.now())) {
            throw new InvalidApiKeyException("Expiration date must be in future.");
        }
    }

    @Override
    public Page<ApiKeyResponse> listAllApiKeys(Pageable pageable) {
        return findAll(pageable).map(apiKey -> new ApiKeyResponse(apiKey.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(apiKey.getName()),
            apiKey.getUsageType(), apiKey.getValidUntil(), apiKey.getClientEmail(),
            apiKey.getDailyRequests()));
    }

    @Override
    public void deleteApiKey(Integer apiKeyId) {
        var apiKeyToDelete = findOne(apiKeyId);

        // Physical delete
        apiKeyRepository.delete(apiKeyToDelete);

        log.info("Deleted API key with ID {} for {}.", apiKeyToDelete.getId(),
            apiKeyToDelete.getClientEmail());
    }

    @Override
    public boolean validateApiKey(String apiKeyValue, ApiKeyType apiKeyType) {
        log.info("Validating API key for accessing {}.", apiKeyType.name());

        var lookupHash = truncatedLookupHash(apiKeyValue);
        var potentialApiKeys = apiKeyRepository.findByLookupHashAndType(lookupHash, apiKeyType);

        var matchedApiKey = potentialApiKeys.stream()
            .filter(apiKey -> encoder.matches(apiKeyValue, apiKey.getValue()))
            .findFirst()
            .orElse(null);

        if (Objects.isNull(matchedApiKey) ||
            LocalDate.now().isAfter(matchedApiKey.getValidUntil())) {
            return false;
        }

        synchronized (matchedApiKey) {
            if (matchedApiKey.getTimesUsedToday() >= matchedApiKey.getDailyRequests()) {
                return false;
            }
            matchedApiKey.setTimesUsedToday(matchedApiKey.getTimesUsedToday() + 1);
            save(matchedApiKey);
        }

        return true;
    }

    private String truncatedLookupHash(String key) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available.",
                e); // should never happen
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Every day at 00:00 AM
    public void resetDailyUsageAndCleanExpiredApiKeys() {
        apiKeyRepository.resetApiKeyDailyUsage();
        apiKeyRepository.deleteByValidUntilLessThanEqual(LocalDate.now());
    }
}
