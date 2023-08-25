package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.repository.person.PersonNameRepository;
import rs.teslaris.core.service.impl.person.PersonNameServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class PersonNameServiceTest {

    @Mock
    private PersonNameRepository personNameRepository;

    @InjectMocks
    private PersonNameServiceImpl personNameService;

    @Test
    public void shouldReturnPersonNameWhenItExists() {
        // given
        var expected = new PersonName();
        when(personNameRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = personNameService.findOne(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenPersonNameDoesNotExist() {
        // given
        when(personNameRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> personNameService.findOne(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldDeletePersonNamesWithValidIds() {
        // given
        Integer id1 = 1;
        Integer id2 = 2;
        PersonName personName1 = new PersonName();
        personName1.setId(id1);
        PersonName personName2 = new PersonName();
        personName2.setId(id2);
        var personNameIds = Arrays.asList(id1, id2);

        when(personNameRepository.findById(id1)).thenReturn(Optional.of(personName1));
        when(personNameRepository.findById(id2)).thenReturn(Optional.of(personName2));

        // when
        personNameService.deletePersonNamesWithIds(personNameIds);

        // then
        verify(personNameRepository, times(1)).save(personName1);
        verify(personNameRepository, times(1)).save(personName2);
        verify(personNameRepository, never()).delete(any());
    }

    @Test
    public void shouldNotDeleteWhenInputArrayIsEmpty() {
        // given
        var personNameIds = new ArrayList<Integer>();

        // when
        personNameService.deletePersonNamesWithIds(personNameIds);

        // then
        verify(personNameRepository, never()).delete(any());
    }

    @Test
    public void shouldThrowNotFoundExceptionAndNotDeleteWhenIdsDoNotExist() {
        // given
        Integer id1 = 1;
        Integer id2 = 2;
        var personNameIds = Arrays.asList(id1, id2);

        when(personNameRepository.findById(id1)).thenReturn(Optional.empty());
        when(personNameRepository.findById(id2)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> personNameService.deletePersonNamesWithIds(personNameIds));

        // then, also (NotFoundException should be thrown)
        verify(personNameRepository, never()).delete(any());
    }
}
