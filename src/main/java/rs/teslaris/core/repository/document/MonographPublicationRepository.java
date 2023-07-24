package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.MonographPublication;

@Repository
public interface MonographPublicationRepository
    extends JpaRepository<MonographPublication, Integer> {
}
