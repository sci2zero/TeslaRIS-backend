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
}
