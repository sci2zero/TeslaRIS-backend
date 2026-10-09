package rs.teslaris.project.revision;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.project.FundingQualityViewDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.revision.RevisionRestorer;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.service.interfaces.funding.FundingService;

/**
 * {@code updateFunding} writes only the funding's own fields - funding parts are built at creation
 * and agreements are managed through their own endpoints, so both are excluded from the snapshot.
 */
@Component
@RequiredArgsConstructor
public class FundingRevisionRestorer implements RevisionRestorer<FundingDTO> {

    private final FundingService fundingService;


    @Override
    public String entityType() {
        return EntityType.FUNDING.name();
    }

    @Override
    public Class<FundingDTO> dtoClass() {
        return FundingDTO.class;
    }

    /**
     * The calculator lives in {@code revisioner}, which may not depend on this module, so the
     * assessment reads a core-owned projection of the snapshot rather than FundingDTO itself.
     */
    @Override
    public Class<?> assessmentDtoClass() {
        return FundingQualityViewDTO.class;
    }

    @Override
    public void restore(Integer entityId, FundingDTO dto) {
        fundingService.updateFunding(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return fundingService.readFunding(entityId);
    }
}
