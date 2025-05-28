package rs.teslaris.core.model.user;

public enum UserRole {
    ADMIN("ADMIN"),
    RESEARCHER("RESEARCHER"),
    INSTITUTIONAL_EDITOR("INSTITUTIONAL_EDITOR"),
    COMMISSION("COMMISSION"),
    VICE_DEAN_FOR_SCIENCE("VICE_DEAN_FOR_SCIENCE"),
    INSTITUTIONAL_LIBRARIAN("INSTITUTIONAL_LIBRARIAN"),
    HEAD_OF_LIBRARY("HEAD_OF_LIBRARY"),
    PROMOTION_REGISTRY_ADMINISTRATOR("PROMOTION_REGISTRY_ADMINISTRATOR");

    private final String text;


    UserRole(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
