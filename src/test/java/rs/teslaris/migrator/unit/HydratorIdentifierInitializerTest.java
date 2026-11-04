package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.IdentifierRepository;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.migrator.configuration.hydrator.HydratorIdentifierInitializer;
import rs.teslaris.migrator.converter.hydrator.HydratorSource;

@ExtendWith(MockitoExtension.class)
public class HydratorIdentifierInitializerTest {

    @Mock
    private IdentifierRepository identifierRepository;

    @Mock
    private LanguageTagService languageTagService;

    @InjectMocks
    private HydratorIdentifierInitializer initializer;


    @Test
    public void shouldCreateSciprojIdentifierWhenMissing() {
        var english = new LanguageTag("EN", "English");
        english.setId(1);
        when(identifierRepository.findByCode(HydratorSource.SCIPROJ_IDENTIFIER_CODE))
            .thenReturn(null);
        when(languageTagService.findLanguageTagByValue("EN")).thenReturn(english);

        initializer.ensureIdentifiers();

        var captor = ArgumentCaptor.forClass(Identifier.class);
        verify(identifierRepository).save(captor.capture());
        var saved = captor.getValue();
        assertEquals(HydratorSource.SCIPROJ_IDENTIFIER_CODE, saved.getCode());
        assertEquals("SciPROJ ID", saved.getTitle().iterator().next().getContent());
        assertEquals(Set.of(ApplicableEntityType.PROJECT), saved.getApplicableTypes());
        assertEquals(AccessLevel.OPEN, saved.getAccessLevel());
    }

    @Test
    public void shouldNotCreateSciprojIdentifierWhenItExists() {
        when(identifierRepository.findByCode(HydratorSource.SCIPROJ_IDENTIFIER_CODE))
            .thenReturn(new Identifier());

        initializer.ensureIdentifiers();

        verify(identifierRepository, never()).save(any());
    }

    @Test
    public void shouldNotCreateSciprojIdentifierWithoutEnglishTag() {
        when(identifierRepository.findByCode(HydratorSource.SCIPROJ_IDENTIFIER_CODE))
            .thenReturn(null);
        when(languageTagService.findLanguageTagByValue("EN")).thenReturn(new LanguageTag());

        initializer.ensureIdentifiers();

        verify(identifierRepository, never()).save(any());
    }
}
