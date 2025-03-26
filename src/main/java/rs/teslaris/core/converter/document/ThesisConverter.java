package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.document.Thesis;

@Component
public class ThesisConverter extends DocumentPublicationConverter {

    private static Integer daysOnPublicReview;


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
        thesisDTO.setTopicAcceptanceDate(thesis.getTopicAcceptanceDate());
        thesisDTO.setThesisDefenceDate(thesis.getThesisDefenceDate());
        thesisDTO.setIsOnPublicReview(thesis.getIsOnPublicReview());
        thesisDTO.setIsOnPublicReviewPause(thesis.getIsOnPublicReviewPause());
        thesisDTO.setPublicReviewDates(
            thesis.getPublicReviewStartDates().stream().sorted().toList());

        if (thesisDTO.getIsOnPublicReview()) {
            thesisDTO.setPublicReviewEnd(
                thesisDTO.getPublicReviewDates().getLast().plusDays(daysOnPublicReview));
        }

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

        thesis.getPreliminaryFiles().forEach(file -> {
            thesisDTO.getPreliminaryFiles().add(DocumentFileConverter.toDTO(file));
        });

        thesis.getPreliminarySupplements().forEach(supplement -> {
            thesisDTO.getPreliminarySupplements().add(DocumentFileConverter.toDTO(supplement));
        });

        thesis.getCommissionReports().forEach(commissionReport -> {
            thesisDTO.getCommissionReports().add(DocumentFileConverter.toDTO(commissionReport));
        });
    }

    @Value("${thesis.public-review.duration-days}")
    public void setConfigValue(Integer value) {
        ThesisConverter.daysOnPublicReview = value;
    }
}
