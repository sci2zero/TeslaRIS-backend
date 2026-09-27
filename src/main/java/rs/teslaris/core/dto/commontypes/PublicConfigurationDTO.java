package rs.teslaris.core.dto.commontypes;

public record PublicConfigurationDTO(

    Integer schemaVersion,

    String updatedAt,

    PublicBrandingDTO branding
) {

    public static final int SCHEMA_VERSION = 1;
}
