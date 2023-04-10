package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.Person;
import rs.teslaris.core.repository.PersonRepository;
import rs.teslaris.core.service.impl.PersonServiceImpl;

@SpringBootTest
public class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private PersonServiceImpl personService;

    @Test
    public void shouldReturnPersonWhenPersonExists() {
        // given
        var expectedPerson = new Person();

        when(personRepository.findById(1)).thenReturn(Optional.of(expectedPerson));

        // when
        Person actualPerson = personService.findPersonById(1);

        // then
        assertEquals(expectedPerson, actualPerson);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenPersonDoesNotExist() {
        // given
        when(personRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> personService.findPersonById(1));

        // then (NotFoundException should be thrown)
    }


}
