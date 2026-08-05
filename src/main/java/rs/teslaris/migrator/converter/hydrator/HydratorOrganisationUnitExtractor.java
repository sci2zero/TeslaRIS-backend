package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;

/**
 * Every distinct institution referenced by the curriculum's employments becomes an organisation
 * unit. Duplicates within one curriculum are collapsed here; duplicates across curricula are
 * collapsed by the record log.
 */
@Component
@RequiredArgsConstructor
public class HydratorOrganisationUnitExtractor
    implements RecordExtractor<HydratorCVModel.Curriculum, OrganisationUnitRequestDTO> {

    private final HydratorConversionUtil conversionUtil;


    @Override
    public List<OrganisationUnitRequestDTO> extract(HydratorCVModel.Curriculum record) {
        var employments = employmentsOf(record);

        if (employments.isEmpty()) {
            return List.of();
        }

        var byKey = new LinkedHashMap<String, OrganisationUnitRequestDTO>();

        employments.forEach(employment -> {
            if (Objects.isNull(employment.institution())) {
                return;
            }

            employment.institution().forEach(institution -> {
                var key = conversionUtil.institutionKey(institution);

                if (Objects.nonNull(key) && !byKey.containsKey(key)) {
                    byKey.put(key, toDTO(institution, record));
                }
            });
        });

        return new ArrayList<>(byKey.values());
    }

    public String keyOf(OrganisationUnitRequestDTO dto) {
        if (dto.getName().isEmpty()) {
            return null;
        }

        return conversionUtil.normalise(dto.getName().getFirst().getContent());
    }

    private OrganisationUnitRequestDTO toDTO(HydratorCVModel.Institution institution,
                                             HydratorCVModel.Curriculum record) {
        var language = Objects.isNull(record.curriculum()) ? null : record.curriculum().language();

        var dto = new OrganisationUnitRequestDTO();
        dto.setName(conversionUtil.multilingualContent(institution.name(), language));
        dto.setNameAbbreviation(List.of());
        dto.setDescription(List.of());
        dto.setKeyword(List.of());
        dto.setResearchAreasId(List.of());

        if (Objects.nonNull(institution.url()) && !institution.url().isBlank()) {
            dto.setUris(new java.util.HashSet<>(List.of(institution.url().trim())));
        }

        return dto;
    }

    private List<HydratorCVModel.Employment> employmentsOf(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().employments()) ||
            Objects.isNull(record.curriculum().employments().employment())) {
            return List.of();
        }

        return record.curriculum().employments().employment();
    }
}
