package rs.teslaris.migrator.converter.hydrator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.model.person.PrizeType;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Distinctions of one curriculum (MAP-040), each carrying the source key of the person it belongs
 * to. Country and awarding institutions have no target field in {@code Prize} and are not carried
 * over.
 */
@Component
@RequiredArgsConstructor
public class HydratorPrizeExtractor
    implements RecordExtractor<HydratorCVModel.Curriculum, PrizeMigrationDTO> {

    private static final Map<String, PrizeType> PRIZE_TYPES = Map.of(
        "P", PrizeType.AWARD,
        "T", PrizeType.TITLE,
        "O", PrizeType.OTHER
    );

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;


    @Override
    public List<PrizeMigrationDTO> extract(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().distinctions()) ||
            Objects.isNull(record.curriculum().distinctions().distinction())) {
            return List.of();
        }

        var language = record.curriculum().language();
        var result = new ArrayList<PrizeMigrationDTO>();

        record.curriculum().distinctions().distinction().forEach(distinction -> {
            if (Objects.isNull(distinction.id()) || distinction.id().isBlank()) {
                dropped(record.id(), "distinction without id");
                return;
            }

            var key = keyOf(record.id(), distinction.id());

            if (Objects.isNull(distinction.name()) || distinction.name().isBlank()) {
                dropped(key, "distinction without name");
                return;
            }

            var dto = new PrizeDTO();
            dto.setTitle(conversionUtil.multilingualContent(distinction.name(), language));
            dto.setDescription(
                conversionUtil.multilingualContent(distinction.description(), language));
            dto.setKeywords(
                conversionUtil.multilingualContent(keywords(distinction), language));
            String rejection = null;
            try {
                dto.setDate(date(distinction.effectiveDate()));
                dto.setEndDate(conversionUtil.localDate(distinction.endDate()));
            } catch (InvalidSourceValueException e) {
                rejection = e.getMessage();
            }

            dto.setPrizeType(prizeType(key, distinction.distinctionType()));
            dto.setFavorite(false);
            dto.setResearchAreasId(new HashSet<>(researchAreas(key, distinction)));

            result.add(new PrizeMigrationDTO(record.id(), distinction.id(), dto, rejection));
        });

        return result;
    }

    /**
     * Distinction ids are unique only within a curriculum, hence the composite key.
     */
    public String keyOf(HydratorCVModel.Curriculum record, PrizeMigrationDTO dto) {
        return keyOf(record.id(), dto.sourceId());
    }

    private String keyOf(String curriculumId, String distinctionId) {
        return curriculumId + "#distinction#" + distinctionId;
    }

    // One keyword per line, as for the person's keywords
    private String keywords(HydratorCVModel.Distinction distinction) {
        if (Objects.isNull(distinction.keywords()) ||
            Objects.isNull(distinction.keywords().keyword())) {
            return null;
        }

        var keywords = new LinkedHashSet<String>();
        distinction.keywords().keyword().stream()
            .filter(keyword -> Objects.nonNull(keyword) && !keyword.isBlank())
            .map(String::trim)
            .forEach(keywords::add);

        return keywords.isEmpty() ? null : String.join("\n", keywords);
    }

    private Set<Integer> researchAreas(String key,
                                                 HydratorCVModel.Distinction distinction) {
        if (Objects.isNull(distinction.researchClassifications()) ||
            Objects.isNull(distinction.researchClassifications().researchClassification())) {
            return Set.of();
        }

        return conversionUtil.researchAreaIds(
            distinction.researchClassifications().researchClassification(),
            reason -> dropped(key, reason));
    }

    // The source keeps only the year of award; a non-numeric year is rejected, not guessed
    private LocalDate date(String effectiveDate) {
        if (Objects.isNull(effectiveDate) || effectiveDate.isBlank()) {
            return null;
        }

        var year = conversionUtil.parseInteger(effectiveDate);
        if (Objects.isNull(year) || year < 1 || year > 9999) {
            throw new InvalidSourceValueException(
                "invalid effective date '" + effectiveDate + "'");
        }

        return LocalDate.of(year, 1, 1);
    }

    private PrizeType prizeType(String key, HydratorCVModel.DistinctionType type) {
        if (Objects.isNull(type) || Objects.isNull(type.code())) {
            return null;
        }

        var prizeType = PRIZE_TYPES.get(type.code().trim().toUpperCase());
        if (Objects.isNull(prizeType)) {
            dropped(key, "unknown distinction type '" + type.code() + "'");
        }

        return prizeType;
    }

    private void dropped(String key, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PERSON_PRIZE.name(),
            key, "MAP-040", reason);
    }
}
