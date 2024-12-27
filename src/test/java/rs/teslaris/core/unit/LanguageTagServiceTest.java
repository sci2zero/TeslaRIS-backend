package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.commontypes.LanguageTagDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.service.impl.commontypes.LanguageTagServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.LanguageTagReferenceConstraintException;
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

    @Test
    public void shouldCreateLanguageTag() {
        // given
        var dto = new LanguageTagDTO("English", "en");
        var expected = new LanguageTag();
        when(languageTagRepository.save(any(LanguageTag.class))).thenReturn(expected);

        // when
        var result = languageTagService.createLanguageTag(dto);

        // then
        assertEquals(expected, result);
        verify(languageTagRepository).save(any(LanguageTag.class));
    }

    @Test
    public void shouldUpdateLanguageTag() {
        // given
        var dto = new LanguageTagDTO("es", "Spanish");
        var existingTag = new LanguageTag();
        when(languageTagRepository.findById(1)).thenReturn(Optional.of(existingTag));
        when(languageTagRepository.save(existingTag)).thenReturn(existingTag);

        // when
        languageTagService.updateLanguageTag(1, dto);

        // then
        assertEquals("Spanish", existingTag.getDisplay());
        assertEquals("es", existingTag.getLanguageTag());
        verify(languageTagRepository).save(existingTag);
    }

    @Test
    public void shouldThrowExceptionWhenDeletingUsedLanguageTag() {
        // given
        when(languageTagRepository.isUsedInContent(1)).thenReturn(true);

        // when & then
        var exception = assertThrows(LanguageTagReferenceConstraintException.class, () -> {
            languageTagService.deleteLanguageTag(1);
        });

        assertEquals("languageTagInUse", exception.getMessage());
        verify(languageTagRepository, never()).deleteById(any());
    }

    @Test
    public void shouldDeleteLanguageTagWhenNotInUse() {
        // given
        when(languageTagRepository.isUsedInContent(1)).thenReturn(false);
        when(languageTagRepository.findById(1)).thenReturn(Optional.of(new LanguageTag()));

        // when
        languageTagService.deleteLanguageTag(1);

        // then
        verify(languageTagRepository).save(any());
    }

    @Test
    public void shouldReturnLanguageTagByValueWhenItExists() {
        // given
        var existingTag = new LanguageTag();
        when(languageTagRepository.findLanguageTagByLanguageTag("en")).thenReturn(
            Optional.of(existingTag));

        // when
        var result = languageTagService.findLanguageTagByValue("en");

        // then
        assertEquals(existingTag, result);
    }

    @Test
    public void shouldReturnNewLanguageTagWhenValueDoesNotExist() {
        // given
        when(languageTagRepository.findLanguageTagByLanguageTag("fr")).thenReturn(Optional.empty());

        // when
        var result = languageTagService.findLanguageTagByValue("fr");

        // then
        assertNotNull(result);
        assertNull(result.getDisplay());
        assertNull(result.getLanguageTag());
    }

    @Test
    public void shouldSearchLanguageTagsWithWildcard() {
        // Given
        var searchExpression = "*";
        var pageable = PageRequest.of(0, 10);
        var languageTagsPage = new PageImpl<>(List.of(
            new LanguageTag("en", "English"),
            new LanguageTag("es", "Spanish")
        ));
        when(languageTagRepository.searchLanguageTags("", pageable)).thenReturn(languageTagsPage);

        // When
        var result = languageTagService.searchLanguageTags(pageable, searchExpression);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals("en", result.getContent().get(0).getLanguageCode());
        assertEquals("Spanish", result.getContent().get(1).getDisplay());
        verify(languageTagRepository).searchLanguageTags("", pageable);
    }

    @Test
    public void shouldSearchLanguageTagsWithExpression() {
        // Given
        var searchExpression = "Eng";
        var pageable = PageRequest.of(0, 10);
        var languageTagsPage = new PageImpl<>(List.of(
            new LanguageTag("en", "English")
        ));
        when(languageTagRepository.searchLanguageTags(searchExpression, pageable)).thenReturn(
            languageTagsPage);

        // When
        var result = languageTagService.searchLanguageTags(pageable, searchExpression);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals("en", result.getContent().getFirst().getLanguageCode());
        assertEquals("English", result.getContent().getFirst().getDisplay());
        verify(languageTagRepository).searchLanguageTags(searchExpression, pageable);
    }

}
