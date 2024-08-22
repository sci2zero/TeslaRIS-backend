package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import java.util.Objects;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.document.Thesis;

public class ThesisConverter extends DocumentPublicationConverter {

    public static ThesisResponseDTO toDTO(Thesis thesis) {
        var thesisDTO = new ThesisResponseDTO();

        setCommonFields(thesis, thesisDTO);
        setThesisRelatedFields(thesis, thesisDTO);

        return thesisDTO;
    }

    private static void setThesisRelatedFields(Thesis thesis, ThesisResponseDTO thesisDTO) {
        if (Objects.nonNull(thesis.getOrganisationUnit())) {
            thesisDTO.setOrganisationUnitId(thesis.getOrganisationUnit().getId());
        }

        thesisDTO.setThesisType(thesis.getThesisType());
        thesisDTO.setNumberOfPages(thesis.getNumberOfPages());

        thesisDTO.setLanguageTagIds(new ArrayList<>());
        thesisDTO.setLanguageTagNames(new ArrayList<>());
        thesis.getLanguages().forEach(languageTag -> {
            thesisDTO.getLanguageTagNames().add(languageTag.getLanguageTag());
            thesisDTO.getLanguageTagIds().add(languageTag.getId());
        });

        if (Objects.nonNull(thesis.getResearchArea())) {
            thesisDTO.setResearchAreaId(thesis.getResearchArea().getId());
        }

        if (Objects.nonNull(thesis.getPublisher())) {
            thesisDTO.setPublisherId(thesis.getPublisher().getId());
        }
    }
}
