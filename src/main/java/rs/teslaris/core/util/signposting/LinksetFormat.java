package rs.teslaris.core.util.signposting;

import lombok.Getter;

@Getter
public enum LinksetFormat {
    JSON("application/linkset+json"),
    LINKSET("application/linkset");

    private String value;

    LinksetFormat(String value) {
        this.value = value;
    }
}
