package rs.teslaris.project.util;

import java.util.Objects;

/**
 * EU/Horizon DOIs are minted by the European Commission under a single prefix, and the part after
 * it is the CORDIS project id (which doubles as the EC grant agreement number).
 */
public class CordisDoiUtil {

    public static final String EU_HORIZON_DOI_PREFIX = "10.3030/";

    private CordisDoiUtil() {
    }

    public static boolean isEuHorizonDoi(String doi) {
        return Objects.nonNull(doi) && doi.startsWith(EU_HORIZON_DOI_PREFIX);
    }

    public static String extractCordisProjectId(String doi) {
        return doi.substring(EU_HORIZON_DOI_PREFIX.length());
    }
}
