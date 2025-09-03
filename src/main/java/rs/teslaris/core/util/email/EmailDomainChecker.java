package rs.teslaris.core.util.email;

import java.net.IDN;
import java.util.Objects;
import java.util.regex.Pattern;

public final class EmailDomainChecker {

    // local-part allowed chars (unquoted). Disallow quoted local-parts for safety in this checker.
    // This pattern allows A-Z a-z 0-9 and the allowed special chars, with dots not at ends and not consecutive.
    private static final Pattern LOCAL_PART = Pattern.compile(
        "^(?!\\.)[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]{1,64}(?<!\\.)$"
    );

    // domain label: start/end with alnum, may contain hyphens inside. Each label 1..63.
    private static final Pattern DOMAIN_LABEL =
        Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$");

    // basic ASCII control chars detection (includes CR/LF and NUL)
    private static final Pattern CTL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");


    private EmailDomainChecker() {
    }

    public static boolean isEmailFromInstitution(String email, String institutionDomain,
                                                 boolean allowSubdomains) {
        if (Objects.isNull(email) || Objects.isNull(institutionDomain)) {
            return false;
        }

        if (CTL_CHARS.matcher(email).find()) {
            return false;
        }
        if (email.contains(",") || email.contains(";") || email.contains("<") ||
            email.contains(">")) {
            return false;
        }

        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        int at = trimmed.indexOf('@');
        if (at <= 0 || at != trimmed.lastIndexOf('@') || at == trimmed.length() - 1) {
            return false;
        }

        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at + 1);

        if (local.startsWith("\"") || local.endsWith("\"") || local.contains(" ")) {
            return false;
        }

        if (!LOCAL_PART.matcher(local).matches()) {
            return false;
        }

        final String asciiDomain;
        try {
            asciiDomain = IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES).toLowerCase();
        } catch (Exception e) {
            return false; // invalid IDN or illegal characters
        }

        if (asciiDomain.isEmpty() || asciiDomain.length() > 255) {
            return false;
        }

        String[] labels = asciiDomain.split("\\.");
        for (String lbl : labels) {
            if (lbl.isEmpty() || lbl.length() > 63) {
                return false;
            }
            if (!DOMAIN_LABEL.matcher(lbl).matches()) {
                return false;
            }
        }

        final String asciiInstitution;
        try {
            asciiInstitution =
                IDN.toASCII(institutionDomain, IDN.USE_STD3_ASCII_RULES).toLowerCase();
        } catch (Exception e) {
            return false;
        }

        if (allowSubdomains) {
            return asciiDomain.equals(asciiInstitution) ||
                asciiDomain.endsWith("." + asciiInstitution);
        } else {
            return asciiDomain.equals(asciiInstitution);
        }
    }
}
