package rs.teslaris.core.repository.institution;

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

    @Query("SELECT ou FROM OrganisationUnit ou " +
        "LEFT JOIN FETCH ou.keyword " +
        "LEFT JOIN FETCH ou.name " +
        "LEFT JOIN FETCH ou.researchAreas " +
        "WHERE ou.id = :id")
    Optional<OrganisationUnit> findByIdWithLangDataAndResearchArea(Integer id);

    Optional<OrganisationUnit> findOrganisationUnitByOldId(Integer oldId);

    @Query(value = "SELECT ou FROM OrganisationUnit ou left " +
        "JOIN FETCH ou.keyword left " +
        "JOIN FETCH ou.name " +
        "LEFT JOIN FETCH ou.researchAreas",
        countQuery = "SELECT COUNT(ou) FROM OrganisationUnit ou")
    Page<OrganisationUnit> findAllWithLangData(Pageable pageable);

    @Query("SELECT COUNT(t) > 0 FROM Thesis t " +
        "JOIN t.organisationUnit ou " +
        "WHERE ou.id = :organisationUnitId")
    boolean hasThesis(Integer organisationUnitId);

    @Query("SELECT COUNT(i) > 0 FROM Involvement i " +
        "JOIN i.organisationUnit ou " +
        "WHERE ou.id = :organisationUnitId")
    boolean hasInvolvement(Integer organisationUnitId);

    @Query("SELECT COUNT(our) > 0 FROM OrganisationUnitsRelation our " +
        "JOIN our.sourceOrganisationUnit sou JOIN our.targetOrganisationUnit tou " +
        "WHERE sou.id = :organisationUnitId or tou.id = :organisationUnitId")
    boolean hasRelation(Integer organisationUnitId);

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.organisationUnit ou WHERE ou.id = :organisationUnitId")
    boolean hasEmployees(Integer organisationUnitId);

    @Query("SELECT CASE WHEN COUNT(ou) > 0 THEN TRUE ELSE FALSE END " +
        "FROM OrganisationUnit ou WHERE ou.scopusAfid = :scopusAfid AND (:id IS NULL OR ou.id <> :id)")
    boolean existsByScopusAfid(String scopusAfid, Integer id);

    @Query("SELECT CASE WHEN COUNT(ou) > 0 THEN TRUE ELSE FALSE END " +
        "FROM OrganisationUnit ou WHERE ou.openAlexId = :openAlexId AND (:id IS NULL OR ou.id <> :id)")
    boolean existsByOpenAlexId(String openAlexId, Integer id);

    @Query("SELECT CASE WHEN COUNT(ou) > 0 THEN TRUE ELSE FALSE END " +
        "FROM OrganisationUnit ou WHERE ou.ror = :ror AND (:id IS NULL OR ou.id <> :id)")
    boolean existsByROR(String ror, Integer id);

    @Query(value = "SELECT * FROM organisation_units ou WHERE " +
        "ou.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY'", nativeQuery = true)
    Page<OrganisationUnit> findAllModifiedInLast24Hours(Pageable pageable);

    @Query("SELECT t FROM Thesis t WHERE t.organisationUnit.id = :organisationUnitId")
    Page<Thesis> fetchAllThesesForOU(Integer organisationUnitId, Pageable pageable);

    @Modifying
    @Query("UPDATE Involvement i SET i.deleted = true " +
        "WHERE i.organisationUnit.id = :organisationUnitId")
    void deleteInvolvementsForOrganisationUnit(Integer organisationUnitId);

    @Modifying
    @Query("UPDATE OrganisationUnitsRelation our SET our.deleted = true " +
        "WHERE our.sourceOrganisationUnit.id = :organisationUnitId OR " +
        "our.targetOrganisationUnit.id = :organisationUnitId")
    void deleteRelationsForOrganisationUnit(Integer organisationUnitId);

    @Query("SELECT u FROM User u WHERE u.organisationUnit.id = :organisationUnitId AND u.authority.name = 'INSTITUTIONAL_EDITOR'")
    List<User> fetchInstitutionalEditorsForOrganisationUnit(Integer organisationUnitId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.organisationUnit.id = :organisationUnitId AND u.authority.name = 'INSTITUTIONAL_EDITOR'")
    boolean checkIfInstitutionalAdminsExist(Integer organisationUnitId);

    @Query("SELECT ou FROM OrganisationUnit ou " +
        "JOIN ou.accountingIds aid " +
        "WHERE aid = :id AND ou.approveStatus = 1")
    Optional<OrganisationUnit> findApprovedOrganisationUnitByAccountingId(String id);
}
