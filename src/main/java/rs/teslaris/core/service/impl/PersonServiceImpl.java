package rs.teslaris.core.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.person.PersonToPersonDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDto;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.CountryService;
import rs.teslaris.core.service.LanguageTagService;
import rs.teslaris.core.service.OrganisationUnitService;
import rs.teslaris.core.service.PersonNameService;
import rs.teslaris.core.service.PersonService;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;

    private final PersonIndexRepository personIndexRepository;

    private final OrganisationUnitService organisationUnitService;

    private final CountryService countryService;

    private final LanguageTagService languageTagService;

    private final PersonNameService personNameService;

    @Value("${person.approved_by_default}")
    private Boolean approvedByDefault;


    @Override
    public Person findPersonById(Integer id) {
        return personRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Person with given ID does not exist."));
    }

    @Override
    @Transactional
    public PersonResponseDto readPersonWithBasicInfo(Integer id) {
        var person = personRepository.findApprovedPersonById(id)
            .orElseThrow(() -> new NotFoundException("Person with given ID does not exist."));
        return PersonToPersonDTO.toDTO(person);
    }

    @Override
    @Transactional
    public boolean isPersonEmployedInOrganisationUnit(Integer personId,
                                                      Integer organisationUnitId) {
        var person = findPersonById(personId);

        for (var involvement : person.getInvolvements()) {
            if (involvement.getInvolvementType() == InvolvementType.EMPLOYED_AT &&
                Objects.equals(involvement.getOrganisationUnit().getId(), organisationUnitId)) {
                return true;
                // TODO: add recursive check
            }
        }

        return false;
    }

    @Override
    @Transactional
    public Person createPersonWithBasicInfo(BasicPersonDTO personDTO) {
        var defaultApproveStatus =
            approvedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED;

        var personNameDTO = personDTO.getPersonName();
        var personName = new PersonName(personNameDTO.getFirstname(), personNameDTO.getOtherName(),
            personNameDTO.getLastname(), personDTO.getLocalBirthDate(), null);

        var personalContact = new Contact(personDTO.getContactEmail(), personDTO.getPhoneNumber());
        var personalInfo = new PersonalInfo(personDTO.getLocalBirthDate(), null, personDTO.getSex(),
            new PostalAddress(), personalContact);

        var employmentInstitution =
            organisationUnitService.findOrganisationUnitById(personDTO.getOrganisationUnitId());

        var currentEmployment = new Employment(null, null, defaultApproveStatus, new HashSet<>(),
            InvolvementType.EMPLOYED_AT, new HashSet<>(), null, employmentInstitution,
            personDTO.getEmploymentPosition(), new HashSet<>());

        var newPerson = new Person();
        newPerson.setName(personName);
        newPerson.setPersonalInfo(personalInfo);
        newPerson.setApvnt(personDTO.getApvnt());
        newPerson.setMnid(personDTO.getMnid());
        newPerson.setOrcid(personDTO.getOrcid());
        newPerson.setScopusAuthorId(personDTO.getScopusAuthorId());
        newPerson.addInvolvement(currentEmployment);
        newPerson.setApproveStatus(defaultApproveStatus);

        var savedPerson = personRepository.save(newPerson);
        newPerson.setId(savedPerson.getId());

        indexPerson(savedPerson, 0);

        return newPerson;
    }

    @Override
    @Transactional
    public void setPersonBiography(List<MultilingualContentDTO> biographyDTO, Integer personId) {
        var personToUpdate = findPersonById(personId);
        personToUpdate.getBiography().clear();
        biographyDTO.stream().map(biography -> {
            var languageTag = languageTagService.findLanguageTagById(biography.getLanguageTagId());
            return new MultiLingualContent(languageTag, biography.getContent(),
                biography.getPriority());
        }).forEach(biography -> {
            personToUpdate.getBiography().add(biography);
            personRepository.save(personToUpdate);
        });
    }

    @Override
    @Transactional
    public void setPersonKeyword(List<MultilingualContentDTO> keywordDTO, Integer personId) {
        var personToUpdate = findPersonById(personId);
        personToUpdate.getKeyword().clear();
        keywordDTO.stream().map(keyword -> {
            var languageTag = languageTagService.findLanguageTagById(keyword.getLanguageTagId());
            return new MultiLingualContent(languageTag, keyword.getContent(),
                keyword.getPriority());
        }).forEach(keyword -> {
            personToUpdate.getBiography().add(keyword);
            personRepository.save(personToUpdate);
        });
    }

    @Override
    @Transactional
    public void setPersonMainName(Integer personNameId, Integer personId) {
        var personToUpdate = findPersonById(personId);
        var chosenName = personNameService.findPersonNameById(personNameId);

        personToUpdate.getOtherNames().add(personToUpdate.getName());
        personToUpdate.setName(chosenName);
        personToUpdate.getOtherNames().remove(chosenName);

        personRepository.save(personToUpdate);
        indexPerson(personToUpdate, personToUpdate.getId());
    }

    @Override
    @Transactional
    public void setPersonOtherNames(List<PersonNameDTO> personNameDTO, Integer personId) {
        var personToUpdate = findPersonById(personId);

        var personNameIds = personToUpdate.getOtherNames().stream().map(PersonName::getId)
            .collect(Collectors.toList());

        personToUpdate.getOtherNames().clear();
        personNameService.deletePersonNamesWithIds(personNameIds);

        personNameDTO.stream().map(
                personName -> new PersonName(personName.getFirstname(), personName.getOtherName(),
                    personName.getLastname(), personName.getDateFrom(), personName.getDateTo()))
            .forEach(personName -> {
                personToUpdate.getOtherNames().add(personName);
                personRepository.save(personToUpdate);
            });
    }

    @Override
    @Transactional
    public void updatePersonalInfo(PersonalInfoDTO personalInfo, Integer personId) {
        var personToUpdate = findPersonById(personId);
        personToUpdate.setApvnt(personalInfo.getApvnt());
        personToUpdate.setMnid(personalInfo.getMnid());
        personToUpdate.setOrcid(personalInfo.getOrcid());
        personToUpdate.setScopusAuthorId(personalInfo.getScopusAuthorId());

        var personalInfoToUpdate = personToUpdate.getPersonalInfo();
        personalInfoToUpdate.setPlaceOfBrith(personalInfo.getPlaceOfBrith());
        personalInfoToUpdate.setLocalBirthDate(personalInfo.getLocalBirthDate());
        personalInfoToUpdate.setSex(personalInfo.getSex());

        if (personalInfo.getPostalAddress() != null) {
            var country =
                countryService.findCountryById(personalInfo.getPostalAddress().getCountryId());
            personalInfoToUpdate.getPostalAddress().setCountry(country);

            personToUpdate.getPersonalInfo().getPostalAddress().getStreetAndNumber().clear();
            setPersonStreetAndNumberInfo(personToUpdate, personalInfoToUpdate, personalInfo);

            personToUpdate.getPersonalInfo().getPostalAddress().getCity().clear();
            setPersonCityInfo(personToUpdate, personalInfoToUpdate, personalInfo);
        }

        personalInfoToUpdate.getContact()
            .setContactEmail(personalInfo.getContact().getContactEmail());
        personalInfoToUpdate.getContact()
            .setPhoneNumber(personalInfo.getContact().getPhoneNumber());

        personRepository.save(personToUpdate);
        indexPerson(personToUpdate, personToUpdate.getId());
    }

    @Override
    public void approvePerson(Integer personId, Boolean approve) {
        var personToBeApproved = findPersonById(personId);

        var approveStatus = approve ? ApproveStatus.APPROVED : ApproveStatus.DECLINED;
        if (personToBeApproved.getApproveStatus().equals(ApproveStatus.REQUESTED)) {
            personToBeApproved.setApproveStatus(approveStatus);
        }

        personRepository.save(personToBeApproved);
    }

    @Transactional
    private void setPersonStreetAndNumberInfo(Person personToUpdate,
                                              PersonalInfo personalInfoToUpdate,
                                              PersonalInfoDTO personalInfo) {
        personalInfo.getPostalAddress().getStreetAndNumber().stream().map(streetAndNumber -> {
            var languageTag =
                languageTagService.findLanguageTagById(streetAndNumber.getLanguageTagId());
            return new MultiLingualContent(languageTag, streetAndNumber.getContent(),
                streetAndNumber.getPriority());
        }).forEach(streetAndNumberContent -> {
            personalInfoToUpdate.getPostalAddress().getStreetAndNumber()
                .add(streetAndNumberContent);
            personRepository.save(personToUpdate);
        });
    }

    @Transactional
    private void setPersonCityInfo(Person personToUpdate, PersonalInfo personalInfoToUpdate,
                                   PersonalInfoDTO personalInfo) {
        personalInfo.getPostalAddress().getCity().stream().map(city -> {
            var languageTag = languageTagService.findLanguageTagById(city.getLanguageTagId());
            return new MultiLingualContent(languageTag, city.getContent(), city.getPriority());
        }).forEach(city -> {
            personalInfoToUpdate.getPostalAddress().getCity().add(city);
            personRepository.save(personToUpdate);
        });
    }

    private void indexPerson(Person savedPerson, Integer personDatabaseId) {
        var personIndex = getPersonIndexForId(personDatabaseId);

        setPersonIndexProperties(personIndex, savedPerson);

        setPersonIndexEmploymentDetails(personIndex, savedPerson);

        personIndexRepository.save(personIndex);
    }

    private PersonIndex getPersonIndexForId(Integer personDatabaseId) {
        PersonIndex personIndex;
        if (personDatabaseId > 0) {
            personIndex = personIndexRepository.findByDatabaseId(personDatabaseId).orElseThrow(
                () -> new NotFoundException("Person index with given database ID does not exist."));
        } else {
            personIndex = new PersonIndex();
        }
        return personIndex;
    }

    private void setPersonIndexProperties(PersonIndex personIndex, Person savedPerson) {
        personIndex.setName(
            savedPerson.getName().getFirstname() + " " + savedPerson.getName().getOtherName() +
                " " + savedPerson.getName().getLastname());
        personIndex.setBirthdate(savedPerson.getPersonalInfo().getLocalBirthDate().toString());
        personIndex.setDatabaseId(savedPerson.getId());
    }

    private void setPersonIndexEmploymentDetails(PersonIndex personIndex, Person savedPerson) {
        var employmentInstitutions = savedPerson.getInvolvements().stream()
            .filter(i -> i.getInvolvementType() == InvolvementType.EMPLOYED_AT)
            .map(Involvement::getOrganisationUnit).collect(Collectors.toList());

        personIndex.setEmploymentInstitutionsId(
            employmentInstitutions.stream().map(BaseEntity::getId).collect(Collectors.toList()));

        var employments_sr = new StringBuilder();
        var employments_other = new StringBuilder();
        for (var organisationUnit : employmentInstitutions) {
            var institutionName_sr = new StringBuilder();
            var institutionName_other = new StringBuilder();

            organisationUnit.getName().stream()
                .filter(mc -> mc.getLanguage().getLanguageTag().startsWith("SR"))
                .forEach(mc -> institutionName_sr.append(mc.getContent()).append(" | "));
            organisationUnit.getName().stream()
                .filter(mc -> !mc.getLanguage().getLanguageTag().startsWith("SR"))
                .forEach(mc -> institutionName_other.append(mc.getContent()).append(" | "));
            employments_sr.append(institutionName_sr)
                .append(organisationUnit.getNameAbbreviation());
            employments_other.append(institutionName_other);
        }
        personIndex.setEmploymentsSr(employments_sr.toString());
        personIndex.setEmployments(employments_other.toString());
    }
}
