package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.service.impl.document.PublisherServiceImpl;

@SpringBootTest
public class PublisherServiceTest {

    @Mock
    private PublisherRepository publisherRepository;

    @InjectMocks
    private PublisherServiceImpl publisherService;

    @Test
    public void shouldReturnPublisherWhenItExists() {
        // given
        var expected = new Publisher();
        when(publisherRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = publisherService.findPublisherById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenPublisherDoesNotExist() {
        // given
        when(publisherRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> publisherService.findPublisherById(1));

        // then (NotFoundException should be thrown)
    }
}
