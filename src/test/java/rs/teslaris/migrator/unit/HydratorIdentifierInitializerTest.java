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
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.IdentifierRepository;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.migrator.configuration.hydrator.HydratorIdentifierInitializer;
import rs.teslaris.migrator.converter.hydrator.HydratorSource;

@ExtendWith(MockitoExtension.class)
public class HydratorIdentifierInitializerTest {

    private static final String SCIPROJ_URL_TEMPLATE = "https://sciproj.ptcris.pt/{ID}/";

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
        assertEquals("Identifier of the project in SciPROJ",
            saved.getDescription().iterator().next().getContent());
        assertEquals(Set.of(ApplicableEntityType.PROJECT), saved.getApplicableTypes());
        assertEquals(AccessLevel.OPEN, saved.getAccessLevel());
        assertEquals(SCIPROJ_URL_TEMPLATE, saved.getUrlTemplate());
    }

    @Test
    public void shouldAddMissingValuesToExistingIdentifier() {
        var english = new LanguageTag("EN", "English");
        english.setId(1);
        var existing = new Identifier();
        existing.setUrlTemplate("");
        when(identifierRepository.findByCode(HydratorSource.SCIPROJ_IDENTIFIER_CODE))
            .thenReturn(existing);
        when(languageTagService.findLanguageTagByValue("EN")).thenReturn(english);

        initializer.ensureIdentifiers();

        verify(identifierRepository).save(existing);
        assertEquals(SCIPROJ_URL_TEMPLATE, existing.getUrlTemplate());
        assertEquals("Identifier of the project in SciPROJ",
            existing.getDescription().iterator().next().getContent());
    }

    @Test
    public void shouldKeepValuesOfExistingIdentifier() {
        var english = new LanguageTag("EN", "English");
        var existing = new Identifier();
        existing.setUrlTemplate("https://example.com/{ID}");
        existing.getDescription().add(new MultiLingualContent(english, "Custom", 1));
        when(identifierRepository.findByCode(HydratorSource.SCIPROJ_IDENTIFIER_CODE))
            .thenReturn(existing);

        initializer.ensureIdentifiers();

        verify(identifierRepository, never()).save(any());
        assertEquals("https://example.com/{ID}", existing.getUrlTemplate());
        assertEquals("Custom", existing.getDescription().iterator().next().getContent());
    }

    @Test
    public void shouldAddOnlyMissingDescriptionToExistingIdentifier() {
        var english = new LanguageTag("EN", "English");
        english.setId(1);
        var existing = new Identifier();
        existing.setUrlTemplate("https://example.com/{ID}");
        when(identifierRepository.findByCode(HydratorSource.SCIPROJ_IDENTIFIER_CODE))
            .thenReturn(existing);
        when(languageTagService.findLanguageTagByValue("EN")).thenReturn(english);

        initializer.ensureIdentifiers();

        verify(identifierRepository).save(existing);
        assertEquals("https://example.com/{ID}", existing.getUrlTemplate());
        assertEquals("Identifier of the project in SciPROJ",
            existing.getDescription().iterator().next().getContent());
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
