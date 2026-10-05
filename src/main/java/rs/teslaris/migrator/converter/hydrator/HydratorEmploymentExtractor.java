package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Employments of one curriculum (MAP-000039), each carrying the source key of the person it belongs
 * to and the institution it points at. The institution name is always set as the display
 * organisation unit; it is replaced by the organisation unit when the creator matches one.
 */
@Component
@RequiredArgsConstructor
public class HydratorEmploymentExtractor
    implements RecordExtractor<HydratorCVModel.Curriculum, EmploymentMigrationDTO> {

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;


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
            if (Objects.isNull(employment.id()) || employment.id().isBlank()) {
                migrationLog.valueDropped(HydratorSource.NAME,
                    MigrationEntityType.PERSON_EMPLOYMENT.name(), record.id(), "MAP-000039",
                    "employment without id");
                return;
            }

            var institution = primaryInstitution(employment);

            if (Objects.isNull(institution)) {
                migrationLog.valueDropped(HydratorSource.NAME,
                    MigrationEntityType.PERSON_EMPLOYMENT.name(),
                    record.id() + "#employment#" + employment.id(), "MAP-000039",
                    "employment without institution");
                return;
            }

            var dto = new EmploymentDTO();
            dto.setInvolvementType(InvolvementType.EMPLOYED_AT);
            String rejection = null;
            try {
                dto.setDateFrom(conversionUtil.localDate(employment.startDate()));
                dto.setDateTo(conversionUtil.localDate(employment.endDate()));
            } catch (InvalidSourceValueException e) {
                rejection = e.getMessage();
            }

            dto.setDisplayOrganisationUnit(
                conversionUtil.multilingualContent(institution.name(), language));

            if (Objects.nonNull(employment.positionTitle())) {
                dto.setRole(conversionUtil.multilingualContent(
                    employment.positionTitle().title(), language));
            }

            result.add(new EmploymentMigrationDTO(record.id(), employment.id(), institution, dto,
                rejection));
        });

        return result;
    }

    /**
     * Employment ids are unique only within a curriculum, hence the composite key.
     */
    public String keyOf(HydratorCVModel.Curriculum record, EmploymentMigrationDTO dto) {
        return record.id() + "#employment#" + dto.sourceId();
    }

    private HydratorCVModel.Institution primaryInstitution(HydratorCVModel.Employment employment) {
        if (Objects.isNull(employment.institution()) || employment.institution().isEmpty()) {
            return null;
        }

        return employment.institution().getFirst();
    }
}
