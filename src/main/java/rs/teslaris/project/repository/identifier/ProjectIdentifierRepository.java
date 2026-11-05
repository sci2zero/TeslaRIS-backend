package rs.teslaris.project.repository.identifier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.project.model.identifier.ProjectIdentifier;

@Repository
public interface ProjectIdentifierRepository extends JpaRepository<ProjectIdentifier, Integer> {

    @Query("SELECT pi FROM ProjectIdentifier pi JOIN FETCH pi.identifier i " +
        "WHERE pi.project.id = :projectId AND i.accessLevel <= :accessLevel " +
        "ORDER BY i.code, pi.value")
    List<ProjectIdentifier> findIdentifiersForProjectAndIdentifierAccessLevel(Integer projectId,
                                                                              AccessLevel accessLevel);
}
