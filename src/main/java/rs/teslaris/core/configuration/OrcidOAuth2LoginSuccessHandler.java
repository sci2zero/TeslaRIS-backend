package rs.teslaris.core.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.user.OAuthCode;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.repository.user.OAuthCodeRepository;

@Component
@RequiredArgsConstructor
public class OrcidOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final PersonRepository personRepository;

    private final OAuthCodeRepository oAuthCodeRepository;

    @Value("${frontend.application.address}")
    private String frontendUrl;

    @Value("${default.locale}")
    private String defaultLocale;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
        throws IOException {

        var oauthToken = (OAuth2AuthenticationToken) authentication;
        var oauthUser = oauthToken.getPrincipal();

        String orcidId = oauthUser.getAttribute("orcid");
        String name = oauthUser.getAttribute("name");

        var user = personRepository.findUserForOrcid(orcidId);
        if (user.isEmpty() || Objects.isNull(name)) {
            request.getSession().invalidate();
            response.sendRedirect(
                frontendUrl + defaultLocale.toLowerCase() + "/login?error=orcidNotLinked");
            return;
        }

        var code = UUID.randomUUID().toString();
        oAuthCodeRepository.save(new OAuthCode(code, orcidId, user.get().getId()));

        var language = user.get().getPreferredUILanguage();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        response.sendRedirect(
            frontendUrl + language.getLanguageTag().toLowerCase() + "/oauth2?code=" + code +
                "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "&identifier=" +
                orcidId + "&registrationId=" + registrationId);
    }
}