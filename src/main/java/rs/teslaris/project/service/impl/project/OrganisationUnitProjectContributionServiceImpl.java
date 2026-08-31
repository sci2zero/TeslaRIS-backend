package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.OrganisationUnitProjectContributionRepository;
import rs.teslaris.project.service.interfaces.project.OrganisationUnitProjectContributionService;

import java.util.HashSet;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrganisationUnitProjectContributionServiceImpl
    extends JPAServiceImpl<OrganisationUnitProjectContribution> implements
    OrganisationUnitProjectContributionService {

    private final OrganisationUnitProjectContributionRepository
        organisationUnitProjectContributionRepository;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;

    private final PersonService personService;

    private final CurrencyService currencyService;

    @Override
    protected JpaRepository<OrganisationUnitProjectContribution, Integer> getEntityRepository() {
        return organisationUnitProjectContributionRepository;
    }

    @Override
    public OrganisationUnitProjectContribution createContribution(OrganisationUnitProjectContributionDTO dto, Project project) {
        var contribution = new OrganisationUnitProjectContribution();

        if (Objects.nonNull(dto.getOrganisationUnitId())) {
            contribution.setOrganisationUnit(
                    organisationUnitService.findOne(dto.getOrganisationUnitId()));
        } else {
            contribution.setDisplayOrganisationUnit(
                    multilingualContentService.getMultilingualContent(
                            dto.getDisplayOrganisationUnit()));
        }

        contribution.setContributionType(dto.getContributionType());
        contribution.setContributionDescription(
                multilingualContentService.getMultilingualContent(
                        dto.getContributionDescription()));
        contribution.setOrderNumber(dto.getOrderNumber());
        contribution.setApproveStatus(ApproveStatus.APPROVED);
        contribution.setDateFrom(dto.getDateFrom());
        contribution.setDateTo(dto.getDateTo());
        contribution.setUris(dto.getUris());
        contribution.setMainContributor(
                Objects.requireNonNullElse(dto.getIsMainContributor(), false));
        contribution.setFavorite(Objects.requireNonNullElse(dto.getFavorite(), false));

        if (Objects.nonNull(dto.getContactPersonId())) {
            contribution.setContactPerson(
                    personService.findOne(dto.getContactPersonId()));
        }

        contribution.setDisplayProject(
                multilingualContentService.getMultilingualContent(dto.getDisplayProject()));

        contribution.setFundingParts(new HashSet<>());
        dto.getFundingParts().forEach(partDto ->
                contribution.getFundingParts().add(buildContributionFundingPart(partDto, contribution)));

        contribution.setProject(project);

        // Adds saved contribution entity with id != null (if this part is omitted the Set will treat
        // each entity with null value id as the same one, thus overwriting/ignoring it each time)
        return organisationUnitProjectContributionRepository.save(contribution);
    }

    private FundingPart buildContributionFundingPart(FundingPartDTO partDto,
                                                     OrganisationUnitProjectContribution contribution) {
        var part = new FundingPart();

        part.setDescription(
                multilingualContentService.getMultilingualContent(partDto.getDescription()));

        part.setAmount(new MonetaryAmount());
        part.getAmount().setCurrency(currencyService.findOne(partDto.getAmount().getCurrencyId()));
        part.getAmount().setAmount(partDto.getAmount().getAmount());

        part.setOrganisationUnitContribution(contribution);

        return part;
    }

}
