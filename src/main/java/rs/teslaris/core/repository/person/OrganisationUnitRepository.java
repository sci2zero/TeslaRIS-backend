package rs.teslaris.core.repository.person;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Repository
public interface OrganisationUnitRepository extends JpaRepository<OrganisationUnit, Integer> {


    @Query("select ou from  OrganisationUnit ou left join fetch ou.keyword left join fetch ou.name left join fetch ou.researchAreas where ou.id = :id")
    Optional<OrganisationUnit> findByIdWithLangDataAndResearchArea(Integer id);

    @Query(value = "select ou from  OrganisationUnit ou left join fetch ou.keyword left join fetch ou.name left join fetch ou.researchAreas", countQuery = "select count(ou) from OrganisationUnit ou")
    Page<OrganisationUnit> findAllWithLangData(Pageable pageable);
}
