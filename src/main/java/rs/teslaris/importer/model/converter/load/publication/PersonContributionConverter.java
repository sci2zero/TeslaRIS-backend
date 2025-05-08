package rs.teslaris.importer.model.converter.load.publication;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.EmploymentTitle;
import rs.teslaris.core.model.document.PersonalTitle;
import rs.teslaris.core.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.OAIPMHParseUtility;

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
            if (Objects.nonNull(contribution)) {
                contribution.setIsMainContributor(i == 0);
                contribution.setIsCorrespondingContributor(false);
                contribution.setIsBoardPresident(false);
                contributions.add(contribution);
            }
        }
    }

    @Nullable
    private <T extends PersonAttributes> PersonDocumentContributionDTO createContribution(
        T contributor,
        DocumentContributionType contributionType,
        int orderNumber) {
        var contribution = new PersonDocumentContributionDTO();
        contribution.setContributionType(contributionType);
        contribution.setContributionDescription(new ArrayList<>());
        contribution.setDisplayAffiliationStatement(new ArrayList<>());

        var person = personService.findPersonByOldId(
            OAIPMHParseUtility.parseBISISID(contributor.getPerson().getOldId()));
        if (Objects.isNull(person)) {
            log.warn("No saved person with id: . Proceeding with unmanaged contribution." +
                contributor.getPerson().getOldId());
        } else {
            contribution.setPersonId(person.getId());
        }

        contribution.setInstitutionIds(new ArrayList<>());
        if (Objects.nonNull(contributor.getAffiliation())) {
            if (Objects.nonNull(contributor.getAffiliation().getOrgUnit()) &&
                Objects.nonNull(contributor.getAffiliation().getOrgUnit().getOldId())) {
                contribution.getInstitutionIds().add(
                    organisationUnitService.findOrganisationUnitByOldId(
                        OAIPMHParseUtility.parseBISISID(
                            contributor.getAffiliation().getOrgUnit().getOldId())).getId());
            } else {
                contribution.setDisplayAffiliationStatement(multilingualContentConverter.toDTO(
                    contributor.getAffiliation().getDisplayName()));
            }
        } else if (Objects.nonNull(person)) {
            contribution.getInstitutionIds()
                .addAll(personService.findInstitutionIdsForPerson(person.getId()));
        }

        if (Objects.nonNull(contributor.getDisplayName())) {
            contribution.setPersonName(
                new PersonNameDTO(null, contributor.getDisplayName(), "", "", null, null));
        }

        if (Objects.nonNull(contributor.getPerson().getTitle())) {
            contribution.setPersonalTitle(
                deducePersonalTitleFromName(contributor.getPerson().getTitle()));
        }

        if (Objects.nonNull(contributor.getPerson().getPositions()) &&
            !contributor.getPerson().getPositions().isEmpty()) {
            var employmentTitleName =
                contributor.getPerson().getPositions().stream().findFirst().get().getName();
            contribution.setEmploymentTitle(getEmploymentTitleFromName(employmentTitleName));
        }

        contribution.setOrderNumber(orderNumber + 1);

        return contribution;
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
        switch (name) {
            case "docent":
                return EmploymentTitle.ASSISTANT_PROFESSOR;
            case "vanredni profesor":
                return EmploymentTitle.ASSOCIATE_PROFESSOR;
            case "redovni profesor":
                return EmploymentTitle.FULL_PROFESSOR;
            case "profesor emeritus":
                return EmploymentTitle.PROFESSOR_EMERITUS;
            case "profesor u penziji":
                return EmploymentTitle.RETIRED_PROFESSOR;
            case "naučni - saradnik":
                return EmploymentTitle.SCIENTIFIC_COLLABORATOR;
            case "viši naučni - saradnik":
                return EmploymentTitle.SENIOR_SCIENTIFIC_COLLABORATOR;
            case "naučni savetnik":
                return EmploymentTitle.SCIENTIFIC_ADVISOR;
            case "profesor inženjer habilitovan":
                return EmploymentTitle.PROFESSOR_ENGINEER_HABILITATED;
        }

        log.info("Unable to deduce employment title while performing migration: '{}'", name);
        return null; // should never happen
    }
}
