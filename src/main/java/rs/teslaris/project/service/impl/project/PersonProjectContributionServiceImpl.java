package rs.teslaris.project.service.impl.project;

import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonNameType;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.PersonProjectContributionRepository;
import rs.teslaris.project.service.interfaces.project.PersonProjectContributionService;

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

    private final ResearchAreaService researchAreaService;

    @Override
    protected JpaRepository<PersonProjectContribution, Integer> getEntityRepository() {
        return personProjectContributionRepository;
    }

    @Override
    public PersonProjectContribution createContribution(PersonProjectContributionDTO dto,
                                                        Project parent) {
        var contribution = new PersonProjectContribution();

        // Supports external affiliations (taken from core)
        var contributor = Objects.nonNull(dto.getPersonId()) ? personService.findOne(dto.getPersonId()) : null;
        contribution.setPerson(contributor);
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

        contribution.setAffiliationStatement(buildAffiliationStatement(dto, contributor));

        contribution.setContributionType(dto.getContributionType());
        contribution.setInvestigationRole(dto.getInvestigationRole());

        contribution.setOtherRoleDescription(
            multilingualContentService.getMultilingualContent(dto.getOtherRoleDescription()));

        contribution.setFundingParts(new HashSet<>());
        dto.getFundingParts().forEach(partDto ->
            contribution.getFundingParts()
                .add(buildContributionFundingPart(partDto, contribution)));

        contribution.setProject(parent);
        contribution.setFavorite(dto.getFavorite());
        contribution.setKeywords(
                multilingualContentService.getMultilingualContent(dto.getKeywords()));

        if (Objects.nonNull(dto.getResearchAreasId()) && !dto.getResearchAreasId().isEmpty()) {
            contribution.setResearchAreas(new HashSet<>(
                    researchAreaService.getResearchAreasByIds(
                            dto.getResearchAreasId().stream().toList())));
        }

        contribution.setDateFrom(dto.getDateFrom());
        contribution.setDateTo(dto.getDateTo());
        contribution.setUris(dto.getUris());
        contribution.setIsMainContributor(
            Objects.requireNonNullElse(dto.getIsMainContributor(), false));
        contribution.setIsInvitedContributor(
            Objects.requireNonNullElse(dto.getIsInvitedContributor(), false));

        contribution.setDisplayProject(
                multilingualContentService.getMultilingualContent(dto.getDisplayProject()));

        // Returns saved contribution entity with id != null (if this part is omitted the Set will treat
        // each entity with null value id as the same one, thus overwriting/ignoring it each time)
        return personProjectContributionRepository.save(contribution);
    }

    private AffiliationStatement buildAffiliationStatement(PersonProjectContributionDTO dto,
                                                           Person contributor) {
        var affiliation = new AffiliationStatement();

        affiliation.setDisplayAffiliationStatement(
            multilingualContentService.getMultilingualContent(
                dto.getDisplayAffiliationStatement()));

        affiliation.setDisplayPersonName(buildDisplayPersonName(dto, contributor));
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

    private PersonName buildDisplayPersonName(PersonProjectContributionDTO dto,
                                              Person contributor) {
        var nameDto = dto.getPersonName();

        if (Objects.nonNull(nameDto) &&
            (StringUtils.hasText(nameDto.getFirstname()) ||
                StringUtils.hasText(nameDto.getLastname()))) {
            return new PersonName(nameDto.getFirstname(), nameDto.getOtherName(),
                nameDto.getLastname(), nameDto.getDateFrom(), nameDto.getDateTo(),
                Objects.requireNonNullElse(nameDto.getPersonNameType(),
                    PersonNameType.CITATION_NAME));
        }

        if (Objects.nonNull(contributor) && Objects.nonNull(contributor.getName())) {
            var name = contributor.getName();
            return new PersonName(name.getFirstname(), name.getOtherName(), name.getLastname(),
                name.getDateFrom(), name.getDateTo(),
                Objects.requireNonNullElse(name.getNameType(), PersonNameType.CITATION_NAME));
        }

        return new PersonName();
    }

    private FundingPart buildContributionFundingPart(FundingPartDTO partDto,
                                                     PersonProjectContribution contribution) {
        var part = new FundingPart();

        part.setDescription(
            multilingualContentService.getMultilingualContent(partDto.getDescription()));

        part.setAmount(new MonetaryAmount());
        part.getAmount().setCurrency(
            currencyService.findOne(partDto.getAmount().getCurrencyId()));
        part.getAmount().setAmount(partDto.getAmount().getAmount());

        if (Objects.nonNull(partDto.getFundingId())) {
            part.setPersonContribution(contribution);
        }

        return part;
    }
}
