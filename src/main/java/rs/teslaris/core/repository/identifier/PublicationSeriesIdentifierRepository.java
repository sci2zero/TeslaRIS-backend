package rs.teslaris.core.repository.identifier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.PublicationSeriesIdentifier;

@Repository
public interface PublicationSeriesIdentifierRepository
    extends JpaRepository<PublicationSeriesIdentifier, Integer> {

    @Query("SELECT psi FROM PublicationSeriesIdentifier psi " +
        "WHERE psi.publicationSeries.id = :publicationSeriesId AND psi.identifier.accessLevel <= :accessLevel")
    List<PublicationSeriesIdentifier> findIdentifiersForPublicationSeriesAndIdentifierAccessLevel(
        Integer publicationSeriesId,
        AccessLevel accessLevel);
}
