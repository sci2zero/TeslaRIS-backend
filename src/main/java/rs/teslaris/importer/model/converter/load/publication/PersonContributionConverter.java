package rs.teslaris.importer.model.converter.load.publication;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.EmploymentTitle;
import rs.teslaris.core.model.document.PersonalTitle;
import rs.teslaris.core.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PersonContributionConverter {

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentConverter multilingualContentConverter;


    public <T extends PersonAttributes> void addContributors(List<T> contributors,
                                                             DocumentContributionType contributionType,
                                                             List<PersonDocumentContributionDTO> contributions) {
        if (Objects.isNull(contributors)) {
            return;
        }

        for (int i = 0; i < contributors.size(); i++) {
            var contributor = contributors.get(i);
            var contribution = createContribution(contributor, contributionType, i);
            contribution.setIsMainContributor(i == 0);
            contribution.setIsCorrespondingContributor(false);
            contribution.setIsBoardPresident(false);
            contributions.add(contribution);
        }
    }

    @NotNull
    private <T extends PersonAttributes> PersonDocumentContributionDTO createContribution(
        T contributor,
        DocumentContributionType contributionType,
        int orderNumber) {

        var contribution = initializeContribution(contributionType);

        var person = resolvePerson(contributor, contribution);

        setInstitutionInfo(contributor, person, contribution);
        setPersonNameIfPresent(contributor, contribution);

        if (Objects.nonNull(contribution.getPersonName()) && Objects.nonNull(person) &&
            !isNameEqual(contribution.getPersonName(), person.getName()) &&
            person.getOtherNames().stream()
                .noneMatch(name -> isNameEqual(contribution.getPersonName(), name))) {
            try {
                personService.addPersonOtherNames(contribution.getPersonName(), person.getId());

                log.info("Added new other name for Person {} -> {}", person.getId(),
                    contribution.getPersonName().toString());
            } catch (Exception e) {
                log.error("Unable to add other name for Person {}. Reason: {}", person.getId(),
                    e.getMessage());
            }
        }

        contribution.setOrderNumber(orderNumber + 1);

        return contribution;
    }

    private PersonDocumentContributionDTO initializeContribution(
        DocumentContributionType contributionType) {
        var contribution = new PersonDocumentContributionDTO();
        contribution.setContributionType(contributionType);
        contribution.setContributionDescription(new ArrayList<>());
        contribution.setDisplayAffiliationStatement(new ArrayList<>());
        contribution.setInstitutionIds(new ArrayList<>());
        return contribution;
    }

    @Nullable
    private <T extends PersonAttributes> Person resolvePerson(T contributor,
                                                              PersonDocumentContributionDTO contribution) {
        if (Objects.isNull(contributor.getPerson())) {
            return null;
        }

        var contributorPerson = contributor.getPerson();
        var person = personService.findPersonByOldId(
            OAIPMHParseUtility.parseBISISID(contributorPerson.getOldId()));

        if (Objects.isNull(person)) {
            log.warn("No saved person with id: {}. Proceeding with unmanaged contribution.",
                contributorPerson.getOldId());
        } else {
            contribution.setPersonId(person.getId());
        }

        if (Objects.nonNull(contributorPerson.getTitle())) {
            contribution.setPersonalTitle(
                deducePersonalTitleFromName(contributorPerson.getTitle()));
        }

        if (Objects.nonNull(contributorPerson.getPositions()) &&
            !contributorPerson.getPositions().isEmpty()) {
            var titleName = contributorPerson.getPositions().getFirst().getName();
            contribution.setEmploymentTitle(getEmploymentTitleFromName(titleName));
        }

        return person;
    }

    private <T extends PersonAttributes> void setInstitutionInfo(
        T contributor, Person person, PersonDocumentContributionDTO contribution) {

        var affiliation = contributor.getAffiliation();
        if (Objects.nonNull(affiliation) && Objects.nonNull(affiliation.getOrgUnit())) {
            var oldId = affiliation.getOrgUnit().getOldId();
            if (Objects.nonNull(oldId)) {
                var institution = organisationUnitService.findOrganisationUnitByOldId(
                    OAIPMHParseUtility.parseBISISID(oldId));

                if (Objects.nonNull(institution)) {
                    contribution.getInstitutionIds().add(institution.getId());
                } else {
                    contribution.setDisplayAffiliationStatement(
                        multilingualContentConverter.toDTO(affiliation.getDisplayName()));
                }
            } else {
                contribution.setDisplayAffiliationStatement(
                    multilingualContentConverter.toDTO(affiliation.getDisplayName()));
            }
        } else if (Objects.nonNull(person)) {
            contribution.getInstitutionIds().addAll(
                personService.findInstitutionIdsForPerson(person.getId()));
        }
    }

    private <T extends PersonAttributes> void setPersonNameIfPresent(
        T contributor, PersonDocumentContributionDTO contribution) {

        if (Objects.nonNull(contributor.getDisplayName())) {
            if (contributor.getDisplayName().contains(", ")) {
                var nameParts = contributor.getDisplayName().split(", ");
                if (nameParts.length == 2) {
                    contribution.setPersonName(
                        new PersonNameDTO(null, nameParts[1], "", nameParts[0], null, null));
                    return;
                }
            }

            contribution.setPersonName(
                new PersonNameDTO(null, contributor.getDisplayName(), "", "", null, null));
        }
    }

    @Nullable
    private PersonalTitle deducePersonalTitleFromName(String name) {
        if (name.contains("академик")) {
            return PersonalTitle.ACADEMIC;
        } else if (name.contains("ум")) {
            return PersonalTitle.DR_ART;
        } else if (name.startsWith("мр")) {
            return PersonalTitle.MR;
        } else if (name.startsWith("др")) {
            return PersonalTitle.DR;
        } else if (name.isBlank()) {
            return PersonalTitle.NONE;
        }

        return null;
    }

    @Nullable
    private EmploymentTitle getEmploymentTitleFromName(String name) {
        if (Objects.isNull(name) || name.isBlank()) {
            return null;
        }

        name = name.toLowerCase();
        switch (name) {
            case "akademik":
                return EmploymentTitle.ACADEMICIAN;
            case "docent", "doc":
                return EmploymentTitle.ASSISTANT_PROFESSOR;
            case "vanredni profesor",
                 "vаnredni profesor", // for some reason "n" has different encoding
                 "vanred. prof",
                 "vanr.prof.",
                 "vanredan profesor":
                return EmploymentTitle.ASSOCIATE_PROFESSOR;
            case "redovni profesor",
                 "profesor",
                 "prof.",
                 "prof. dr",
                 "red. prof",
                 "red prof.",
                 "red.prof.",
                 "redovan profesor":
                return EmploymentTitle.FULL_PROFESSOR;
            case "profesor emeritus", "emeritus":
                return EmploymentTitle.PROFESSOR_EMERITUS;
            case "profesor u penziji", "red. prof. u penziji":
                return EmploymentTitle.RETIRED_PROFESSOR;
            case "naučni - saradnik", "nauč.sar.":
                return EmploymentTitle.SCIENTIFIC_COLLABORATOR;
            case "viši naučni - saradnik", "viši nauč.sar.", "viši naučni saradnik":
                return EmploymentTitle.SENIOR_SCIENTIFIC_COLLABORATOR;
            case "naučni savetnik", "nauč.sav.":
                return EmploymentTitle.SCIENTIFIC_ADVISOR;
            case "profesor inženjer habilitovan", "prof. inž. habil":
                return EmploymentTitle.PROFESSOR_ENGINEER_HABILITATED;
        }

        log.info("Unable to deduce employment title while performing migration: '{}'", name);
        return null; // should never happen
    }

    private boolean isNameEqual(PersonNameDTO nameDTO, PersonName name) {
        return Objects.equals(
            name.getFirstname() != null ? name.getFirstname() : "",
            nameDTO.getFirstname() != null ? nameDTO.getFirstname() : ""
        ) &&
            Objects.equals(
                name.getLastname() != null ? name.getLastname() : "",
                nameDTO.getLastname() != null ? nameDTO.getLastname() : ""
            ) &&
            Objects.equals(
                name.getOtherName() != null ? name.getOtherName() : "",
                nameDTO.getOtherName() != null ? nameDTO.getOtherName() : ""
            );
    }
}
