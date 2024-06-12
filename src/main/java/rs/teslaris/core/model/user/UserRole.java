package rs.teslaris.core.model.user;

public enum UserRole {
    ADMIN("ADMIN"),
    RESEARCHER("RESEARCHER"),
    INSTITUTIONAL_EDITOR("INSTITUTIONAL_EDITOR");

    private final String text;


    UserRole(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
