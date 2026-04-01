package rs.teslaris.core.converter.person;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.person.involvement.EmploymentPositionDTO;
import rs.teslaris.core.model.person.EmploymentPositionHierarchy;

public class EmploymentPositionConverter {

    public static EmploymentPositionDTO toDTO(EmploymentPositionHierarchy employmentPosition) {
        return new EmploymentPositionDTO(employmentPosition.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(
                employmentPosition.getName()),
            employmentPosition.getProcessedName(),
            employmentPosition.getSchemeName(),
            Objects.requireNonNullElse(
                employmentPosition.getSuperEmploymentPosition(),
                new EmploymentPositionHierarchy()
            ).getId()
        );
    }
}
