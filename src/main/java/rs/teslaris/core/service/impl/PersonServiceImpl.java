package rs.teslaris.core.service.impl;

import java.util.HashSet;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.CountryService;
import rs.teslaris.core.service.LanguageTagService;
import rs.teslaris.core.service.OrganisationUnitService;
import rs.teslaris.core.service.PersonService;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;

    private final OrganisationUnitService organisationUnitService;

    private final CountryService countryService;

    private final LanguageTagService languageTagService;


    @Override
    public Person findPersonById(Integer id) {
        return personRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Person with given ID does not exist."));
    }

    @Override
    @Transactional
    public Person createPersonWithBasicInfo(BasicPersonDTO personDTO) {
        var personNameDTO = personDTO.getPersonName();
        var personName = new PersonName(personNameDTO.getFirstname(), personNameDTO.getOtherName(),
            personNameDTO.getLastname(), personDTO.getLocalBirthDate(), null);

        var personalContact = new Contact(personDTO.getContactEmail(), personDTO.getPhoneNumber());
        var personalInfo = new PersonalInfo(personDTO.getLocalBirthDate(), null, personDTO.getSex(),
            new PostalAddress(), personalContact);

        var employmentInstitution = organisationUnitService.findOrganisationalUnitById(
            personDTO.getOrganisationUnitId());

        var currentEmployment =
            new Employment(null, null, ApproveStatus.APPROVED, new HashSet<>(),
                InvolvementType.EMPLOYED_AT, new HashSet<>(), null,
                employmentInstitution, personDTO.getEmploymentPosition(),
                new HashSet<>());

        var newPerson = new Person();
        newPerson.setName(personName);
        newPerson.setPersonalInfo(personalInfo);
        newPerson.setApvnt(personDTO.getApvnt());
        newPerson.setMnid(personDTO.getMnid());
        newPerson.setOrcid(personDTO.getOrcid());
        newPerson.setScopusAuthorId(personDTO.getScopusAuthorId());
        newPerson.addInvolvement(currentEmployment);
        newPerson.setApproveStatus(ApproveStatus.APPROVED);

        var savedPerson = personRepository.save(newPerson);
        newPerson.setId(savedPerson.getId());
        return newPerson;
    }

    @Override
    @Transactional
    public void setPersonOtherNames(List<PersonNameDTO> personNameDTO, Integer personId) {
        var personToUpdate = findPersonById(personId);
        personToUpdate.getOtherNames().clear();

        personNameDTO.stream()
            .map(personName -> new PersonName(
                personName.getFirstname(),
                personName.getOtherName(),
                personName.getLastname(),
                personName.getDateFrom(),
                personName.getDateTo()
            ))
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

        var country =
            countryService.findCountryById(personalInfo.getPostalAddress().getCountryId());
        personalInfoToUpdate.getPostalAddress().setCountry(country);

        personToUpdate.getPersonalInfo().getPostalAddress().getStreetAndNumber().clear();
        for (var streetAndNumber : personalInfo.getPostalAddress().getStreetAndNumber()) {
            var languageTag = languageTagService.findLanguageTagById(
                streetAndNumber.getLanguageTagId());
            personalInfoToUpdate.getPostalAddress().getStreetAndNumber().add(
                new MultiLingualContent(languageTag, streetAndNumber.getContent(),
                    streetAndNumber.getPriority()));
            personRepository.save(personToUpdate);
        }

        personToUpdate.getPersonalInfo().getPostalAddress().getCity().clear();
        for (var city : personalInfo.getPostalAddress().getCity()) {
            var languageTag = languageTagService.findLanguageTagById(
                city.getLanguageTagId());
            personalInfoToUpdate.getPostalAddress().getCity()
                .add(new MultiLingualContent(languageTag, city.getContent(), city.getPriority()));
            personRepository.save(personToUpdate);
        }

        personalInfoToUpdate.getContact()
            .setContactEmail(personalInfo.getContact().getContactEmail());
        personalInfoToUpdate.getContact()
            .setPhoneNumber(personalInfo.getContact().getPhoneNumber());

        personRepository.save(personToUpdate);
    }
}
