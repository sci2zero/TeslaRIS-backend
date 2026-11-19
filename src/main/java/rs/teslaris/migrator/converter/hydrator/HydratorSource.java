package rs.teslaris.migrator.converter.hydrator;

public final class HydratorSource {

    public static final String NAME = "hydrator";

    /**
     * Stamped on {@code BaseEntity.importSource} of every entity created from a curriculum.
     */
    public static final String IMPORT_SOURCE = "CIENCIA_VITAE";

    // Stamped on projects and fundings, which come from SciPROJ rather than a curriculum
    public static final String PROJECTS_IMPORT_SOURCE = "SCIPROJ";

    // Other identifiers holding the SciPROJ project and funding ids
    public static final String SCIPROJ_PROJECT_IDENTIFIER_CODE = "SciPROJ_Project_Id";

    public static final String SCIPROJ_FUNDING_IDENTIFIER_CODE = "SciPROJ_Funding_Id";

    // Other identifier holding the curriculum's own id of an output ("cv-prod-id-…")
    public static final String SOURCE_WORK_IDENTIFIER_CODE = "CienciaVitae_source_work_id";

    // Code of the project identifier before the rename, kept to migrate existing definitions
    public static final String LEGACY_SCIPROJ_PROJECT_IDENTIFIER_CODE = "SCIPROJID";

    /**
     * Sort key pinned on the client call. See the design document - offset paging over a mutable
     * sort key can skip records, which is why keyset pagination was requested from hydrator.
     */
    public static final String CURRICULA_SORT = "id,asc";

    // Hydrator's default project sort (harvest datestamp) is the same for every record
    public static final String PROJECTS_SORT = "id,asc";

    private HydratorSource() {
    }
}
