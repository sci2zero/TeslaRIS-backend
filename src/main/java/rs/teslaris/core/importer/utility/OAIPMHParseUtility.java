package rs.teslaris.core.importer.utility;

import java.util.List;
import rs.teslaris.core.dto.person.BasicPersonDTO;

public class OAIPMHParseUtility {

    public static Integer parseBISISID(String id) {
        var tokens = id.split("\\)");
        return Integer.parseInt(tokens[1]);
    }

    public static ResumptionTokenData parseResumptionToken(String resumptionToken)
        throws IllegalArgumentException {
        var tokens = resumptionToken.split("!");

        if (tokens.length != 5) {
            throw new IllegalArgumentException("Resumption token is invalid");
        }

        return new ResumptionTokenData(tokens[0], tokens[1], tokens[2], Integer.parseInt(tokens[3]),
            tokens[4]);
    }

    public static void parseElectronicAddresses(List<String> electronicAddresses,
                                                BasicPersonDTO dto) {
        electronicAddresses.forEach((electronicAddress) -> {
            var tokens = electronicAddress.split(":");
            switch (tokens[0]) {
                case "mailto":
                    dto.setContactEmail(tokens[1]);
                    break;
                case "tel":
                    // TODO: SUPPORT MULTIPLE PHONE NUMBERS
                    dto.setPhoneNumber(tokens[1]);
                    break;
            }
        });
    }

    public record ResumptionTokenData(String from, String until, String set, int page,
                                      String format) {
    }
}
