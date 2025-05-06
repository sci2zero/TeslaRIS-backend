package rs.teslaris.core.repository.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.ThesisResearchOutput;

@Repository
public interface ThesisResearchOutputRepository
    extends JpaRepository<ThesisResearchOutput, Integer> {

    Optional<ThesisResearchOutput> findByThesisIdAndResearchOutputId(Integer thesisId,
                                                                     Integer researchOutputId);

    @Query("SELECT tro.researchOutput.id FROM ThesisResearchOutput tro " +
        "WHERE tro.thesis.id = :thesisId")
    List<Integer> findResearchOutputIdsForThesis(Integer thesisId);
}
