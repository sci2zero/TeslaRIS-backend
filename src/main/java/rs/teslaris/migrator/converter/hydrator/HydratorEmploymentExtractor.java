package rs.teslaris.migrator.converter.hydrator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;

/**
 * Employments of one curriculum, each carrying the source keys of the person it belongs to and the
 * institution it points at.
 */
@Component
@RequiredArgsConstructor
public class HydratorEmploymentExtractor
    implements RecordExtractor<HydratorCVModel.Curriculum, EmploymentMigrationDTO> {

    private final HydratorConversionUtil conversionUtil;


    @Override
    public List<EmploymentMigrationDTO> extract(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().employments()) ||
            Objects.isNull(record.curriculum().employments().employment())) {
            return List.of();
        }

        var language = record.curriculum().language();
        var result = new ArrayList<EmploymentMigrationDTO>();

        record.curriculum().employments().employment().forEach(employment -> {
            var institution = primaryInstitution(employment);

            if (Objects.isNull(institution)) {
                return;
            }

            var dto = new EmploymentDTO();
            dto.setInvolvementType(InvolvementType.EMPLOYED_AT);
            dto.setDateFrom(toLocalDate(employment.startDate()));
            dto.setDateTo(toLocalDate(employment.endDate()));
            dto.setDisplayOrganisationUnit(
                conversionUtil.multilingualContent(institution.name(), language));

            if (Objects.nonNull(employment.positionTitle())) {
                dto.setRole(conversionUtil.multilingualContent(
                    employment.positionTitle().title(), language));
            }

            result.add(new EmploymentMigrationDTO(record.id(),
                conversionUtil.institutionKey(institution), dto));
        });

        return result;
    }

    /**
     * Employment ids are unique only within a curriculum, hence the composite key.
     */
    public String keyOf(HydratorCVModel.Curriculum record, EmploymentMigrationDTO dto) {
        return record.id() + "#employment#" + dto.institutionSourceKey() + "#" +
            Objects.toString(dto.employment().getDateFrom(), "no-start");
    }

    private HydratorCVModel.Institution primaryInstitution(HydratorCVModel.Employment employment) {
        if (Objects.isNull(employment.institution()) || employment.institution().isEmpty()) {
            return null;
        }

        return employment.institution().getFirst();
    }

    private LocalDate toLocalDate(HydratorCVModel.DateInfo dateInfo) {
        if (Objects.isNull(dateInfo)) {
            return null;
        }

        var year = conversionUtil.parseInteger(dateInfo.year());

        if (Objects.isNull(year)) {
            return null;
        }

        var month = Objects.requireNonNullElse(conversionUtil.parseInteger(dateInfo.month()), 1);
        var day = Objects.requireNonNullElse(conversionUtil.parseInteger(dateInfo.day()), 1);

        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return LocalDate.of(year, 1, 1);
        }
    }
}
