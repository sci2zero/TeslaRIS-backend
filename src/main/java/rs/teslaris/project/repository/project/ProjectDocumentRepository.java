package rs.teslaris.project.repository.project;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.project.ProjectDocument;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Integer> {

    @Query("SELECT pd.document.id FROM ProjectDocument pd WHERE pd.project.id = :projectId")
    List<Integer> findDocumentIdsByProjectId(Integer projectId);

    @Query("SELECT pd FROM ProjectDocument pd WHERE pd.project.id = :projectId")
    List<ProjectDocument> findAllByProjectId(Integer projectId);

    // One query for a page of projects in the common export. Returns
    // [projectId, documentId, relationType, projectDocumentId, fundingId] rows, one per funding
    // part (fundingId is null for an output without funding parts).
    @Query("SELECT pd.project.id, pd.document.id, pd.relationType, pd.id, fp.funding.id " +
        "FROM ProjectDocument pd LEFT JOIN pd.fundingParts fp " +
        "WHERE pd.project.id IN :projectIds AND pd.document IS NOT NULL " +
        "AND pd.privateInfo = FALSE ORDER BY pd.project.id, pd.id")
    List<Object[]> findDocumentLinksForProjects(Collection<Integer> projectIds);

}
