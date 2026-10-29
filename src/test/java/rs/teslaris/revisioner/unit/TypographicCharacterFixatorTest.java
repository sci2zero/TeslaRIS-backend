package rs.teslaris.revisioner.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.revisioner.util.fixators.impl.TypographicCharacterFixator;

public class TypographicCharacterFixatorTest {

    private final TypographicCharacterFixator typographicCharacterFixator =
        new TypographicCharacterFixator();

    private static MultilingualContentDTO content(String text) {
        var multilingualContent = new MultilingualContentDTO();
        multilingualContent.setContent(text);
        return multilingualContent;
    }

    @Test
    public void shouldReplaceQuotesDashesAndEllipsisInTitle() {
        var document = new JournalPublicationDTO();
        document.setTitle(List.of(content("Rad o „citatima“ – nastavak…")));

        typographicCharacterFixator.applyFix(document);

        assertEquals("Rad o \"citatima\" - nastavak...",
            document.getTitle().getFirst().getContent());
    }

    @Test
    public void shouldCollapseHarvestedWhitespaceAndDropZeroWidthCharacters() {
        var document = new JournalPublicationDTO();
        document.setDescription(List.of(content("  Apstrakt  sa   razmacima i ne​vidljivim  ")));

        typographicCharacterFixator.applyFix(document);

        assertEquals("Apstrakt sa razmacima i nevidljivim",
            document.getDescription().getFirst().getContent());
    }

    @Test
    public void shouldPreserveParagraphBreaksInDescription() {
        var document = new JournalPublicationDTO();
        document.setDescription(List.of(content("Prvi deo.\nDrugi deo.")));

        typographicCharacterFixator.applyFix(document);

        assertEquals("Prvi deo.\nDrugi deo.", document.getDescription().getFirst().getContent());
    }

    @Test
    public void shouldExpandLigaturesLeftBehindByPdfExtraction() {
        var document = new JournalPublicationDTO();
        document.setKeywords(List.of(content("deﬁnition")));

        typographicCharacterFixator.applyFix(document);

        assertEquals("definition", document.getKeywords().getFirst().getContent());
    }

    @Test
    public void shouldComposeDecomposedAccentedLetters() {
        var document = new JournalPublicationDTO();
        document.setTitle(List.of(content("Investigac\u0327a\u0303o em educac\u0327a\u0303o")));

        typographicCharacterFixator.applyFix(document);

        assertEquals("Investigação em educação",
            document.getTitle().getFirst().getContent());
    }

    @Test
    public void shouldPutSpacingDiacriticsBackOntoTheirLetters() {
        var document = new JournalPublicationDTO();
        document.setTitle(List.of(content("Jo\u02DCao e a cria\u00B8c\u02DCao da ci\u02C6encia")));

        typographicCharacterFixator.applyFix(document);

        assertEquals("João e a criação da ciência",
            document.getTitle().getFirst().getContent());
    }

    @Test
    public void shouldLeaveSpacingDiacriticThatComposesNoPortugueseLetterAsApostrophe() {
        var document = new JournalPublicationDTO();
        document.setTitle(List.of(content("What the data don\u00B4t say")));

        typographicCharacterFixator.applyFix(document);

        assertEquals("What the data don't say", document.getTitle().getFirst().getContent());
    }

    @Test
    public void shouldKeepContentThatNormalisingWouldEmpty() {
        var document = new JournalPublicationDTO();
        document.setTitle(List.of(content("​​")));

        typographicCharacterFixator.applyFix(document);

        assertEquals("​​", document.getTitle().getFirst().getContent());
    }

    @Test
    public void shouldNormalisePersonNamesAndBiography() {
        var person = new PersonSnapshotDTO();
        person.setPersonName(
            new PersonNameDTO(null, "O’Neill", null, "D’Angelo", null, null, null));
        person.setPersonOtherNames(
            List.of(new PersonNameDTO(null, "Jean‑Luc", null, "Picard", null, null, null)));
        person.setBiography(List.of(content("Radi na „projektu“.")));

        typographicCharacterFixator.applyFix(person);

        assertEquals("O'Neill", person.getPersonName().getFirstname());
        assertEquals("D'Angelo", person.getPersonName().getLastname());
        assertEquals("Jean-Luc", person.getPersonOtherNames().getFirst().getFirstname());
        assertEquals("Radi na \"projektu\".", person.getBiography().getFirst().getContent());
    }

    @Test
    public void shouldNormaliseEventTextFields() {
        var conference = new ConferenceDTO();
        conference.setName(List.of(content("Konferencija – 2024")));
        conference.setPlace(List.of(content("Novi Sad")));

        typographicCharacterFixator.applyFix(conference);

        assertEquals("Konferencija - 2024", conference.getName().getFirst().getContent());
        assertEquals("Novi Sad", conference.getPlace().getFirst().getContent());
    }

    @Test
    public void shouldNormalisePublisherTextFields() {
        var publisher = new PublisherDTO();
        publisher.setName(List.of(content("Izdavač „Prosveta“")));

        typographicCharacterFixator.applyFix(publisher);

        assertEquals("Izdavač \"Prosveta\"", publisher.getName().getFirst().getContent());
    }

    @Test
    public void shouldTolerateEntitiesWithoutTextFields() {
        assertDoesNotThrow(
            () -> typographicCharacterFixator.applyFix(new JournalPublicationDTO()));
        assertDoesNotThrow(() -> typographicCharacterFixator.applyFix(new PersonSnapshotDTO()));
        assertDoesNotThrow(() -> typographicCharacterFixator.applyFix(new ConferenceDTO()));
    }
}
