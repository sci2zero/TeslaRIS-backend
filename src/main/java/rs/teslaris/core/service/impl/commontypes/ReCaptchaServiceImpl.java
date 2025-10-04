package rs.teslaris.core.service.impl.commontypes;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.commontypes.ReCaptchaService;
import rs.teslaris.core.util.session.RestTemplateProvider;

@Service
@RequiredArgsConstructor
@Traceable
public class ReCaptchaServiceImpl implements ReCaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestTemplateProvider restTemplateProvider;

    @Value("${recaptcha.secret.key}")
    private String secretKey;


    @Override
    public boolean isCaptchaValid(String token) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var params = new LinkedMultiValueMap<String, String>();
        params.add("secret", secretKey);
        params.add("response", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        var restTemplate = restTemplateProvider.provideRestTemplate();
        var response = restTemplate.postForEntity(VERIFY_URL, request, Map.class);

        return response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"));
    }

}
