package rs.teslaris.core.model.document;

import lombok.Getter;

@Getter
public enum BibliographicFormat {
    BIBTEX("application/x-bibtex"),
    ENDNOTE("application/x-endnote+xml"),
    REFMAN("application/x-research-info-systems");

    private final String value;

    BibliographicFormat(String value) {
        this.value = value;
    }
}
