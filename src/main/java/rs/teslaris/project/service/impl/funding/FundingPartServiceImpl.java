package rs.teslaris.project.service.impl.funding;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.repository.funding.FundingPartRepository;
import rs.teslaris.project.service.interfaces.funding.FundingPartService;
import rs.teslaris.project.service.interfaces.funding.FundingProposalService;
import rs.teslaris.project.service.interfaces.funding.FundingService;
import rs.teslaris.project.service.interfaces.project.ProjectDocumentService;
import rs.teslaris.project.service.interfaces.project.ProjectEventService;

@Service
@RequiredArgsConstructor
public class FundingPartServiceImpl extends JPAServiceImpl<FundingPart>
    implements FundingPartService {

    private final FundingPartRepository fundingPartRepository;

    private final FundingService fundingService;

    private final MultilingualContentService multilingualContentService;

    private final CurrencyService currencyService;

    private final ProjectEventService projectEventService;

    private final ProjectDocumentService projectDocumentService;

    private final FundingProposalService fundingProposalService;


    @Override
    protected JpaRepository<FundingPart, Integer> getEntityRepository() {
        return fundingPartRepository;
    }

    @Override
    @Transactional
    public FundingPart createFundingPart(FundingPartDTO fundingPartDTO) {
        var fundingPart = new FundingPart();

        setCommonFields(fundingPart, fundingPartDTO);

        return save(fundingPart);
    }

    @Override
    @Transactional
    public void updateFundingPart(Integer fundingPartId, FundingPartDTO fundingPartDTO) {
        var fundingPart = findOne(fundingPartId);

        clearCommonFields(fundingPart);
        setCommonFields(fundingPart, fundingPartDTO);

        save(fundingPart);
    }

    @Override
    @Transactional
    public void deleteFundingPart(Integer fundingPartId) {
        delete(fundingPartId);
    }

    private void setCommonFields(FundingPart fundingPart, FundingPartDTO dto) {
        fundingPart.setFunding(fundingService.findOne(dto.getFundingId()));
        fundingPart.setDescription(
            multilingualContentService.getMultilingualContent(dto.getDescription()));

        if (Objects.isNull(fundingPart.getCosts())) {
            fundingPart.setCosts(new MonetaryAmount());
        }

        fundingPart.getCosts().setCurrency(currencyService.findOne(dto.getCosts().getCurrencyId()));
        fundingPart.getCosts().setAmount(dto.getCosts().getAmount());

        if (Objects.nonNull(dto.getProjectEventId())) {
            fundingPart.setProjectEvent(projectEventService.findOne(dto.getProjectEventId()));
        } else if (Objects.nonNull(dto.getFundingProposalId())) {
            fundingPart.setFundingProposal(
                fundingProposalService.findOne(dto.getFundingProposalId()));
        } else if (Objects.nonNull(dto.getProjectDocumentId())) {
            fundingPart.setProjectDocument(
                projectDocumentService.findOne(dto.getProjectDocumentId()));
        } else if (Objects.nonNull(dto.getForFundingId())) {
            fundingPart.setForFunding(fundingService.findOne(dto.getForFundingId()));
        } else {
            throw new ReferenceConstraintException(
                "Funding part must belong to one of the following: funding, funding proposal, project event or project event.");
        }
    }

    private void clearCommonFields(FundingPart fundingPart) {
        fundingPart.setProjectEvent(null);
        fundingPart.setProjectDocument(null);
        fundingPart.setFundingProposal(null);
        fundingPart.setForFunding(null);

    }
}
