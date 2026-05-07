package rs.teslaris.core.service.impl.identifier;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.PersonIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.PersonIdentifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.PersonIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.cruddelegate.PersonIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.service.interfaces.identifier.PersonIdentifierService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Service
@Traceable
public class PersonIdentifierServiceImpl extends EntityIdentifierServiceImpl
    implements PersonIdentifierService {

    private final PersonIdentifierRepository personIdentifierRepository;

    private final PersonIdentifierJPAServiceImpl personIdentifierJPAService;

    private final PersonService personService;


    @Autowired
    public PersonIdentifierServiceImpl(EntityIdentifierRepository entityIdentifierRepository,
                                       IdentifierService identifierService,
                                       PersonIdentifierRepository personIdentifierRepository,
                                       PersonIdentifierJPAServiceImpl personIdentifierJPAService,
                                       PersonService personService) {
        super(entityIdentifierRepository, identifierService);
        this.personIdentifierRepository = personIdentifierRepository;
        this.personIdentifierJPAService = personIdentifierJPAService;
        this.personService = personService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityIdentifierResponseDTO> getIdentifiersForPerson(Integer personId,
                                                                     AccessLevel accessLevel) {
        return personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(personId,
            accessLevel).stream().map(
            EntityIdentifierConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PersonIdentifier createPersonIdentifier(PersonIdentifierDTO personIdentifierDTO,
                                                   Integer userId) {
        var newPersonIdentifier = new PersonIdentifier();

        setCommonFields(newPersonIdentifier, personIdentifierDTO);

        newPersonIdentifier.setPerson(personService.findOne(personIdentifierDTO.getPersonId()));

        return personIdentifierJPAService.save(newPersonIdentifier);
    }

    @Override
    @Transactional
    public void updatePersonIdentifier(Integer personIdentifierId,
                                       PersonIdentifierDTO personIdentifierDTO) {
        var personIdentifierToUpdate = personIdentifierJPAService.findOne(personIdentifierId);

        setCommonFields(personIdentifierToUpdate, personIdentifierDTO);

        personIdentifierToUpdate.setPerson(
            personService.findOne(personIdentifierDTO.getPersonId()));

        personIdentifierJPAService.save(personIdentifierToUpdate);
    }
}
