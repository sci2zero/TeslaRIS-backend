package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.impl.JournalServiceImpl;

@SpringBootTest
public class JournalServiceTest {

    @Mock
    private JournalRepository journalRepository;

    @InjectMocks
    private JournalServiceImpl journalService;

    @Test
    public void shouldReturnJournalWhenItExists() {
        // given
        var expected = new Journal();
        when(journalRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = journalService.findJournalById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenJournalDoesNotExist() {
        // given
        when(journalRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> journalService.findJournalById(1));

        // then (NotFoundException should be thrown)
    }
}
