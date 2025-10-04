package rs.teslaris.core.configuration;

import lombok.Getter;

@Getter
public enum OAuth2Provider {
    ORCID("orcid");

    private final String value;

    OAuth2Provider(String value) {
        this.value = value;
    }
}
