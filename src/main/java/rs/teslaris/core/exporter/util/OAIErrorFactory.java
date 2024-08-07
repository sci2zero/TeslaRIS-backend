package rs.teslaris.core.exporter.util;

import rs.teslaris.core.importer.model.oaipmh.common.OAIError;

public class OAIErrorFactory {

    public static OAIError constructBadArgumentError() {
        return new OAIError("badArgument",
            "The request includes illegal arguments, is missing required arguments, includes a repeated argument, or values for arguments have an illegal syntax.");
    }

    public static OAIError constructNotFoundOrForbiddenError(String identifier) {
        return new OAIError("idDoesNotExist",
            "\"" + identifier + "\" is unknown or illegal in this repository");
    }

    public static OAIError constructFormatError(String format) {
        return new OAIError("cannotDisseminateFormat",
            "\"" + format + "\" is not supported by the item or by the repository");
    }

    public static OAIError constructNoRecordsMatchError() {
        return new OAIError("noRecordsMatch",
            "The combination of the values of the from, until, set, and metadataPrefix arguments results in an empty list.");
    }
}
