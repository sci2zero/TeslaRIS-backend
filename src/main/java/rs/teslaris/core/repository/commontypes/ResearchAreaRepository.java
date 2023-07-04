package rs.teslaris.core.repository.commontypes;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface ResearchAreaRepository extends JPASoftDeleteRepository<ResearchArea> {

    @Query("select count(ra) > 0 from ResearchArea ra join ra.superResearchArea ras where ra.superResearchArea.id = :researchAreaId and ra.deleted = false and ras.deleted = false")
    boolean isSuperArea(Integer researchAreaId);

    @Query("select count(p) > 0 from Person p join p.researchAreas ra where ra.id = :researchAreaId and p.deleted = false and ra.deleted = false")
    boolean isResearchedBySomeone(Integer researchAreaId);

    @Query("select count(m) > 0 from Monograph m join m.researchArea where m.researchArea.id = :researchAreaId and m.deleted = false and m.researchArea.deleted = false")
    boolean isResearchedInMonograph(Integer researchAreaId);

    @Query("select count(t) > 0 from Thesis t join t.researchArea where t.researchArea.id = :researchAreaId and t.deleted = false and t.researchArea.deleted = false")
    boolean isResearchedInThesis(Integer researchAreaId);
}
