package rs.teslaris.core.model.document;

public enum DocumentFileSection implements FileSection {
    FILE_ITEMS("backup.fileItems"),
    PROOFS("backup.proofs");

    private final String internationalizationName;

    DocumentFileSection(String internationalizationName) {
        this.internationalizationName = internationalizationName;
    }

    @Override
    public String getInternationalizationMessageName() {
        return internationalizationName;
    }
}
