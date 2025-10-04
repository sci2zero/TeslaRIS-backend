package rs.teslaris.core.configuration;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.user.UserRole;

@Component
public class OrcidOAuth2UserInfoHandler
    implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        Map<String, Object> additionalParams = userRequest.getAdditionalParameters();
        var orcid = (String) additionalParams.get("orcid");
        var name = (String) additionalParams.get("name");

        if (Objects.isNull(orcid)) {
            throw new OAuth2AuthenticationException("ORCID ID not found in token response");
        }

        Map<String, Object> attributes = Map.of(
            "orcid", orcid,
            "name", name
        );

        return new DefaultOAuth2User(
            Set.of(UserRole.RESEARCHER::name),
            attributes,
            "orcid");
    }
}
