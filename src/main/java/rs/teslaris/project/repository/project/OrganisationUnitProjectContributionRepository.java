package rs.teslaris.project.repository.project;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;

@Repository
public interface OrganisationUnitProjectContributionRepository
    extends JpaRepository<OrganisationUnitProjectContribution, Integer> {

    @Query("SELECT oupc.organisationUnit.id FROM OrganisationUnitProjectContribution oupc " +
        "WHERE oupc.project.id = :projectId AND oupc.organisationUnit IS NOT NULL")
    List<Integer> findOrganisationUnitIdsByProjectId(Integer projectId);
}
