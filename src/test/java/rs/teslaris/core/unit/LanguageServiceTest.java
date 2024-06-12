package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.service.impl.commontypes.LanguageServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class LanguageServiceTest {

    @Mock
    private LanguageRepository languageRepository;
    @InjectMocks
    private LanguageServiceImpl languageService;

    @Test
    public void shouldReturnLanguageWhenLanguageExists() {
        // given
        var expectedLanguage = new Language();
        expectedLanguage.setLanguageCode("ENG");

        when(languageRepository.findById(1)).thenReturn(Optional.of(expectedLanguage));

        // when
        var actualLanguage = languageService.findOne(1);

        // then
        assertEquals(expectedLanguage, actualLanguage);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenLanguageDoesNotExist() {
        // given
        when(languageRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> languageService.findOne(1));

        // then (NotFoundException should be thrown)
    }
}
