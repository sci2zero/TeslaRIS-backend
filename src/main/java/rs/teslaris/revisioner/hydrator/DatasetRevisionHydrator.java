package rs.teslaris.revisioner.hydrator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Component
@RequiredArgsConstructor
public class DatasetRevisionHydrator
    implements RevisionHydrator<DatasetDTO> {

    @Override
    public String entityType() {
        return DocumentPublicationType.DATASET.name();
    }

    @Override
    public void hydrate(DatasetDTO dto) {
        // nothing to hydrate
    }
}
