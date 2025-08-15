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

    @Value("${registration.allow-creation-of-researchers}")
    private boolean allowNewResearcherCreation;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
        throws IOException {

        var oauthToken = (OAuth2AuthenticationToken) authentication;
        var oauthUser = oauthToken.getPrincipal();

        String orcidId = oauthUser.getAttribute("orcid");
        String name = oauthUser.getAttribute("name");

        var user = personRepository.findUserForOrcid(orcidId);
        var person = personRepository.findPersonForOrcid(orcidId);

        var frontendBasePath = frontendUrl + defaultLocale.toLowerCase();

        if (user.isEmpty() || Objects.isNull(name)) {
            request.getSession().invalidate();

            if (person.isEmpty() && !allowNewResearcherCreation) {
                response.sendRedirect(frontendBasePath + "/login?error=orcidNotLinked");
                return;
            }

            if ((Objects.isNull(name) || name.isBlank())) {
                // should never happen, but is still covered just in case
                name = person.map(
                        value -> value.getName().getFirstname() + " " + value.getName().getLastname())
                    .orElse("");
            }

            response.sendRedirect(
                frontendUrl + defaultLocale.toLowerCase() + "/register?identifier=" + orcidId +
                    "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8) +
                    "&oauth=true&provider=orcid");
            return;
        }

        if (!user.get().isAccountNonLocked()) {
            request.getSession().invalidate();
            response.sendRedirect(frontendBasePath + "/login?error=accountNotActivated");
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