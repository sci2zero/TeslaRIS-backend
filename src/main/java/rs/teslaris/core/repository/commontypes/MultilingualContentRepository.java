package rs.teslaris.core.repository.commontypes;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface MultilingualContentRepository
    extends JPASoftDeleteRepository<MultiLingualContent> {
}
