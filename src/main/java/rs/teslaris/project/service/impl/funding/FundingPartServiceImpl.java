package rs.teslaris.project.service.impl.funding;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.repository.funding.FundingPartRepository;
import rs.teslaris.project.service.interfaces.funding.FundingPartService;
import rs.teslaris.project.util.FundingPartFactory;

@Service
@RequiredArgsConstructor
public class FundingPartServiceImpl extends JPAServiceImpl<FundingPart>
    implements FundingPartService {

    private final FundingPartRepository fundingPartRepository;

    private final FundingPartFactory fundingPartFactory;

    @Override
    protected JpaRepository<FundingPart, Integer> getEntityRepository() {
        return fundingPartRepository;
    }

    @Override
    @Transactional
    public FundingPart createFundingPart(FundingPartDTO fundingPartDTO) {
        return save(fundingPartFactory.buildFundingPart(fundingPartDTO));
    }

    @Override
    @Transactional
    public void updateFundingPart(Integer fundingPartId, FundingPartDTO fundingPartDTO) {
        var fundingPart = findOne(fundingPartId);

        fundingPartFactory.setCommonFields(fundingPart, fundingPartDTO);

        save(fundingPart);
    }

    @Override
    @Transactional
    public void deleteFundingPart(Integer fundingPartId) {
        delete(fundingPartId);
    }

}
