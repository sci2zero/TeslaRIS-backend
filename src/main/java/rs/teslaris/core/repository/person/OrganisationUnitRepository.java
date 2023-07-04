package rs.teslaris.core.repository.person;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface OrganisationUnitRepository extends JPASoftDeleteRepository<OrganisationUnit> {

    @Query("select ou from  OrganisationUnit ou left join fetch ou.keyword left join fetch ou.name left join fetch ou.researchAreas where ou.id = :id and ou.deleted = false")
    Optional<OrganisationUnit> findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(Integer id);

    @Query(value = "select ou from  OrganisationUnit ou left join fetch ou.keyword left join fetch ou.name left join fetch ou.researchAreas", countQuery = "select count(ou) from OrganisationUnit ou")
    @Deprecated(forRemoval = true)
    Page<OrganisationUnit> findAllWithLangData(Pageable pageable);

    @Query(value = "select ou from  OrganisationUnit ou left join fetch ou.keyword left join fetch ou.name left join fetch ou.researchAreas where ou.deleted = false", countQuery = "select count(ou) from OrganisationUnit ou")
    @Deprecated(forRemoval = true)
    Page<OrganisationUnit> findAllWithLangDataAndDeletedIsFalse(Pageable pageable);
}
