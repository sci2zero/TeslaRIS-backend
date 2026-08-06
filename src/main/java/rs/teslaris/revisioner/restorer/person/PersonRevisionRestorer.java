package rs.teslaris.revisioner.restorer.person;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

/**
 * Persons have no single edit method, so the restore replays the individual updates that produce a
 * person revision, in the order the fields depend on each other.
 * <p>
 * Only what a person revision actually captures is restored - involvements, prizes and
 * expertises/skills are separate entities with their own lifecycle and are left untouched.
 */
@Component
@RequiredArgsConstructor
public class PersonRevisionRestorer implements RevisionRestorer<PersonResponseDTO> {

    private final PersonService personService;


    @Override
    public String entityType() {
        return EntityType.PERSON.name();
    }

    @Override
    public Class<PersonResponseDTO> dtoClass() {
        return PersonResponseDTO.class;
    }

    @Override
    public void restore(Integer entityId, PersonResponseDTO dto) {
        if (Objects.nonNull(dto.getPersonalInfo())) {
            personService.updatePersonalInfo(entityId, dto.getPersonalInfo());
        }

        if (Objects.nonNull(dto.getPersonName())) {
            personService.updatePersonMainName(entityId, dto.getPersonName());
        }

        if (Objects.nonNull(dto.getPersonOtherNames())) {
            personService.setPersonOtherNames(dto.getPersonOtherNames(), entityId);
        }

        if (Objects.nonNull(dto.getBiography())) {
            personService.setPersonBiography(dto.getBiography(), entityId);
        }

        if (Objects.nonNull(dto.getKeyword())) {
            personService.setPersonKeyword(dto.getKeyword(), entityId);
        }
    }
}
