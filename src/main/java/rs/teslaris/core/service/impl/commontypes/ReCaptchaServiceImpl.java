package rs.teslaris.core.service.impl.commontypes;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import rs.teslaris.core.service.interfaces.commontypes.ReCaptchaService;

@Service
public class ReCaptchaServiceImpl implements ReCaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Value("${recaptcha.secret.key}")
    private String secretKey;

    @Override
    public boolean isCaptchaValid(String token) {

        var restTemplate = new RestTemplate();
        var params = Map.of("secret", secretKey, "response", token);
        var response = restTemplate.postForObject(VERIFY_URL, params, Map.class);

        return response != null && Boolean.TRUE.equals(response.get("success"));
    }
}
