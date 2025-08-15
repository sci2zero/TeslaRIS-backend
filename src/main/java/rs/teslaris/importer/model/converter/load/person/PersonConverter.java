package rs.teslaris.importer.model.converter.load.person;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.ImportPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.model.oaipmh.person.Person;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@RequiredArgsConstructor
@Slf4j
public class PersonConverter implements RecordConverter<Person, ImportPersonDTO> {

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentConverter multilingualContentConverter;


    public ImportPersonDTO toDTO(Person person) {
        var dto = new ImportPersonDTO();

        dto.setOldId(OAIPMHParseUtility.parseBISISID(person.getOldId()));

        var personName = new PersonNameDTO();
        personName.setFirstname(person.getPersonName().getFirstNames());
        personName.setLastname(person.getPersonName().getFamilyNames());
        if (Objects.nonNull(person.getPersonName().getMiddleNames()) &&
            !person.getPersonName().getMiddleNames().isBlank()) {
            personName.setOtherName(person.getPersonName().getMiddleNames());
        }
        // TODO: Are other name variations supported?

        if (Objects.nonNull(person.getGender())) {
            dto.setSex(person.getGender().trim().equalsIgnoreCase("f") ? Sex.FEMALE : Sex.MALE);
        }

        if (Objects.nonNull(person.getElectronicAddresses())) {
            OAIPMHParseUtility.parseElectronicAddresses(person.getElectronicAddresses(), dto);
        }

        dto.setScopusAuthorId(person.getScopusAuthorId());
        dto.setOrcid(person.getOrcid());
        dto.setApvnt(person.getApvnt());
        dto.setPersonName(personName);

        if (Objects.nonNull(person.getBirthDate())) {
            dto.setLocalBirthDate(
                person.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        dto.setPlaceOfBirth((Objects.requireNonNullElse(person.getPlaceOfBirth(), "") + " " +
            Objects.requireNonNullElse(person.getCountryOfBirth(), "").trim()));

        if (Objects.nonNull(person.getCv()) && !person.getCv().isEmpty()) {
            person.getCv().forEach(
                cv -> cv.setValue(cv.getValue().replace("&nbsp;", " ").replace("\u00A0", " ")));
            dto.setBiography(multilingualContentConverter.toDTO(person.getCv()));
        }

        if (Objects.nonNull(person.getKeywords()) && !person.getKeywords().isEmpty()) {
            // remove <p> tags from some keywords
            person.getKeywords()
                .forEach(keyword -> keyword.setValue(Jsoup.parse(keyword.getValue()).text()));

            dto.setKeywords(multilingualContentConverter.toDTO(person.getKeywords()));
        }

        if (Objects.nonNull(person.getAddressLine()) && !person.getAddressLine().isBlank()) {
            dto.setAddressLine(multilingualContentConverter.toDTO(person.getAddressLine()));
        }

        if (Objects.nonNull(person.getPlace()) && !person.getPlace().isBlank()) {
            dto.setAddressCity(multilingualContentConverter.toDTO(person.getPlace()));
        }

        if (Objects.nonNull(person.getTitle()) && !person.getTitle().isBlank()) {
            dto.setDisplayTitle(multilingualContentConverter.toDTO(person.getTitle()));
        }

        // TODO: How to set research areas from serbian names?
        // TODO: What to do with year of birth?

        return dto;
    }

    public ArrayList<EmploymentDTO> toPersonEmployment(Person person, OrgUnit affiliation) {
        var organisationUnit = organisationUnitService.findOrganisationUnitByOldId(
            OAIPMHParseUtility.parseBISISID(affiliation.getOldId()));
        var employmentsToCreate = new ArrayList<EmploymentDTO>();

        if (Objects.nonNull(person.getPositions()) && !person.getPositions().isEmpty()) {
            person.getPositions().forEach(position -> {
                var dto = createBaseEmployment(organisationUnit, person);
                var positionName = position.getName().toLowerCase();
                var employmentPosition = getEmploymentPositionFromName(positionName);
                if (Objects.nonNull(employmentPosition)) {
                    dto.setEmploymentPosition(employmentPosition);
                }

                if (Objects.nonNull(position.getStartDate())) {
                    var startDate =
                        position.getStartDate().toInstant().atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    if (startDate.isBefore(LocalDate.now())) {
                        dto.setDateFrom(startDate);
                    }
                }

                if (Objects.nonNull(position.getEndDate())) {
                    dto.setDateTo(
                        position.getEndDate().toInstant().atZone(ZoneId.systemDefault())
                            .toLocalDate());
                }

                employmentsToCreate.add(dto);
            });
        } else {
            employmentsToCreate.add(createBaseEmployment(organisationUnit, person));
        }

        return employmentsToCreate;
    }

    private EmploymentDTO createBaseEmployment(OrganisationUnit organisationUnit, Person person) {
        var dto = new EmploymentDTO();
        dto.setInvolvementType(InvolvementType.EMPLOYED_AT);

        dto.setRole(new ArrayList<>());
        dto.setProofs(new ArrayList<>());
        dto.setAffiliationStatement(new ArrayList<>());

        if (Objects.isNull(organisationUnit)) {
            dto.setAffiliationStatement(
                multilingualContentConverter.toDTO(person.getAffiliation().getDisplayName()));
        } else {
            dto.setOrganisationUnitId(organisationUnit.getId());
        }

        return dto;
    }

    @Nullable
    private EmploymentPosition getEmploymentPositionFromName(String name) {
        switch (name) {
            case "saradnik":
                return EmploymentPosition.COLLABORATOR;
            case "saradnik praktikant", "asistent - pripravnik":
                return EmploymentPosition.ASSISTANT_TRAINEE;
            case "saradnik u nastavi":
                return EmploymentPosition.TEACHING_ASSOCIATE;
            case "asistent":
                return EmploymentPosition.TEACHING_ASSISTANT;
            case "asistent sa magistraturom":
                return EmploymentPosition.ASSISTANT_WITH_MAGISTRATE;
            case "asistent sa doktoratom":
                return EmploymentPosition.ASSISTANT_WITH_DOCTORATE;
            case "docent":
                return EmploymentPosition.ASSISTANT_PROFESSOR;
            case "vanredni profesor",
                 "vаnredni profesor": // for some reason "n" has different encoding
                return EmploymentPosition.ASSOCIATE_PROFESSOR;
            case "redovni profesor":
                return EmploymentPosition.FULL_PROFESSOR;
            case "profesor emeritus":
                return EmploymentPosition.PROFESSOR_EMERITUS;
            case "profesor u penziji":
                return EmploymentPosition.RETIRED_PROFESSOR;
            case "predavač":
                return EmploymentPosition.LECTURER;
            case "stariji predavač":
                return EmploymentPosition.SENIOR_LECTURER;
            case "nastavnik stranih jezika i veština":
                return EmploymentPosition.TEACHER_OF_FOREIGN_LANGUAGES_AND_SKILLS;
            case "profesor strukovnih studija":
                return EmploymentPosition.PROFESSOR_OF_VOCATIONAL_STUDIES;
            case "istraživač - pripravnik":
                return EmploymentPosition.RESEARCH_TRAINEE;
            case "istraživač - saradnik":
                return EmploymentPosition.RESEARCH_ASSOCIATE;
            case "istraživač":
                return EmploymentPosition.RESEARCHER;
            case "naučni - saradnik":
                return EmploymentPosition.SCIENTIFIC_COLLABORATOR;
            case "viši naučni - saradnik":
                return EmploymentPosition.SENIOR_SCIENTIFIC_COLLABORATOR;
            case "naučni savetnik":
                return EmploymentPosition.SCIENTIFIC_ADVISOR;
            case "stručni saradnik":
                return EmploymentPosition.EXPERT_ASSOCIATE;
            case "viši stručni saradnik":
                return EmploymentPosition.SENIOR_EXPERT_ASSOCIATE;
            case "stručni savetnik":
                return EmploymentPosition.EXPERT_ADVISOR;
            case "viši predavač":
                return EmploymentPosition.SENIOR_LECTURER;
            case "asistent sa master diplomom":
                return EmploymentPosition.ASSISTANT_WITH_MASTER;
        }

        log.info("Unable to deduce employment position while performing migration: '{}'", name);
        return null; // should never happen
    }
}
