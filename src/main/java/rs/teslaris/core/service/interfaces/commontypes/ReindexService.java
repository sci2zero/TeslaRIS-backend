package rs.teslaris.core.service.interfaces.commontypes;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.IndexType;

@Service
public interface ReindexService {

    void reindexDatabase(List<IndexType> indexesToRepopulate);
}
