package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.model.person.EducationStatus;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Education of one degree (CW-001-MAP-000038). The crosswalk maps the degree to
 * {@code Person.involvements} only, so the field mapping is our own (thesis-mapiranje.md, section
 * 7).
 */
@Component
@RequiredArgsConstructor
public class HydratorEducationExtractor {

    private static final Map<String, EducationStatus> EDUCATION_STATUSES = Map.of(
        "concluded", EducationStatus.CONCLUDED,
        "ongoing", EducationStatus.ONGOING,
        "attended", EducationStatus.ATTENDED
    );

    private final HydratorConversionUtil conversionUtil;

    private final HydratorOutputConverters outputConverters;

    private final MigrationLog migrationLog;


    public EducationMigrationDTO toEducation(HydratorCVModel.Curriculum record,
                                             HydratorCVModel.Degree degree) {
        var key = outputConverters.degreeKey(record, degree);
        var language = record.curriculum().language();
        var institution = primaryInstitution(degree);

        var dto = new EducationDTO();
        dto.setInvolvementType(InvolvementType.STUDIED_AT);
        dto.setTitle(conversionUtil.multilingualContent(degree.degreeName(), language));
        dto.setDegreeType(HydratorDegreeTypes.degreeType(degree.degreeType()));
        dto.setEducationStatus(educationStatus(key, degree));
        dto.setDegreeClassification(
            conversionUtil.multilingualContent(degree.classification(), language));
        dto.setDescription(conversionUtil.multilingualContent(description(degree), language));
        dto.setDisplaySupervisors(
            conversionUtil.multilingualContent(supervisorNames(degree), language));

        if (Objects.nonNull(institution)) {
            dto.setDisplayOrganisationUnit(
                conversionUtil.multilingualContent(institution.name(), language));
        }

        if (Objects.nonNull(degree.researchClassifications()) &&
            Objects.nonNull(degree.researchClassifications().researchClassification())) {
            dto.setResearchAreasId(conversionUtil.researchAreaIds(
                degree.researchClassifications().researchClassification(),
                reason -> dropped(key, reason)));
        }

        String rejection = null;
        try {
            dto.setDateFrom(conversionUtil.localDate(degree.startDate()));
            dto.setDateTo(conversionUtil.localDate(degree.endDate()));
        } catch (InvalidSourceValueException e) {
            rejection = e.getMessage();
        }

        return new EducationMigrationDTO(record.id(), key, institution, dto,
            thesisSourceKeys(record, degree, key), rejection);
    }

    // The dissertation output with the same title wins over a thesis of the degree's own
    private List<String> thesisSourceKeys(HydratorCVModel.Curriculum record,
                                          HydratorCVModel.Degree degree, String key) {
        var dissertationKey = outputConverters.dissertationKey(record, degree);

        if (Objects.nonNull(dissertationKey)) {
            return List.of(dissertationKey);
        }

        var degreeThesisKey = outputConverters.degreeThesisKey(record, degree);

        if (Objects.nonNull(degreeThesisKey)) {
            return List.of(degreeThesisKey);
        }

        if (outputConverters.hasThesisTitle(degree)) {
            dropped(key, "thesis not created (" + (outputConverters.isConcluded(degree) ?
                "degree type '" + degreeTypeValue(degree) + "' has no thesis" :
                "degree not concluded") + "): '" + degree.thesis().title().trim() + "'");
        }

        return List.of();
    }

    private EducationStatus educationStatus(String key, HydratorCVModel.Degree degree) {
        var status = Objects.isNull(degree.degreeStatus()) ? "" :
            Objects.toString(degree.degreeStatus().status(), "").trim();
        var educationStatus = EDUCATION_STATUSES.get(status.toLowerCase());

        if (Objects.isNull(educationStatus) && !status.isEmpty()) {
            dropped(key, "unknown degree status '" + status + "'");
        }

        return educationStatus;
    }

    private String description(HydratorCVModel.Degree degree) {
        var parts = Stream.of(degree.degreeMajor(), degree.description())
            .filter(value -> !isBlank(value))
            .map(String::trim)
            .toList();

        return parts.isEmpty() ? null : String.join("\n\n", parts);
    }

    private String supervisorNames(HydratorCVModel.Degree degree) {
        if (Objects.isNull(degree.thesis()) || Objects.isNull(degree.thesis().supervisors()) ||
            Objects.isNull(degree.thesis().supervisors().supervisor())) {
            return null;
        }

        var names = new ArrayList<String>();
        degree.thesis().supervisors().supervisor().stream()
            .filter(supervisor -> Objects.nonNull(supervisor) && !isBlank(supervisor.name()) &&
                !conversionUtil.isNoValue(supervisor.name()))
            .map(supervisor -> supervisor.name().trim())
            .forEach(names::add);

        return names.isEmpty() ? null : String.join(", ", names);
    }

    private HydratorCVModel.Institution primaryInstitution(HydratorCVModel.Degree degree) {
        if (Objects.isNull(degree.institutions()) ||
            Objects.isNull(degree.institutions().institution())) {
            return null;
        }

        return degree.institutions().institution().stream()
            .filter(institution -> Objects.nonNull(institution) && !isBlank(institution.name()))
            .findFirst()
            .orElse(null);
    }

    private String degreeTypeValue(HydratorCVModel.Degree degree) {
        return Objects.isNull(degree.degreeType()) ? "" :
            Objects.toString(degree.degreeType().value(), "");
    }

    private void dropped(String key, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME,
            MigrationEntityType.PERSON_EDUCATION.name(), key, HydratorMappingRule.cv(38), reason);
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
