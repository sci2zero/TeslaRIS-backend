package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.model.document.Software;

public class SoftwareConverter extends DocumentPublicationConverter {

    public static SoftwareDTO toDTO(Software software) {
        var softwareDTO = new SoftwareDTO();

        setCommonFields(software, softwareDTO);

        softwareDTO.setInternalNumber(software.getInternalNumber());
        if (Objects.nonNull(software.getPublisher())) {
            softwareDTO.setPublisherId(software.getPublisher().getId());
        }

        return softwareDTO;
    }

    public static BibTeXEntry toBibTexEntry(Software software) {
        var entry = new BibTeXEntry(new Key("software"),
            new Key("(TESLARIS)" + software.getId().toString()));

        setCommonFields(software, entry);

        if (valueExists(software.getInternalNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(software.getInternalNumber(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(software.getPublisher())) {
            setMCBibTexField(software.getPublisher().getName(), entry, BibTeXEntry.KEY_PUBLISHER);
        }

        return entry;
    }

    public static String toTaggedFormat(Software software, boolean refMan) {
        var sb = new StringBuilder();
        sb.append("TY  - ").append("GEN").append("\n");

        setCommonTaggedFields(software, sb, refMan);

        if (valueExists(software.getInternalNumber())) {
            sb.append(refMan ? "C6  - " : "%N ").append(software.getInternalNumber()).append("\n");
        }

        if (Objects.nonNull(software.getPublisher())) {
            setMCTaggedField(software.getPublisher().getName(), sb, refMan ? "PB" : "%I");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
