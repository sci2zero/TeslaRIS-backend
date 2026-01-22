package rs.teslaris.core.repository.document;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    @Query("SELECT d FROM Document d JOIN FETCH d.contributors WHERE d.id IN :ids")
    List<Document> findBulkDocuments(List<Integer> ids);

    @Query(value = """
        SELECT id FROM datasets WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM software WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM monographs WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM patents WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM proceedings WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM journal_publications WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM proceedings_publications WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM monograph_publications WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM theses WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM material_products WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM genetic_materials WHERE old_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        """, nativeQuery = true)
    Optional<Integer> findDocumentByOldIdsContains(Integer oldId);

    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT 1 FROM datasets WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM software WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM monographs WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM patents WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM proceedings WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM journal_publications WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM proceedings_publications WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM monograph_publications WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM theses WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM material_products WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
            UNION ALL
            SELECT 1 FROM genetic_materials WHERE old_ids @> to_jsonb(array[cast(?1 as int)])
        ) AS document_counts
        """, nativeQuery = true)
    Integer countDocumentsByOldIdsContains(Integer oldId);

    @Query(value = """
        SELECT id FROM datasets WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM software WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM monographs WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM patents WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM proceedings WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM journal_publications WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM proceedings_publications WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM monograph_publications WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM theses WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM material_products WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        UNION ALL
        SELECT id FROM genetic_materials WHERE merged_ids @> to_jsonb(array[cast(?1 as int)]) AND deleted = FALSE
        """, nativeQuery = true)
    Optional<Integer> findDocumentByMergedIdsContains(Integer documentId);

    @Query(value = """
        SELECT id FROM datasets WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM software WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM monographs WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM patents WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM proceedings WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM journal_publications WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM proceedings_publications WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM monograph_publications WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM theses WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM material_products WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        UNION ALL
        SELECT id FROM genetic_materials WHERE internal_identifiers @> to_jsonb(array[cast(?1 as text)])
        """, nativeQuery = true)
    Optional<Integer> findDocumentByInternalIdentifiersContains(String internalId);

    @Query("SELECT d FROM Document d " +
        "JOIN FETCH d.contributors " +
        "LEFT JOIN FETCH d.fileItems " +
        "LEFT JOIN FETCH d.proofs " +
        "WHERE d.id in :ids")
    List<Document> findDocumentByIdIn(List<Integer> ids, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Document d WHERE d.doi = :doi AND (:id IS NULL OR d.id <> :id)")
    boolean existsByDoi(String doi, Integer id);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Document d WHERE d.scopusId = :scopusId AND (:id IS NULL OR d.id <> :id)")
    boolean existsByScopusId(String scopusId, Integer id);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Document d WHERE d.openAlexId = :openAlexId AND (:id IS NULL OR d.id <> :id)")
    boolean existsByOpenAlexId(String openAlexId, Integer id);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Document d WHERE d.webOfScienceId = :webOfScienceId AND (:id IS NULL OR d.id <> :id)")
    boolean existsByWebOfScienceId(String webOfScienceId, Integer id);

    @Query("SELECT d FROM Document d " +
        "JOIN PersonDocumentContribution dc ON d.id = dc.document.id " +
        "LEFT JOIN FETCH d.contributors " +
        "WHERE dc.person.id = :authorId")
    List<Document> getDocumentsForAuthorId(Integer authorId);

    @Query("SELECT DISTINCT inst.id " +
        "FROM ProceedingsPublication pp " +
        "JOIN pp.proceedings p " +
        "JOIN pp.contributors pc " +
        "JOIN pc.institutions inst " +
        "WHERE p.id = :proceedingsId " +
        "AND pc.contributionType = 0")
    Set<Integer> findInstitutionIdsByProceedingsIdAndAuthorContribution(Integer proceedingsId);

    @Query("SELECT DISTINCT inst.id " +
        "FROM MonographPublication mp " +
        "JOIN mp.monograph m " +
        "JOIN mp.contributors pc " +
        "JOIN pc.institutions inst " +
        "WHERE m.id = :monographId " +
        "AND pc.contributionType = 0")
    Set<Integer> findInstitutionIdsByMonographIdAndAuthorContribution(Integer monographId);

    @Query("SELECT COUNT(d) > 0 FROM Document d LEFT JOIN d.fileItems fi WHERE " +
        "d.id = :documentId AND fi.resourceType = 1 AND fi.accessRights = 2")
    boolean isDocumentPubliclyAvailable(Integer documentId);

    @Query("SELECT d FROM Document d WHERE " +
        "d.openAlexId = :openAlexId OR " +
        "d.doi = :doi OR " +
        "d.scopusId = :scopusId OR " +
        "d.webOfScienceId = :webOfScienceId")
    Optional<Document> findByOpenAlexIdOrDoiOrScopusIdOrWOSId(String openAlexId, String doi,
                                                              String scopusId,
                                                              String webOfScienceId);

    @Query("SELECT DISTINCT p.id FROM Document doc " +
        "JOIN doc.contributors cont " +
        "JOIN cont.person p " +
        "WHERE doc.id = :documentId AND p IS NOT NULL")
    List<Integer> findPersonIdsByDocumentId(Integer documentId);
}
