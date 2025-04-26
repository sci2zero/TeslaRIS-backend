package rs.teslaris.thesislibrary.model;

import rs.teslaris.core.model.document.FileSection;

public enum ThesisFileSection implements FileSection {

    PRELIMINARY_FILES("backup.preliminaryFiles"),
    PRELIMINARY_SUPPLEMENTS("backup.preliminarySupplements"),
    COMMISSION_REPORTS("backup.commissionReports");

    private final String internationalizationName;

    ThesisFileSection(String internationalizationName) {
        this.internationalizationName = internationalizationName;
    }

    @Override
    public String getInternationalizationMessageName() {
        return internationalizationName;
    }
}
