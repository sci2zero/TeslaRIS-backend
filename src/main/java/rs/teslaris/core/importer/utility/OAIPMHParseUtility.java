package rs.teslaris.core.importer.utility;

import java.util.List;
import rs.teslaris.core.dto.person.BasicPersonDTO;

public class OAIPMHParseUtility {

    public static Integer parseBISISID(String id) {
        var tokens = id.split("\\)");
        var aaa = Integer.parseInt(tokens[1]);
        return Integer.parseInt(tokens[1]);
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
}
