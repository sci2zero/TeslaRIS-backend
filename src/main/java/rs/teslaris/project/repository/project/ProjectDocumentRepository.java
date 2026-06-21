package rs.teslaris.project.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.project.ProjectDocument;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Integer> {

    @Query("SELECT pd.document.id FROM ProjectDocument pd WHERE pd.project.id = :projectId")
    List<Integer> findDocumentIdsByProjectId(Integer projectId);

}
