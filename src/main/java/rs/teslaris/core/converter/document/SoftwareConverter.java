package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;

public class SoftwareConverter extends DocumentPublicationConverter {

    public static SoftwareDTO toDTO(Software software) {
        var softwareDTO = new SoftwareDTO();

        setCommonFields(software, softwareDTO);

        softwareDTO.setInternalNumber(software.getInternalNumber());
        if (Objects.nonNull(software.getPublisher())) {
            softwareDTO.setPublisherId(software.getPublisher().getId());
        } else {
            softwareDTO.setAuthorReprint(software.getAuthorReprint());
        }

        return softwareDTO;
    }

    public static BibTeXEntry toBibTexEntry(Software software, String defaultLanguageTag) {
        var entry = new BibTeXEntry(new Key("software"),
            new Key(IdentifierUtil.identifierPrefix + software.getId().toString()));

        setCommonFields(software, entry, defaultLanguageTag);

        if (StringUtil.valueExists(software.getInternalNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(software.getInternalNumber(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(software.getPublisher())) {
            setMCBibTexField(software.getPublisher().getName(), entry, BibTeXEntry.KEY_PUBLISHER,
                defaultLanguageTag);
        } else if (Objects.nonNull(software.getAuthorReprint()) && software.getAuthorReprint()) {
            entry.addField(BibTeXEntry.KEY_PUBLISHER,
                new StringValue(getAuthorReprintString(defaultLanguageTag),
                    StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(Software software, String defaultLanguageTag,
                                        boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? "GEN" : "Computer Program")
            .append("\n");

        setCommonTaggedFields(software, sb, defaultLanguageTag, refMan);

        if (StringUtil.valueExists(software.getInternalNumber())) {
            sb.append(refMan ? "C6  - " : "%N ").append(software.getInternalNumber()).append("\n");
        }

        if (Objects.nonNull(software.getPublisher())) {
            setMCTaggedField(software.getPublisher().getName(), sb, refMan ? "PB" : "%I",
                defaultLanguageTag);
        } else if (Objects.nonNull(software.getAuthorReprint()) && software.getAuthorReprint()) {
            sb.append(refMan ? "PB  - " : "%I ").append(getAuthorReprintString(defaultLanguageTag))
                .append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
