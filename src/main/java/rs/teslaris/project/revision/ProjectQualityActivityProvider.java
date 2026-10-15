package rs.teslaris.project.revision;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.OrganisationUnitContributionDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.revision.QualityActivityProvider;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.repository.project.OrganisationUnitProjectContributionRepository;
import rs.teslaris.project.repository.project.PersonProjectContributionRepository;

@Component
@RequiredArgsConstructor
public class ProjectQualityActivityProvider implements QualityActivityProvider {

    private final PersonProjectContributionRepository personProjectContributionRepository;

    private final OrganisationUnitProjectContributionRepository
        organisationUnitProjectContributionRepository;


    @Override
    public String entityType() {
        return EntityType.PROJECT.name();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonContributionDTO> personContributions(Integer entityId) {
        return personProjectContributionRepository.findByProjectId(entityId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisationUnitContributionDTO> organisationUnitContributions(Integer entityId) {
        return organisationUnitProjectContributionRepository.findByProjectId(entityId).stream()
            .map(this::toDTO)
            .toList();
    }

    private PersonContributionDTO toDTO(PersonProjectContribution contribution) {
        var dto = new PersonContributionDTO();

        dto.setId(contribution.getId());
        dto.setPersonId(Objects.nonNull(contribution.getPerson())
            ? contribution.getPerson().getId()
            : null);
        dto.setOrderNumber(contribution.getOrderNumber());
        dto.setDateFrom(contribution.getDateFrom());
        dto.setDateTo(contribution.getDateTo());
        dto.setIsMainContributor(contribution.getIsMainContributor());
        dto.setPersonName(displayName(contribution));
        dto.setIsInvitedContributor(contribution.getIsInvitedContributor());
        dto.setResearchAreasId(Objects.nonNull(contribution.getResearchAreas())
            ? contribution.getResearchAreas().stream().map(ResearchArea::getId)
            .collect(Collectors.toCollection(HashSet::new))
            : new HashSet<>());

        return dto;
    }

    private PersonNameDTO displayName(PersonProjectContribution contribution) {
        if (Objects.isNull(contribution.getAffiliationStatement()) ||
            Objects.isNull(contribution.getAffiliationStatement().getDisplayPersonName())) {
            return null;
        }

        var name = contribution.getAffiliationStatement().getDisplayPersonName();
        var dto = new PersonNameDTO();

        dto.setFirstname(name.getFirstname());
        dto.setOtherName(name.getOtherName());
        dto.setLastname(name.getLastname());

        return dto;
    }

    private OrganisationUnitContributionDTO toDTO(
        OrganisationUnitProjectContribution contribution) {
        return new OrganisationUnitContributionDTO(
            contribution.getId(),
            Objects.nonNull(contribution.getOrganisationUnit())
                ? contribution.getOrganisationUnit().getId()
                : null,
            contribution.getDateFrom(),
            contribution.getDateTo());
    }
}
