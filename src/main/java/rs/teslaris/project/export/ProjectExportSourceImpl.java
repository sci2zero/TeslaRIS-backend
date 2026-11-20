package rs.teslaris.project.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
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

    private final ProjectDocumentRepository projectDocumentRepository;

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
        var page = projectRepository.findAllModified(pageable, allTime);
        var pageData = loadPageData(page.getContent().stream()
            .filter(project -> !project.getDeleted()).map(Project::getId).toList());
        return page.map(project -> toRecord(project, pageData));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FundingRecord> findAllModifiedFundings(Pageable pageable, boolean allTime) {
        var page = fundingRepository.findAllModified(pageable, allTime);
        var pageData = loadPageData(page.getContent().stream()
            .filter(funding -> !funding.getDeleted() && Objects.nonNull(funding.getProject()))
            .map(funding -> funding.getProject().getId()).distinct().toList());
        return page.map(funding -> toRecord(funding, pageData));
    }

    /**
     * The document links and funding counts of all projects of a page, in two queries, so the
     * export does not query once per project.
     */
    private ProjectPageData loadPageData(Collection<Integer> projectIds) {
        if (projectIds.isEmpty()) {
            return new ProjectPageData(Map.of(), Map.of());
        }

        // One row per funding part; group them by project document.
        var linksByProjectDocument = new LinkedHashMap<Integer, LinkRow>();
        projectDocumentRepository.findDocumentLinksForProjects(projectIds).forEach(row -> {
            var link = linksByProjectDocument.computeIfAbsent((Integer) row[3],
                id -> new LinkRow((Integer) row[0], (Integer) row[1],
                    Objects.nonNull(row[2]) ? ((Enum<?>) row[2]).name() : null,
                    new ArrayList<>()));
            var fundingId = (Integer) row[4];
            if (Objects.nonNull(fundingId) && !link.fundingIds().contains(fundingId)) {
                link.fundingIds().add(fundingId);
            }
        });

        var links = new HashMap<Integer, List<DocumentLinkRecord>>();
        linksByProjectDocument.values().forEach(link ->
            links.computeIfAbsent(link.projectId(), id -> new ArrayList<>())
                .add(new DocumentLinkRecord(link.documentId(), link.relationType(),
                    List.copyOf(link.fundingIds()))));

        var fundingCounts = new HashMap<Integer, Integer>();
        fundingRepository.countFundingsForProjects(projectIds).forEach(row ->
            fundingCounts.put((Integer) row[0], ((Number) row[1]).intValue()));

        return new ProjectPageData(links, fundingCounts);
    }

    private ProjectRecord toRecord(Project project, ProjectPageData pageData) {
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
            .documents(pageData.documentLinks().getOrDefault(project.getId(), List.of()))
            .fundingCount(pageData.fundingCounts().getOrDefault(project.getId(), 0))
            .institutionIds(
                new LinkedHashSet<>(projectScopeCalculator.organisationUnitIds(project)))
            .build();
    }

    private FundingRecord toRecord(Funding funding, ProjectPageData pageData) {
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
            var project = toRecord(funding.getProject(), pageData);
            builder.project(project);
            institutionIds.addAll(project.institutionIds());
        }

        if (Objects.nonNull(funding.getFunder())) {
            projectScopeCalculator.addWithSuperHierarchy(institutionIds,
                funding.getFunder().getId());
        }

        return builder.institutionIds(institutionIds).build();
    }

    private record LinkRow(Integer projectId, Integer documentId, String relationType,
                           List<Integer> fundingIds) {
    }

    private record ProjectPageData(Map<Integer, List<DocumentLinkRecord>> documentLinks,
                                   Map<Integer, Integer> fundingCounts) {
    }
}
