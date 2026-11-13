package rs.teslaris.migrator.converter.hydrator;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import rs.teslaris.core.util.search.StringUtil;

/**
 * A curriculum funding reference classified for project lookup. Only {@code original} is stored;
 * {@code lookupValue} is what the project is searched by and is set only for strong references.
 */
public record HydratorFundingReference(
    String original,
    Kind kind,
    String lookupValue
) {

    // FCT project code, e.g. PTDC/ABC-XYZ/1234/2020, found anywhere in the reference. Broader than
    // FCT (any 2-8 letter prefix), to be narrowed once the full dump shows false matches
    private static final Pattern FCT_CODE = Pattern.compile(
        "(?<![A-Za-z0-9])[A-Za-z]{2,8}(?:/[A-Za-z]{2,}(?:-[A-Za-z]+)*)?/\\d{4,6}/\\d{4}(?![0-9])");

    // QREN operation code, e.g. POCI-01-0145-FEDER-000001, found anywhere in the reference
    private static final Pattern QREN = Pattern.compile(
        "(?<![A-Za-z0-9-])[A-Z0-9]+-\\d{2}-\\d{4}-[A-Z]+-\\d{6}(?![0-9])");

    private static final String EU_REPO_PREFIX = "info:eu-repo/";

    // info:eu-repo/grantAgreement/Funder/FundingProgram/ProjectID[/Jurisdiction/...]
    private static final int EU_REPO_FUNDER = 2;

    private static final int EU_REPO_PROJECT_ID = 4;


    public static HydratorFundingReference of(String reference) {
        if (!StringUtil.valueExists(reference)) {
            return new HydratorFundingReference(null, Kind.WEAK, null);
        }

        var original = reference.trim();

        if (original.toLowerCase().startsWith(EU_REPO_PREFIX)) {
            return ofEuRepo(original);
        }

        var fctCode = FCT_CODE.matcher(original);
        if (fctCode.find()) {
            return new HydratorFundingReference(original, Kind.FCT, fctCode.group());
        }

        var qren = QREN.matcher(original);
        if (qren.find()) {
            return new HydratorFundingReference(original, Kind.QREN, qren.group());
        }

        return new HydratorFundingReference(original, Kind.WEAK, null);
    }

    // Only an FCT URI whose ProjectID is a (URL-encoded) FCT code is strong; a numeric id is not
    private static HydratorFundingReference ofEuRepo(String original) {
        var segments = original.split("/");
        if (segments.length <= EU_REPO_PROJECT_ID ||
            !"FCT".equalsIgnoreCase(segments[EU_REPO_FUNDER].trim())) {
            return new HydratorFundingReference(original, Kind.WEAK, null);
        }

        String projectId;
        try {
            projectId =
                URLDecoder.decode(segments[EU_REPO_PROJECT_ID], StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException e) {
            return new HydratorFundingReference(original, Kind.WEAK, null);
        }

        return FCT_CODE.matcher(projectId).matches()
            ? new HydratorFundingReference(original, Kind.FCT, projectId)
            : new HydratorFundingReference(original, Kind.WEAK, null);
    }

    public boolean isStrong() {
        return kind != Kind.WEAK;
    }

    public enum Kind {
        FCT,
        QREN,
        WEAK
    }
}
