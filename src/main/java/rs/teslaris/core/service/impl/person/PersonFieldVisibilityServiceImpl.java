package rs.teslaris.core.service.impl.person;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.person.PersonFieldVisibilityDTO;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.person.PersonFieldVisibility;
import rs.teslaris.core.repository.person.PersonFieldVisibilityRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.person.PersonFieldVisibilityService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Service
@RequiredArgsConstructor
@Traceable
public class PersonFieldVisibilityServiceImpl extends JPAServiceImpl<PersonFieldVisibility>
    implements PersonFieldVisibilityService {

    private final PersonFieldVisibilityRepository personFieldVisibilityRepository;

    private final PersonService personService;

    private final PersonIndexRepository personIndexRepository;


    @Override
    protected JpaRepository<PersonFieldVisibility, Integer> getEntityRepository() {
        return personFieldVisibilityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PersonFieldVisibilityDTO readPublicFieldConfiguration(Integer personId) {
        var savedConfiguration =
            personFieldVisibilityRepository.getFieldVisibilityConfiguration(personId)
                .orElse(new PersonFieldVisibility());

        return new PersonFieldVisibilityDTO(
            savedConfiguration.getPhoneNumberVisible(),
            savedConfiguration.getContactEmailVisible(),
            savedConfiguration.getDateOfBirthVisible(),
            savedConfiguration.getSexVisible(),
            savedConfiguration.getBirthplaceVisible()
        );
    }

    @Override
    @Transactional
    public void savePublicFieldConfiguration(Integer personId,
                                             PersonFieldVisibilityDTO configuration) {
        var savedConfiguration =
            personFieldVisibilityRepository.getFieldVisibilityConfiguration(personId)
                .orElse(new PersonFieldVisibility());

        if (Objects.isNull(savedConfiguration.getPerson())) {
            savedConfiguration.setPerson(personService.findOne(personId));
        }

        savedConfiguration.setPhoneNumberVisible(configuration.phoneNumberVisible());
        savedConfiguration.setContactEmailVisible(configuration.contactEmailVisible());
        savedConfiguration.setDateOfBirthVisible(configuration.dateOfBirthVisible());
        savedConfiguration.setSexVisible(configuration.sexVisible());
        savedConfiguration.setBirthplaceVisible(configuration.birthplaceVisible());

        personFieldVisibilityRepository.save(savedConfiguration);
        personFieldVisibilityRepository.flush();

        personIndexRepository.findByDatabaseId(personId).ifPresent(personIndex -> {
            personIndex.setDisplayBirthdate(savedConfiguration.getDateOfBirthVisible());
            personIndexRepository.save(personIndex);
        });
    }
}
