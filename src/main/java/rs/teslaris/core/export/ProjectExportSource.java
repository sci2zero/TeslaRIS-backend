package rs.teslaris.core.export;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.model.commontypes.MonetaryAmount;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.funding.FundingType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectInvestigationRole;
import rs.teslaris.core.model.project.ProjectCollaborationType;
import rs.teslaris.core.model.project.ProjectResearchType;
import rs.teslaris.core.model.project.ProjectStatus;

/**
 * Read the records in the transaction that gets them, because they contain lazy data.
 */
public interface ProjectExportSource {

    Page<ProjectRecord> findAllModifiedProjects(Pageable pageable, boolean allTime);

    Page<FundingRecord> findAllModifiedFundings(Pageable pageable, boolean allTime);

    @Builder
    record ProjectRecord(
        Integer id,
        Date lastModification,
        Boolean deleted,
        Set<String> oldIds,
        String doi,
        String raid,
        String nationalId,
        List<IdentifierRecord> identifiers,
        Set<MultiLingualContent> name,
        Set<MultiLingualContent> description,
        Set<MultiLingualContent> nameAbbreviation,
        Set<MultiLingualContent> keywords,
        Set<String> uris,
        LocalDate dateFrom,
        LocalDate dateTo,
        ProjectStatus status,
        ProjectCollaborationType collaborationType,
        ProjectResearchType researchType,
        Boolean notFunded,
        MonetaryAmount costs,
        List<PersonContributionRecord> persons,
        List<OrganisationContributionRecord> organisations,
        Set<Integer> institutionIds
    ) {
    }

    @Builder
    record FundingRecord(
        Integer id,
        Date lastModification,
        Boolean deleted,
        Set<String> oldIds,
        String doi,
        String grantAgreementId,
        List<IdentifierRecord> identifiers,
        ProjectRecord project,
        Set<MultiLingualContent> name,
        Set<MultiLingualContent> description,
        Set<MultiLingualContent> nameAbbreviation,
        Set<MultiLingualContent> keywords,
        Set<String> uris,
        OrganisationUnit funder,
        Set<MultiLingualContent> displayFunder,
        Set<MultiLingualContent> displayCall,
        Set<MultiLingualContent> displayProgram,
        Set<MultiLingualContent> fundingCallName,
        Set<MultiLingualContent> fundingProgramName,
        Set<FundingType> fundingTypes,
        MonetaryAmount amount,
        Boolean competitive,
        Boolean renewable,
        LocalDate dateSubmitted,
        LocalDate dateAwarded,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean oaMandated,
        String oaMandateUrl,
        Boolean internalInvestment,
        Set<Integer> institutionIds
    ) {
    }

    record PersonContributionRecord(
        Person person,
        String displayName,
        Integer orderNumber,
        PersonProjectContributionType contributionType,
        PersonProjectInvestigationRole investigationRole,
        List<Integer> institutionIds,
        LocalDate dateFrom,
        LocalDate dateTo
    ) {
    }

    record OrganisationContributionRecord(
        OrganisationUnit organisationUnit,
        Set<MultiLingualContent> displayName,
        Integer orderNumber,
        OrganisationUnitProjectContributionType contributionType,
        LocalDate dateFrom,
        LocalDate dateTo
    ) {
    }

    record IdentifierRecord(String code, String value, String urlTemplate) {
    }
}
