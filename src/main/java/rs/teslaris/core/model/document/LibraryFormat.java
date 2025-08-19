package rs.teslaris.core.model.document;

import lombok.Getter;

@Getter
public enum LibraryFormat {
    ETD_MS("application/etdms+xml"),
    MARC21("application/marcxml+xml"),
    DUBLIN_CORE("application/dc+xml");

    private String value;

    LibraryFormat(String value) {
        this.value = value;
    }
}
