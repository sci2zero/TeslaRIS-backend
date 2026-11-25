package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rs.teslaris.core.converter.person.LanguageKnowledgeConverter;
import rs.teslaris.core.dto.person.LanguageKnowledgeResponseDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.person.LanguageKnowledge;
import rs.teslaris.core.model.person.LanguageLevel;

public class LanguageKnowledgeConverterTest {

    private LanguageKnowledge languageKnowledge() {
        var languageTag = new LanguageTag();
        languageTag.setLanguageTag("EN");

        var language = new Language();
        language.setId(7);
        language.setLanguageCode("ENG");
        language.setName(Set.of(new MultiLingualContent(languageTag, "English", 1)));

        var languageKnowledge = new LanguageKnowledge();
        languageKnowledge.setId(3);
        languageKnowledge.setLanguage(language);
        languageKnowledge.setMotherTongue(false);
        languageKnowledge.setReading(LanguageLevel.C1);
        languageKnowledge.setWriting(LanguageLevel.B2);
        languageKnowledge.setSpeaking(LanguageLevel.B1);
        languageKnowledge.setListening(LanguageLevel.C2);
        languageKnowledge.setAcademicReview(LanguageLevel.A2);
        languageKnowledge.setAcademicWriting(LanguageLevel.A1);
        languageKnowledge.setOverall(LanguageLevel.B2);
        languageKnowledge.getName().add(new MultiLingualContent(languageTag, "English", 1));

        return languageKnowledge;
    }

    @Test
    public void shouldConvertEveryLevelAndTheLanguage() {
        // Given
        var languageKnowledge = languageKnowledge();

        // When
        var dto = LanguageKnowledgeConverter.toDTO(languageKnowledge);

        // Then
        assertEquals(3, dto.getId());
        assertEquals(7, dto.getLanguage().getId());
        assertEquals("ENG", dto.getLanguage().getLanguageCode());
        assertEquals("English", dto.getLanguage().getName().getFirst().getContent());
        assertEquals("English", dto.getName().getFirst().getContent());
        assertEquals(false, dto.getMotherTongue());
        assertEquals(LanguageLevel.C1, dto.getReading());
        assertEquals(LanguageLevel.B2, dto.getWriting());
        assertEquals(LanguageLevel.B1, dto.getSpeaking());
        assertEquals(LanguageLevel.C2, dto.getListening());
        assertEquals(LanguageLevel.A2, dto.getAcademicReview());
        assertEquals(LanguageLevel.A1, dto.getAcademicWriting());
        assertEquals(LanguageLevel.B2, dto.getOverall());
    }

    @Test
    public void shouldConvertWhenLanguageIsMissing() {
        // Given
        var languageKnowledge = languageKnowledge();
        languageKnowledge.setLanguage(null);

        // When
        var dto = LanguageKnowledgeConverter.toDTO(languageKnowledge);

        // Then
        assertNull(dto.getLanguage());
        assertEquals(LanguageLevel.C1, dto.getReading());
    }

    @Test
    public void shouldKeepLanguagesWhenPersonResponseIsCopied() {
        // Given
        var person = new PersonResponseDTO();
        person.setLanguageKnowledges(
            List.of(LanguageKnowledgeConverter.toDTO(languageKnowledge())));

        // When
        var copy = new PersonResponseDTO(person);

        // Then
        assertEquals(1, copy.getLanguageKnowledges().size());
        var copied = copy.getLanguageKnowledges().getFirst();
        assertEquals(3, copied.getId());
        assertEquals(LanguageLevel.C1, copied.getReading());
        assertEquals("ENG", copied.getLanguage().getLanguageCode());
        assertNotSame(person.getLanguageKnowledges().getFirst(), copied);
    }

    @Test
    public void shouldConvertProofs() {
        // Given
        var languageKnowledge = languageKnowledge();
        var proof = new DocumentFile();
        proof.setId(11);
        proof.setFilename("Certificate.pdf");
        proof.setServerFilename("1111.pdf");
        languageKnowledge.getProofs().add(proof);

        // When
        var dto = LanguageKnowledgeConverter.toDTO(languageKnowledge);

        // Then
        assertEquals(1, dto.getProofs().size());
        assertEquals(11, dto.getProofs().getFirst().getId());
        assertEquals("Certificate.pdf", dto.getProofs().getFirst().getFileName());
    }

    @Test
    public void shouldKeepProofsWhenPersonResponseIsCopied() {
        // Given
        var languageKnowledge = languageKnowledge();
        var proof = new DocumentFile();
        proof.setId(11);
        proof.setFilename("Certificate.pdf");
        proof.setServerFilename("1111.pdf");
        languageKnowledge.getProofs().add(proof);

        var person = new PersonResponseDTO();
        person.setLanguageKnowledges(List.of(LanguageKnowledgeConverter.toDTO(languageKnowledge)));

        // When
        var copy = new PersonResponseDTO(person);

        // Then
        var copiedProofs = copy.getLanguageKnowledges().getFirst().getProofs();
        assertEquals(1, copiedProofs.size());
        assertEquals(11, copiedProofs.getFirst().getId());
        assertNotSame(person.getLanguageKnowledges().getFirst().getProofs().getFirst(),
            copiedProofs.getFirst());
    }

    @Test
    public void shouldConvertWhenThereAreNoProofs() {
        // Given
        var languageKnowledge = languageKnowledge();

        // When
        var dto = LanguageKnowledgeConverter.toDTO(languageKnowledge);

        // Then
        assertNotNull(dto.getProofs());
        assertEquals(0, dto.getProofs().size());
    }

    @Test
    public void shouldCopyWhenLanguagesAreMissing() {
        // Given
        var person = new PersonResponseDTO();
        var empty = new LanguageKnowledgeResponseDTO();

        // When
        var copy = new PersonResponseDTO(person);
        var copiedEmpty = new LanguageKnowledgeResponseDTO(empty);

        // Then
        assertNull(copy.getLanguageKnowledges());
        assertNull(copiedEmpty.getLanguage());
        assertNull(copiedEmpty.getName());
        assertNull(copiedEmpty.getProofs());
    }
}
