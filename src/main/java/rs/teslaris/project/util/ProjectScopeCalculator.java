package rs.teslaris.project.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.OrganisationUnitContribution;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.funding.FundingApplication;
import rs.teslaris.project.model.project.Project;

/**
 * The persons and institutions a project belongs to, which both the project index and the funding
 * index are scoped by.
 */
@Component
@RequiredArgsConstructor
public class ProjectScopeCalculator {

    private final InvolvementRepository involvementRepository;

    private final OrganisationUnitService organisationUnitService;


    public List<Integer> personIds(Project project) {
        return project.getPersons().stream()
            .map(PersonContribution::getPerson)
            .filter(Objects::nonNull)
            .map(Person::getId)
            .toList();
    }

    /**
     * The consortium, the institutions that employ the contributors and the funders, each with its
     * super hierarchy.
     */
    public List<Integer> organisationUnitIds(Project project) {
        var institutionIds = new HashSet<Integer>();

        project.getOrganisations().stream()
            .map(OrganisationUnitContribution::getOrganisationUnit)
            .filter(Objects::nonNull)
            .map(OrganisationUnit::getId)
            .forEach(organisationUnitId -> addWithSuperHierarchy(institutionIds,
                organisationUnitId));

        project.getPersons().stream()
            .map(PersonContribution::getPerson)
            .filter(Objects::nonNull)
            .forEach(person -> involvementRepository.findEmploymentsForPerson(person.getId())
                .stream()
                .filter(employment -> InvolvementType.EMPLOYED_AT.equals(
                    employment.getInvolvementType()))
                .forEach(employment -> addWithSuperHierarchy(institutionIds,
                    employment.getOrganisationUnit().getId())));

        project.getFunding().stream()
            .map(Funding::getFunder)
            .filter(Objects::nonNull)
            .map(OrganisationUnit::getId)
            .forEach(funderId -> addWithSuperHierarchy(institutionIds, funderId));

        project.getFundingApplications().stream()
            .map(FundingApplication::getFunding)
            .filter(Objects::nonNull)
            .map(Funding::getFunder)
            .filter(Objects::nonNull)
            .map(OrganisationUnit::getId)
            .forEach(funderId -> addWithSuperHierarchy(institutionIds, funderId));

        return new ArrayList<>(institutionIds);
    }

    public void addWithSuperHierarchy(Set<Integer> institutionIds, Integer organisationUnitId) {
        institutionIds.add(organisationUnitId);
        institutionIds.addAll(
            organisationUnitService.getSuperOUsHierarchyRecursive(organisationUnitId));
    }
}
