package rs.teslaris.core.service.impl;

import java.util.HashSet;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.OrganisationUnitService;
import rs.teslaris.core.service.PersonService;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;

    private final OrganisationUnitService organisationUnitService;

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
}
