package rs.teslaris.project.service.impl.identifier;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.EntityIdentifierServiceImpl;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.project.dto.identifier.FundingIdentifierDTO;
import rs.teslaris.project.model.identifier.FundingIdentifier;
import rs.teslaris.project.repository.identifier.FundingIdentifierRepository;
import rs.teslaris.project.service.impl.identifier.cruddelegate.FundingIdentifierJPAServiceImpl;
import rs.teslaris.project.service.interfaces.identifier.FundingIdentifierService;
import rs.teslaris.project.service.interfaces.funding.FundingService;

@Service
@Traceable
public class FundingIdentifierServiceImpl extends EntityIdentifierServiceImpl
    implements FundingIdentifierService {

    private final FundingIdentifierRepository fundingIdentifierRepository;

    private final FundingIdentifierJPAServiceImpl fundingIdentifierJPAService;

    private final FundingService fundingService;


    @Autowired
    public FundingIdentifierServiceImpl(EntityIdentifierRepository entityIdentifierRepository,
                                    IdentifierService identifierService,
                                    FundingIdentifierRepository fundingIdentifierRepository,
                                    FundingIdentifierJPAServiceImpl fundingIdentifierJPAService,
                                    FundingService fundingService) {
        super(entityIdentifierRepository, identifierService);
        this.fundingIdentifierRepository = fundingIdentifierRepository;
        this.fundingIdentifierJPAService = fundingIdentifierJPAService;
        this.fundingService = fundingService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityIdentifierResponseDTO> getIdentifiersForFunding(Integer fundingId,
                                                                  AccessLevel accessLevel) {
        return fundingIdentifierRepository.findIdentifiersForFundingAndIdentifierAccessLevel(fundingId,
            accessLevel).stream().map(EntityIdentifierConverter::toDTO).toList();
    }

    @Override
    @Transactional
    public FundingIdentifier createFundingIdentifier(FundingIdentifierDTO fundingIdentifierDTO) {
        var newFundingIdentifier = new FundingIdentifier();

        setCommonFields(newFundingIdentifier, fundingIdentifierDTO);

        newFundingIdentifier.setFunding(fundingService.findOne(fundingIdentifierDTO.getFundingId()));

        return fundingIdentifierJPAService.save(newFundingIdentifier);
    }

    @Override
    @Transactional
    public void updateFundingIdentifier(Integer fundingIdentifierId,
                                    FundingIdentifierDTO fundingIdentifierDTO) {
        var fundingIdentifierToUpdate = fundingIdentifierJPAService.findOne(fundingIdentifierId);

        setCommonFields(fundingIdentifierToUpdate, fundingIdentifierDTO);

        fundingIdentifierToUpdate.setFunding(fundingService.findOne(fundingIdentifierDTO.getFundingId()));

        fundingIdentifierJPAService.save(fundingIdentifierToUpdate);
    }
}
