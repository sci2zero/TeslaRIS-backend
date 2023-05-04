package rs.teslaris.core.service;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Service
public interface MultilingualContentService {

    Set<MultiLingualContent> getMultilingualContent(
        List<MultilingualContentDTO> multilingualContent);
}
