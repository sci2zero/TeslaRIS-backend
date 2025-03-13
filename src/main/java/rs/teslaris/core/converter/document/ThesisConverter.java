package rs.teslaris.core.converter.document;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
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
        } else {
            thesisDTO.setExternalOrganisationUnitName(
                MultilingualContentConverter.getMultilingualContentDTO(
                    thesis.getExternalOrganisationUnitName()));
        }

        thesisDTO.setThesisType(thesis.getThesisType());
        thesisDTO.setNumberOfPages(thesis.getNumberOfPages());

        if (Objects.nonNull(thesis.getLanguage())) {
            thesisDTO.setLanguageCode(thesis.getLanguage().getLanguageCode());
            thesisDTO.setLanguageId(thesis.getLanguage().getId());
        }

        if (Objects.nonNull(thesis.getWritingLanguage())) {
            thesisDTO.setWritingLanguageTagId(thesis.getWritingLanguage().getId());
        }

        if (Objects.nonNull(thesis.getResearchArea())) {
            thesisDTO.setResearchAreaId(thesis.getResearchArea().getId());
        }

        if (Objects.nonNull(thesis.getPublisher())) {
            thesisDTO.setPublisherId(thesis.getPublisher().getId());
        }
    }
}
