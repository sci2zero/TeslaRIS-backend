package rs.teslaris.project.service.impl.funding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.repository.commontypes.NotificationRepository;
import rs.teslaris.core.repository.document.PersonContributionRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.person.PersonContributionServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.project.dto.funding.FundingCallDTO;
import rs.teslaris.project.model.funding.FundingCall;
import rs.teslaris.project.model.funding.PersonFundingCallContribution;
import rs.teslaris.project.service.interfaces.funding.PersonFundingCallContributionService;

@Service
public class PersonFundingCallContributionServiceImpl extends PersonContributionServiceImpl
    implements PersonFundingCallContributionService {

    @Autowired
    public PersonFundingCallContributionServiceImpl(
        PersonService personService,
        OrganisationUnitService organisationUnitService,
        MultilingualContentService multilingualContentService,
        PersonContributionRepository personContributionRepository,
        UserRepository userRepository,
        NotificationRepository notificationRepository,
        InvolvementService involvementService) {
        super(personService, organisationUnitService, multilingualContentService,
            personContributionRepository, userRepository, notificationRepository,
            involvementService);
    }

    @Override
    @Transactional
    public void setPersonFundingContributionsForFundingCall(FundingCall fundingCall,
                                                            FundingCallDTO fundingCallDTO) {
        fundingCallDTO.getContributors().forEach(contributionDTO -> {
            var contribution = new PersonFundingCallContribution();
            setPersonContributionCommonFields(contribution, contributionDTO);

            contribution.setContributionType(contributionDTO.getContributionType());

            var addedPreviously = fundingCall.getContributors().stream().anyMatch(
                previousContribution ->
                    compareContributions(previousContribution, contribution)
            );

            if (!addedPreviously) {
                fundingCall.addContributor(contribution);
            }
        });
    }
}
