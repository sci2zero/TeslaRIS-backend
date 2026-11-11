package rs.teslaris.project.export;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.export.ProjectExportSource;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.identifier.FundingIdentifier;
import rs.teslaris.project.model.identifier.ProjectIdentifier;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.repository.identifier.FundingIdentifierRepository;
import rs.teslaris.project.repository.identifier.ProjectIdentifierRepository;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.util.ProjectScopeCalculator;

@Service
@RequiredArgsConstructor
public class ProjectExportSourceImpl implements ProjectExportSource {

    private final ProjectRepository projectRepository;

    private final FundingRepository fundingRepository;

    private final ProjectIdentifierRepository projectIdentifierRepository;

    private final FundingIdentifierRepository fundingIdentifierRepository;

    private final ProjectScopeCalculator projectScopeCalculator;

    private static PersonContributionRecord toRecord(PersonProjectContribution contribution) {
        var displayName = Objects.nonNull(contribution.getAffiliationStatement()) &&
            Objects.nonNull(contribution.getAffiliationStatement().getDisplayPersonName()) ?
            contribution.getAffiliationStatement().getDisplayPersonName().toString() : null;

        List<Integer> institutionIds = Objects.nonNull(contribution.getInstitutions()) ?
            contribution.getInstitutions().stream().map(BaseEntity::getId).toList() : List.of();

        return new PersonContributionRecord(contribution.getPerson(), displayName,
            contribution.getOrderNumber(), contribution.getContributionType(),
            contribution.getInvestigationRole(), institutionIds, contribution.getDateFrom(),
            contribution.getDateTo());
    }

    private static OrganisationContributionRecord toRecord(
        OrganisationUnitProjectContribution contribution) {
        return new OrganisationContributionRecord(contribution.getOrganisationUnit(),
            contribution.getDisplayOrganisationUnit(), contribution.getOrderNumber(),
            contribution.getContributionType(), contribution.getDateFrom(),
            contribution.getDateTo());
    }

    private static IdentifierRecord toRecord(ProjectIdentifier identifier) {
        return new IdentifierRecord(identifier.getIdentifier().getCode(), identifier.getValue(),
            identifier.getIdentifier().getUrlTemplate());
    }

    private static IdentifierRecord toRecord(FundingIdentifier identifier) {
        return new IdentifierRecord(identifier.getIdentifier().getCode(), identifier.getValue(),
            identifier.getIdentifier().getUrlTemplate());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectRecord> findAllModifiedProjects(Pageable pageable, boolean allTime) {
        return projectRepository.findAllModified(pageable, allTime).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FundingRecord> findAllModifiedFundings(Pageable pageable, boolean allTime) {
        return fundingRepository.findAllModified(pageable, allTime).map(this::toRecord);
    }

    private ProjectRecord toRecord(Project project) {
        if (project.getDeleted()) {
            return ProjectRecord.builder()
                .id(project.getId())
                .lastModification(project.getLastModification())
                .deleted(true)
                .build();
        }

        return ProjectRecord.builder()
            .id(project.getId())
            .lastModification(project.getLastModification())
            .deleted(false)
            .oldIds(project.getOldIds())
            .doi(project.getDoi())
            .raid(project.getRaid())
            .nationalId(project.getNationalId())
            .identifiers(projectIdentifierRepository
                .findIdentifiersForProjectAndIdentifierAccessLevel(project.getId(),
                    AccessLevel.OPEN)
                .stream().map(ProjectExportSourceImpl::toRecord).toList())
            .name(project.getName())
            .description(project.getDescription())
            .nameAbbreviation(project.getNameAbbreviation())
            .keywords(project.getKeywords())
            .uris(project.getUris())
            .dateFrom(project.getDateFrom())
            .dateTo(project.getDateTo())
            .status(project.getStatus())
            .collaborationType(project.getCollaborationType())
            .researchType(project.getResearchType())
            .notFunded(project.getNotFunded())
            .costs(project.getCosts())
            .persons(project.getPersons().stream()
                .sorted(Comparator.comparing(PersonProjectContribution::getOrderNumber,
                    Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ProjectExportSourceImpl::toRecord).toList())
            .organisations(project.getOrganisations().stream()
                .sorted(Comparator.comparing(OrganisationUnitProjectContribution::getOrderNumber,
                    Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ProjectExportSourceImpl::toRecord).toList())
            .institutionIds(
                new LinkedHashSet<>(projectScopeCalculator.organisationUnitIds(project)))
            .build();
    }

    private FundingRecord toRecord(Funding funding) {
        if (funding.getDeleted()) {
            return FundingRecord.builder()
                .id(funding.getId())
                .lastModification(funding.getLastModification())
                .deleted(true)
                .build();
        }

        var builder = FundingRecord.builder()
            .id(funding.getId())
            .lastModification(funding.getLastModification())
            .deleted(false)
            .oldIds(funding.getOldIds())
            .doi(funding.getDoi())
            .grantAgreementId(funding.getGrantAgreementId())
            .identifiers(fundingIdentifierRepository
                .findIdentifiersForFundingAndIdentifierAccessLevel(funding.getId(),
                    AccessLevel.OPEN)
                .stream().map(ProjectExportSourceImpl::toRecord).toList())
            .name(funding.getName())
            .description(funding.getDescription())
            .nameAbbreviation(funding.getNameAbbreviation())
            .keywords(funding.getKeywords())
            .uris(funding.getUris())
            .funder(funding.getFunder())
            .displayFunder(funding.getDisplayFunder())
            .displayCall(funding.getDisplayCall())
            .displayProgram(funding.getDisplayProgram())
            .fundingTypes(funding.getFundingTypes())
            .amount(funding.getAmount())
            .competitive(funding.getCompetitive())
            .renewable(funding.getRenewable())
            .dateSubmitted(funding.getDateSubmitted())
            .dateAwarded(funding.getDateAwarded())
            .dateFrom(funding.getDateFrom())
            .dateTo(funding.getDateTo())
            .oaMandated(funding.getOaMandated())
            .oaMandateUrl(funding.getOaMandateUrl())
            .internalInvestment(funding.getInternalInvestment());

        if (Objects.nonNull(funding.getFundingCall())) {
            builder.fundingCallName(funding.getFundingCall().getName());

            if (Objects.nonNull(funding.getFundingCall().getFundingProgram())) {
                builder.fundingProgramName(funding.getFundingCall().getFundingProgram().getName());
            }
        }

        var institutionIds = new LinkedHashSet<Integer>();

        // Do not load a deleted project, because this causes an error.
        if (Objects.nonNull(funding.getProject()) &&
            projectRepository.existsById(funding.getProject().getId())) {
            var project = toRecord(funding.getProject());
            builder.project(project);
            institutionIds.addAll(project.institutionIds());
        }

        if (Objects.nonNull(funding.getFunder())) {
            projectScopeCalculator.addWithSuperHierarchy(institutionIds,
                funding.getFunder().getId());
        }

        return builder.institutionIds(institutionIds).build();
    }
}
