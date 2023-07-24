package rs.teslaris.core.repository.commontypes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Repository
public interface MultilingualContentRepository
    extends JpaRepository<MultiLingualContent, Integer> {
}
