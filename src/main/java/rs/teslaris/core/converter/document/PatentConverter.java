package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;

public class PatentConverter extends DocumentPublicationConverter {

    public static PatentDTO toDTO(Patent patent) {
        var patentDTO = new PatentDTO();

        setCommonFields(patent, patentDTO);

        patentDTO.setNumber(patent.getNumber());
        if (Objects.nonNull(patent.getPublisher())) {
            patentDTO.setPublisherId(patent.getPublisher().getId());
        } else {
            patentDTO.setAuthorReprint(patent.getAuthorReprint());
        }

        return patentDTO;
    }

    public static BibTeXEntry toBibTexEntry(Patent patent, String defaultLanguageTag) {
        var entry = new BibTeXEntry(new Key("patent"),
            new Key(IdentifierUtil.identifierPrefix + patent.getId().toString()));

        setCommonFields(patent, entry, defaultLanguageTag);

        if (StringUtil.valueExists(patent.getNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(patent.getNumber(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(patent.getPublisher())) {
            setMCBibTexField(patent.getPublisher().getName(), entry, BibTeXEntry.KEY_PUBLISHER,
                defaultLanguageTag);
        } else if (Objects.nonNull(patent.getAuthorReprint()) && patent.getAuthorReprint()) {
            entry.addField(BibTeXEntry.KEY_PUBLISHER,
                new StringValue(getAuthorReprintString(defaultLanguageTag),
                    StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(Patent patent, String defaultLanguageTag, boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? "PAT" : "Patent").append("\n");

        setCommonTaggedFields(patent, sb, defaultLanguageTag, refMan);

        if (StringUtil.valueExists(patent.getNumber())) {
            sb.append(refMan ? "C6  - " : "%N ").append(patent.getNumber()).append("\n");
        }

        if (Objects.nonNull(patent.getPublisher())) {
            setMCTaggedField(patent.getPublisher().getName(), sb, refMan ? "PB" : "%I",
                defaultLanguageTag);
        } else if (Objects.nonNull(patent.getAuthorReprint()) && patent.getAuthorReprint()) {
            sb.append(refMan ? "PB  - " : "%I ").append(getAuthorReprintString(defaultLanguageTag))
                .append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
