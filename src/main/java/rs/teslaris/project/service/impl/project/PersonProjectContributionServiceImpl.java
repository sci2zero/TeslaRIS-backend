package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.PersonProjectContributionRepository;
import rs.teslaris.project.service.interfaces.funding.FundingService;
import rs.teslaris.project.service.interfaces.project.PersonProjectContributionService;

import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonProjectContributionServiceImpl extends JPAServiceImpl<PersonProjectContribution>
    implements
    PersonProjectContributionService {

    private final PersonProjectContributionRepository personProjectContributionRepository;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;

    private final CurrencyService currencyService;

    @Override
    protected JpaRepository<PersonProjectContribution, Integer> getEntityRepository() {
        return personProjectContributionRepository;
    }

    @Override
    public PersonProjectContribution createContribution(PersonProjectContributionDTO dto,
                                                        Project parent) {
        var contribution = new PersonProjectContribution();

        contribution.setPerson(personService.findOne(dto.getPersonId()));
        contribution.setOrderNumber(dto.getOrderNumber());
        contribution.setApproveStatus(ApproveStatus.APPROVED);

        contribution.setContributionDescription(
                multilingualContentService.getMultilingualContent(dto.getContributionDescription()));

        if (Objects.nonNull(dto.getInstitutionIds())) {
            var institutions = dto.getInstitutionIds().stream()
                    .map(organisationUnitService::findOne)
                    .collect(Collectors.toSet());
            contribution.setInstitutions(institutions);
        }

        contribution.setAffiliationStatement(buildAffiliationStatement(dto));

        contribution.setContributionType(dto.getContributionType());
        contribution.setInvestigationRole(dto.getInvestigationRole());

        contribution.setOtherRoleDescription(
                multilingualContentService.getMultilingualContent(dto.getOtherRoleDescription()));

        contribution.setFundingParts(new HashSet<>());
        dto.getFundingParts().forEach(partDto ->
                contribution.getFundingParts().add(buildContributionFundingPart(partDto, contribution)));

        contribution.setProject(parent);

        return contribution;
    }

    private AffiliationStatement buildAffiliationStatement(PersonProjectContributionDTO dto) {
        var affiliation = new AffiliationStatement();

        affiliation.setDisplayAffiliationStatement(
                multilingualContentService.getMultilingualContent(dto.getDisplayAffiliationStatement()));

        if (Objects.nonNull(dto.getPersonName())) {
            var personName = new PersonName();
            affiliation.setDisplayPersonName(personName);
        }
        if (Objects.nonNull(dto.getPostalAddress())) {
            var address = new PostalAddress();
            affiliation.setPostalAddress(address);
        }
        if (Objects.nonNull(dto.getContact())) {
            var contact = new Contact();
            affiliation.setContact(contact);
        }

        return affiliation;
    }

    private FundingPart buildContributionFundingPart(FundingPartDTO partDto, PersonProjectContribution contribution) {
        var part = new FundingPart();

        part.setDescription(
                multilingualContentService.getMultilingualContent(partDto.getDescription()));

        part.setAmount(new MonetaryAmount());
        part.getAmount().setCurrency(
                currencyService.findOne(partDto.getAmount().getCurrencyId()));
        part.getAmount().setAmount(partDto.getAmount().getAmount());

        if (Objects.nonNull(partDto.getFundingId())) {
            part.setPersonProjectContribution(contribution);
        }

        return part;
    }
}
