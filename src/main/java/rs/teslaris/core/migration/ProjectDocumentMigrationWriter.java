package rs.teslaris.core.migration;

/**
 * Links a migrated document to a project, so that a module importing from an external source does
 * not have to depend on the project module. Implemented there, consumed by the importing module.
 */
public interface ProjectDocumentMigrationWriter {

    /**
     * Links the document to the project as one of its results, unless it is linked already.
     *
     * @return the link, and whether it was created by this call
     */
    MigratedProjectDocument linkResult(Integer projectId, Integer documentId);

    /**
     * @param id      the project document link
     * @param created false when the document was already linked to the project
     */
    record MigratedProjectDocument(Integer id, boolean created) {
    }
}
