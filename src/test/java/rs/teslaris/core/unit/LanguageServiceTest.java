package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.LanguageMigrationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.service.impl.commontypes.LanguageServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class LanguageServiceTest {

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private LanguageTagRepository languageTagRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

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

    @Test
    void shouldUpdateExistingLanguageWithNewMultilingualContent() {
        // Given
        var languageCode = "EN";
        var existingLanguage = mock(Language.class);
        var nameList = new HashSet<MultiLingualContent>();

        when(existingLanguage.getName()).thenReturn(nameList);
        when(languageRepository.getLanguageByLanguageCode(languageCode))
            .thenReturn(Optional.of(existingLanguage));

        var dto = mock(LanguageMigrationDTO.class);
        when(dto.languageCode()).thenReturn("en");
        when(dto.name()).thenReturn(List.of(
            new MultilingualContentDTO(1, "en", "English", 1)
        ));

        var multilingualContent = mock(MultiLingualContent.class);
        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(multilingualContent));

        // When
        languageService.migrateLanguage(dto);

        // Then
        assertNotNull(existingLanguage.getName());
        verify(languageRepository).save(existingLanguage);
    }

    @Test
    void shouldCreateNewLanguageWhenNotExists() {
        // Given
        var languageCode = "FR";
        when(languageRepository.getLanguageByLanguageCode(languageCode))
            .thenReturn(Optional.empty());

        var dto = mock(LanguageMigrationDTO.class);
        when(dto.languageCode()).thenReturn("fr");
        when(dto.name()).thenReturn(List.of(
            new MultilingualContentDTO(1, "fr", "Français", 1)
        ));

        var multilingualContent = mock(MultiLingualContent.class);
        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(multilingualContent));

        // When
        languageService.migrateLanguage(dto);

        // Then
        verify(languageRepository).save(argThat(language ->
            language.getLanguageCode().equals("FR")
        ));
    }

    @Test
    void shouldConvertToUpperCaseLanguageCode() {
        // Given
        var lowercaseCode = "de";
        var uppercaseCode = "DE";

        when(languageRepository.getLanguageByLanguageCode(uppercaseCode))
            .thenReturn(Optional.empty());

        var dto = mock(LanguageMigrationDTO.class);
        when(dto.languageCode()).thenReturn(lowercaseCode);
        when(dto.name()).thenReturn(List.of(
            new MultilingualContentDTO(1, "de", "Deutsch", 1)
        ));

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(mock(MultiLingualContent.class)));

        // When
        languageService.migrateLanguage(dto);

        // Then
        verify(languageRepository).save(argThat(language ->
            language.getLanguageCode().equals("DE")
        ));
    }

    @Test
    void shouldAddCyrillicContentWhenSerbianLanguagePresent() {
        // Given
        var languageCode = "SR";
        var existingLanguage = mock(Language.class);
        var nameList = new HashSet<MultiLingualContent>();

        when(existingLanguage.getName()).thenReturn(nameList);
        when(languageRepository.getLanguageByLanguageCode(languageCode))
            .thenReturn(Optional.of(existingLanguage));

        var serbianContent = new MultilingualContentDTO(1, "sr", "Srpski", 1);
        var dto = mock(LanguageMigrationDTO.class);
        when(dto.languageCode()).thenReturn("sr");
        when(dto.name()).thenReturn(List.of(serbianContent));

        var cyrillicTag = mock(LanguageTag.class);
        when(languageTagRepository.findLanguageTagByLanguageTag("sr-Cyrl"))
            .thenReturn(Optional.of(cyrillicTag));

        var multilingualContent = mock(MultiLingualContent.class);
        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(multilingualContent));

        // When
        languageService.migrateLanguage(dto);

        // Then
        assertNotNull(existingLanguage.getName());
    }

    @Test
    void shouldNotAddCyrillicContentWhenNoSerbianContentPresent() {
        // Given
        var languageCode = "EN";
        var existingLanguage = mock(Language.class);
        var nameList = new HashSet<MultiLingualContent>();

        when(existingLanguage.getName()).thenReturn(nameList);
        when(languageRepository.getLanguageByLanguageCode(languageCode))
            .thenReturn(Optional.of(existingLanguage));

        var englishContent = new MultilingualContentDTO(1, "en", "English", 1);
        var dto = mock(LanguageMigrationDTO.class);
        when(dto.languageCode()).thenReturn("en");
        when(dto.name()).thenReturn(List.of(englishContent));

        var cyrillicTag = mock(LanguageTag.class);
        when(languageTagRepository.findLanguageTagByLanguageTag("sr-Cyrl"))
            .thenReturn(Optional.of(cyrillicTag));

        var multilingualContent = mock(MultiLingualContent.class);
        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(multilingualContent));

        // When
        languageService.migrateLanguage(dto);

        // Then
        assertNotNull(existingLanguage.getName());
    }
}
