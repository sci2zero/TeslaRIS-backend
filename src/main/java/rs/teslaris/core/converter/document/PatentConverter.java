package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.model.document.Patent;

public class PatentConverter extends DocumentPublicationConverter {

    public static PatentDTO toDTO(Patent patent) {
        var patentDTO = new PatentDTO();

        setCommonFields(patent, patentDTO);

        patentDTO.setNumber(patent.getNumber());
        if (Objects.nonNull(patent.getPublisher())) {
            patentDTO.setPublisherId(patent.getPublisher().getId());
        }

        return patentDTO;
    }

    public static BibTeXEntry toBibTexEntry(Patent patent, String defaultLanguageTag) {
        var entry = new BibTeXEntry(new Key("patent"),
            new Key("(TESLARIS)" + patent.getId().toString()));

        setCommonFields(patent, entry, defaultLanguageTag);

        if (valueExists(patent.getNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(patent.getNumber(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(patent.getPublisher())) {
            setMCBibTexField(patent.getPublisher().getName(), entry, BibTeXEntry.KEY_PUBLISHER,
                defaultLanguageTag);
        }

        return entry;
    }

    public static String toTaggedFormat(Patent patent, String defaultLanguageTag, boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append("PAT").append("\n");

        setCommonTaggedFields(patent, sb, defaultLanguageTag, refMan);

        if (valueExists(patent.getNumber())) {
            sb.append(refMan ? "C6  - " : "%N ").append(patent.getNumber()).append("\n");
        }

        if (Objects.nonNull(patent.getPublisher())) {
            setMCTaggedField(patent.getPublisher().getName(), sb, refMan ? "PB" : "%I",
                defaultLanguageTag);
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
