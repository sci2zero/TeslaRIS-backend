package rs.teslaris.core.assessment.converter;

import java.util.Objects;
import java.util.stream.Collectors;
import rs.teslaris.core.assessment.dto.CommissionResponseDTO;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;

public class CommissionConverter {

    public static CommissionResponseDTO toDTO(Commission commission) {
        return new CommissionResponseDTO(commission.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(commission.getDescription()),
            commission.getSources().stream().toList(), commission.getAssessmentDateFrom(),
            commission.getAssessmentDateTo(),
            commission.getDocumentsForAssessment().stream().map(Document::getId)
                .collect(Collectors.toList()),
            commission.getPersonsForAssessment().stream().map(Person::getId)
                .collect(Collectors.toList()),
            commission.getOrganisationUnitsForAssessment().stream().map(OrganisationUnit::getId)
                .collect(Collectors.toList()),
            commission.getFormalDescriptionOfRule(),
            Objects.nonNull(commission.getSuperCommission()) ?
                commission.getSuperCommission().getId() : null,
            Objects.nonNull(commission.getSuperCommission()) ?
                MultilingualContentConverter.getMultilingualContentDTO(
                    commission.getSuperCommission().getDescription()) : null);
    }
}
