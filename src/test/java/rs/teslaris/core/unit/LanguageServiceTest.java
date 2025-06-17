package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.service.impl.commontypes.LanguageServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class LanguageServiceTest {

    @Mock
    private LanguageRepository languageRepository;

    @InjectMocks
    private LanguageServiceImpl languageService;


    @Test
    public void shouldReturnSupportedLanguagesFromMessageFiles() {
        // given
        var english = new Language();
        english.setId(1);
        english.setLanguageCode("EN");
        english.setName(new HashSet<>());

        var serbian = new Language();
        serbian.setId(2);
        serbian.setLanguageCode("SR");
        serbian.setName(new HashSet<>());

        var german = new Language();
        german.setId(3);
        german.setLanguageCode("DE");
        german.setName(new HashSet<>());

        var allLanguages = List.of(english, serbian, german);
        when(languageRepository.findAll()).thenReturn(allLanguages);

        // when
        var result = languageService.getUISupportedLanguages();

        // then
        var languageCodes = result.stream()
            .map(LanguageResponseDTO::getLanguageCode)
            .collect(Collectors.toSet());

        assertTrue(languageCodes.contains("EN"));
        assertTrue(languageCodes.contains("SR"));
        assertFalse(languageCodes.contains("DE"));
    }

    @Test
    public void shouldThrowLoadingExceptionWhenMessageFilesCannotBeScanned() {
        // given
        LanguageServiceImpl service = new LanguageServiceImpl(languageRepository) {
            @Override
            protected org.springframework.core.io.Resource[] getResourcesForMessages()
                throws IOException {
                throw new IOException("test");
            }
        };

        // when & then
        assertThrows(LoadingException.class, service::getUISupportedLanguages);
    }

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
