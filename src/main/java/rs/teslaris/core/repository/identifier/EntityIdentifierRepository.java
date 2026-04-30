package rs.teslaris.core.repository.identifier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.identifier.EntityIdentifier;

@Repository
public interface EntityIdentifierRepository extends JpaRepository<EntityIdentifier, Integer> {

    @Query("""
            SELECT COUNT(ei) > 0
            FROM EntityIdentifier ei
            WHERE ei.value = :value
              AND TYPE(ei) = :clazz AND (:id IS NULL OR ei.id <> :id)
        """)
    boolean existsByValueForType(String value, Class<? extends EntityIdentifier> clazz, Integer id);
}
