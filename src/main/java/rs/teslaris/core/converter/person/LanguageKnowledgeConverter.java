package rs.teslaris.core.converter.person;

import java.util.ArrayList;
import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.dto.person.LanguageKnowledgeResponseDTO;
import rs.teslaris.core.model.person.LanguageKnowledge;

public class LanguageKnowledgeConverter {

    public static LanguageKnowledgeResponseDTO toDTO(LanguageKnowledge languageKnowledge) {
        var dto = new LanguageKnowledgeResponseDTO();
        dto.setId(languageKnowledge.getId());
        dto.setName(MultilingualContentConverter.getMultilingualContentDTO(
            languageKnowledge.getName()));

        var language = languageKnowledge.getLanguage();
        if (Objects.nonNull(language)) {
            dto.setLanguage(new LanguageResponseDTO(language.getId(), language.getLanguageCode(),
                MultilingualContentConverter.getMultilingualContentDTO(language.getName())));
        }

        dto.setMotherTongue(languageKnowledge.getMotherTongue());
        dto.setReading(languageKnowledge.getReading());
        dto.setWriting(languageKnowledge.getWriting());
        dto.setSpeaking(languageKnowledge.getSpeaking());
        dto.setListening(languageKnowledge.getListening());
        dto.setAcademicReview(languageKnowledge.getAcademicReview());
        dto.setAcademicWriting(languageKnowledge.getAcademicWriting());
        dto.setOverall(languageKnowledge.getOverall());

        dto.setProofs(new ArrayList<>());
        languageKnowledge.getProofs()
            .forEach(proof -> dto.getProofs().add(DocumentFileConverter.toDTO(proof)));

        return dto;
    }
}
