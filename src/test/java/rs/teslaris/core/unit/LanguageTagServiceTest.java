package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.service.impl.commontypes.LanguageTagServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class LanguageTagServiceTest {

    @Mock
    private LanguageTagRepository languageTagRepository;

    @InjectMocks
    private LanguageTagServiceImpl languageTagService;

    @Test
    public void shouldReturnLanguageTagWhenItExists() {
        // given
        var expected = new LanguageTag();
        when(languageTagRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = languageTagService.findOne(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenLanguageTagDoesNotExist() {
        // given
        when(languageTagRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> languageTagService.findOne(1));

        // then (NotFoundException should be thrown)
    }
}
