package rs.teslaris.migrator.converter.hydrator;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Crosswalk rule ids as the workbook writes them ({@code CW-00n-MAP-000nnn}). Rule numbers repeat
 * across crosswalks, so the crosswalk prefix is what makes a logged rule unambiguous.
 */
public final class HydratorMappingRule {

    private static final String CIENCIA_VITAE = "CW-001";

    private static final String SCIPROJ = "CW-002";


    private HydratorMappingRule() {
    }

    // CIÊNCIA VITAE curricula
    public static String cv(int... rules) {
        return format(CIENCIA_VITAE, rules);
    }

    // SciPROJ projects and fundings
    public static String sciproj(int... rules) {
        return format(SCIPROJ, rules);
    }

    // Several rules covering one value share the prefix: CW-002-MAP-000016/000035
    private static String format(String crosswalk, int... rules) {
        return crosswalk + "-MAP-" + Arrays.stream(rules)
            .mapToObj(rule -> String.format("%06d", rule))
            .collect(Collectors.joining("/"));
    }
}
