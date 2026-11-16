package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private static final String PROJECT_CODE = HydratorSource.SCIPROJ_PROJECT_IDENTIFIER_CODE;

    private static final String FUNDING_CODE = HydratorSource.SCIPROJ_FUNDING_IDENTIFIER_CODE;

    @Mock
    private IdentifierRepository identifierRepository;

    @Mock
    private LanguageTagService languageTagService;

    @InjectMocks
    private HydratorIdentifierInitializer initializer;


    private static LanguageTag english() {
        var english = new LanguageTag("EN", "English");
        english.setId(1);
        return english;
    }

    private static Identifier complete() {
        var identifier = new Identifier();
        identifier.setUrlTemplate("https://example.com/{ID}");
        identifier.getDescription().add(new MultiLingualContent(english(), "Custom", 1));
        return identifier;
    }

    @Test
    public void shouldCreateProjectAndFundingIdentifiersWhenMissing() {
        when(languageTagService.findLanguageTagByValue("EN")).thenReturn(english());

        initializer.ensureIdentifiers();

        var captor = ArgumentCaptor.forClass(Identifier.class);
        verify(identifierRepository, times(2)).save(captor.capture());
        var project = captor.getAllValues().get(0);
        assertEquals(PROJECT_CODE, project.getCode());
        assertEquals("SciPROJ Project ID", project.getTitle().iterator().next().getContent());
        assertEquals("Identifier of the project in SciPROJ",
            project.getDescription().iterator().next().getContent());
        assertEquals(Set.of(ApplicableEntityType.PROJECT), project.getApplicableTypes());
        assertEquals(AccessLevel.OPEN, project.getAccessLevel());
        assertEquals(SCIPROJ_URL_TEMPLATE, project.getUrlTemplate());

        var funding = captor.getAllValues().get(1);
        assertEquals(FUNDING_CODE, funding.getCode());
        assertEquals("SciPROJ Funding ID", funding.getTitle().iterator().next().getContent());
        assertEquals("Identifier of the funding in SciPROJ",
            funding.getDescription().iterator().next().getContent());
        assertEquals(Set.of(ApplicableEntityType.FUNDING), funding.getApplicableTypes());
        assertEquals(AccessLevel.OPEN, funding.getAccessLevel());
        assertEquals(SCIPROJ_URL_TEMPLATE, funding.getUrlTemplate());
    }

    @Test
    public void shouldRenameProjectIdentifierCreatedUnderLegacyCode() {
        var legacy = complete();
        legacy.setCode(HydratorSource.LEGACY_SCIPROJ_PROJECT_IDENTIFIER_CODE);
        when(identifierRepository.findByCode(
            HydratorSource.LEGACY_SCIPROJ_PROJECT_IDENTIFIER_CODE)).thenReturn(legacy);
        when(identifierRepository.findByCode(FUNDING_CODE)).thenReturn(complete());

        initializer.ensureIdentifiers();

        verify(identifierRepository).save(legacy);
        assertEquals(PROJECT_CODE, legacy.getCode());
        assertEquals("https://example.com/{ID}", legacy.getUrlTemplate());
    }

    @Test
    public void shouldAddMissingValuesToExistingIdentifier() {
        var existing = new Identifier();
        existing.setUrlTemplate("");
        when(identifierRepository.findByCode(PROJECT_CODE)).thenReturn(existing);
        when(identifierRepository.findByCode(FUNDING_CODE)).thenReturn(complete());
        when(languageTagService.findLanguageTagByValue("EN")).thenReturn(english());

        initializer.ensureIdentifiers();

        verify(identifierRepository).save(existing);
        assertEquals(SCIPROJ_URL_TEMPLATE, existing.getUrlTemplate());
        assertEquals("Identifier of the project in SciPROJ",
            existing.getDescription().iterator().next().getContent());
    }

    @Test
    public void shouldKeepValuesOfExistingIdentifiers() {
        var project = complete();
        var funding = complete();
        when(identifierRepository.findByCode(PROJECT_CODE)).thenReturn(project);
        when(identifierRepository.findByCode(FUNDING_CODE)).thenReturn(funding);

        initializer.ensureIdentifiers();

        verify(identifierRepository, never()).save(any());
        assertEquals("https://example.com/{ID}", project.getUrlTemplate());
        assertEquals("Custom", funding.getDescription().iterator().next().getContent());
    }

    @Test
    public void shouldAddOnlyMissingDescriptionToExistingIdentifier() {
        var existing = new Identifier();
        existing.setUrlTemplate("https://example.com/{ID}");
        when(identifierRepository.findByCode(PROJECT_CODE)).thenReturn(complete());
        when(identifierRepository.findByCode(FUNDING_CODE)).thenReturn(existing);
        when(languageTagService.findLanguageTagByValue("EN")).thenReturn(english());

        initializer.ensureIdentifiers();

        verify(identifierRepository).save(existing);
        assertEquals("https://example.com/{ID}", existing.getUrlTemplate());
        assertEquals("Identifier of the funding in SciPROJ",
            existing.getDescription().iterator().next().getContent());
    }

    @Test
    public void shouldNotCreateIdentifiersWithoutEnglishTag() {
        when(languageTagService.findLanguageTagByValue("EN")).thenReturn(new LanguageTag());

        initializer.ensureIdentifiers();

        verify(identifierRepository, never()).save(any());
    }
}
