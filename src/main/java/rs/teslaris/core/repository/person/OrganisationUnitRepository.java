package rs.teslaris.core.repository.person;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.user.User;

@Repository
public interface OrganisationUnitRepository extends JpaRepository<OrganisationUnit, Integer> {

    @Query("select ou from  OrganisationUnit ou left join fetch ou.keyword left join fetch ou.name left join fetch ou.researchAreas where ou.id = :id")
    Optional<OrganisationUnit> findByIdWithLangDataAndResearchArea(Integer id);

    Optional<OrganisationUnit> findOrganisationUnitByOldId(Integer oldId);

    @Query(value = "select ou from  OrganisationUnit ou left join fetch ou.keyword left join fetch ou.name left join fetch ou.researchAreas", countQuery = "select count(ou) from OrganisationUnit ou")
    Page<OrganisationUnit> findAllWithLangData(Pageable pageable);

    @Query("select count(t) > 0 from Thesis t join t.organisationUnit ou where ou.id = :organisationUnitId")
    boolean hasThesis(Integer organisationUnitId);

    @Query("select count(i) > 0 from Involvement i join i.organisationUnit ou where ou.id = :organisationUnitId")
    boolean hasInvolvement(Integer organisationUnitId);

    @Query("select count(our) > 0 from OrganisationUnitsRelation our " +
        "join our.sourceOrganisationUnit sou join our.targetOrganisationUnit tou " +
        "where sou.id = :organisationUnitId or tou.id = :organisationUnitId")
    boolean hasRelation(Integer organisationUnitId);

    @Query("select count(u) > 0 from User u join u.organisationUnit ou where ou.id = :organisationUnitId")
    boolean hasEmployees(Integer organisationUnitId);

    @Query("SELECT CASE WHEN COUNT(ou) > 0 THEN TRUE ELSE FALSE END " +
        "FROM OrganisationUnit ou WHERE ou.scopusAfid = :scopusAfid AND ou.id <> :id")
    boolean existsByScopusAfid(String scopusAfid, Integer id);

    @Query(value = "SELECT * FROM organisation_units ou WHERE " +
        "ou.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY'", nativeQuery = true)
    Page<OrganisationUnit> findAllModifiedInLast24Hours(Pageable pageable);

    @Query("select t from Thesis t where t.organisationUnit.id = :organisationUnitId")
    Page<Thesis> fetchAllThesesForOU(Integer organisationUnitId, Pageable pageable);

    @Modifying
    @Query("update Involvement i set i.deleted = true where i.organisationUnit.id = :organisationUnitId")
    void deleteInvolvementsForOrganisationUnit(Integer organisationUnitId);

    @Modifying
    @Query("update OrganisationUnitsRelation our set our.deleted = true " +
        "where our.sourceOrganisationUnit.id = :organisationUnitId or " +
        "our.targetOrganisationUnit.id = :organisationUnitId")
    void deleteRelationsForOrganisationUnit(Integer organisationUnitId);

    @Query("select u from User u where u.organisationUnit.id = :organisationUnitId and u.authority.name = 'INSTITUTIONAL_EDITOR'")
    List<User> fetchInstitutionalEditorsForOrganisationUnit(Integer organisationUnitId);

    @Query("select count(u) > 0 from User u where u.organisationUnit.id = :organisationUnitId and u.authority.name = 'INSTITUTIONAL_EDITOR'")
    boolean checkIfInstitutionalAdminsExist(Integer organisationUnitId);
}
