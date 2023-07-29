package rs.teslaris.core.repository.commontypes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.ResearchArea;

@Repository
public interface ResearchAreaRepository extends JpaRepository<ResearchArea, Integer> {

    @Query("select count(ra) > 0 from ResearchArea ra join ra.superResearchArea ras where ra.superResearchArea.id = :researchAreaId")
    boolean isSuperArea(Integer researchAreaId);

    @Query("select count(p) > 0 from Person p join p.researchAreas ra where ra.id = :researchAreaId")
    boolean isResearchedBySomeone(Integer researchAreaId);

    @Query("select count(m) > 0 from Monograph m join m.researchArea where m.researchArea.id = :researchAreaId")
    boolean isResearchedInMonograph(Integer researchAreaId);

    @Query("select count(t) > 0 from Thesis t join t.researchArea where t.researchArea.id = :researchAreaId")
    boolean isResearchedInThesis(Integer researchAreaId);
}
