package rs.teslaris.core.dto.commontypes;

import lombok.Getter;

@Getter
public enum ExportFileType {
    CSV(".csv"),
    XLSX(".xlsx"),
    BIB(".bib"),
    RIS(".ris"),
    ENW(".enw");

    private String value;

    ExportFileType(String value) {
        this.value = value;
    }
}
