package rs.teslaris.core.repository.identifier;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.identifier.Identifier;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, Integer> {

    @Query("SELECT count(ei) > 0 FROM EntityIdentifier ei WHERE ei.identifier.id = :identifierId")
    boolean isInUse(Integer identifierId);

    @Query("SELECT count(i) > 0 FROM Identifier i WHERE i.code = :code AND i.id != :identifierId")
    boolean identifierCodeInUse(String code, Integer identifierId);

    @Query("SELECT i " +
        "FROM Identifier i " +
        "WHERE i.applicableTypes IN :applicableEntityTypes")
    List<Identifier> getIdentifiersApplicableToEntity(
        List<ApplicableEntityType> applicableEntityTypes);

    Identifier findByCode(String code);

    @Query(
        value = """
            SELECT i
            FROM Identifier i
            LEFT JOIN i.title title
                 WITH title.language.languageTag = :languageTag
            """,
        countQuery = """
            SELECT COUNT(i)
            FROM Identifier i
            """
    )
    Page<Identifier> readAll(String languageTag, Pageable pageable);
}
