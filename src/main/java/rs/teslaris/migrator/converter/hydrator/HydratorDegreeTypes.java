package rs.teslaris.migrator.converter.hydrator;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.person.DegreeType;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;

/**
 * CIÊNCIA VITAE degree-type codes. The crosswalk defines no vocabulary for them, so the table is
 * our own (thesis-mapiranje.md, section 7).
 */
public final class HydratorDegreeTypes {

    private static final Map<String, DegreeType> DEGREE_TYPES = Map.ofEntries(
        Map.entry("5", DegreeType.PHD),
        Map.entry("T", DegreeType.PHD),
        Map.entry("E31", DegreeType.PHD),
        Map.entry("E32", DegreeType.PHD),
        Map.entry("E33", DegreeType.PHD),
        Map.entry("E34", DegreeType.PHD),
        Map.entry("E35", DegreeType.PHD),
        Map.entry("E36", DegreeType.PHD),
        Map.entry("E39", DegreeType.PHD),
        Map.entry("E40", DegreeType.PHD),
        Map.entry("E41", DegreeType.PHD),
        Map.entry("3", DegreeType.MASTER),
        Map.entry("8", DegreeType.MASTER),
        Map.entry("S", DegreeType.MASTER),
        Map.entry("E04", DegreeType.MASTER),
        Map.entry("E08", DegreeType.MASTER),
        Map.entry("E17", DegreeType.MASTER),
        Map.entry("E18", DegreeType.MASTER),
        Map.entry("E19", DegreeType.MASTER),
        Map.entry("E20", DegreeType.MASTER),
        Map.entry("E21", DegreeType.MASTER),
        Map.entry("E22", DegreeType.MASTER),
        Map.entry("E23", DegreeType.MASTER),
        Map.entry("E25", DegreeType.MASTER),
        Map.entry("E27", DegreeType.MASTER),
        Map.entry("E28", DegreeType.MASTER),
        Map.entry("E30", DegreeType.MASTER),
        Map.entry("1", DegreeType.BACHELOR),
        Map.entry("2", DegreeType.BACHELOR),
        Map.entry("Q", DegreeType.BACHELOR),
        Map.entry("E01", DegreeType.BACHELOR),
        Map.entry("E02", DegreeType.BACHELOR),
        Map.entry("E05", DegreeType.BACHELOR),
        Map.entry("E07", DegreeType.BACHELOR),
        Map.entry("E09", DegreeType.BACHELOR),
        Map.entry("E10", DegreeType.BACHELOR),
        Map.entry("E13", DegreeType.BACHELOR),
        Map.entry("E15", DegreeType.BACHELOR)
    );

    // Coursework of a degree programme only - the degree itself, and its thesis, were not obtained
    private static final Set<String> COURSEWORK_ONLY = Set.of("T", "S");

    private static final Map<DegreeType, ThesisType> THESIS_TYPES = Map.of(
        DegreeType.PHD, ThesisType.PHD,
        DegreeType.MASTER, ThesisType.MASTER,
        DegreeType.BACHELOR, ThesisType.BACHELOR
    );

    // Degree-type codes of supervisions and examinations; Other, Scientific initiation,
    // Specialization and Aggregation have no thesis
    private static final Map<String, ThesisType> SERVICE_THESIS_TYPES = Map.of(
        "LB", ThesisType.BACHELOR,
        "M", ThesisType.MASTER,
        "D", ThesisType.PHD
    );


    private HydratorDegreeTypes() {
    }

    public static DegreeType degreeType(HydratorCVModel.DegreeType degreeType) {
        return DEGREE_TYPES.getOrDefault(code(degreeType), DegreeType.INFORMAL_EDUCATION);
    }

    /**
     * The thesis type of a degree that can have one, otherwise {@code null}.
     */
    public static ThesisType thesisType(HydratorCVModel.DegreeType degreeType) {
        if (COURSEWORK_ONLY.contains(code(degreeType))) {
            return null;
        }

        return THESIS_TYPES.get(degreeType(degreeType));
    }

    /**
     * The thesis type of a supervised or examined degree that can have one, otherwise
     * {@code null}.
     */
    public static ThesisType serviceThesisType(HydratorCVModel.DegreeType degreeType) {
        return SERVICE_THESIS_TYPES.get(code(degreeType));
    }

    private static String code(HydratorCVModel.DegreeType degreeType) {
        return Objects.isNull(degreeType) || Objects.isNull(degreeType.code()) ? "" :
            degreeType.code().trim();
    }
}
