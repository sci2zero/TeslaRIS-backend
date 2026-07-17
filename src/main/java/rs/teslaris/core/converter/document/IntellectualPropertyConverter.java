package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.converter.commontypes.FlexibleDateConverter;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;
import rs.teslaris.core.model.document.IntellectualProperty;
import rs.teslaris.core.model.document.IntellectualPropertyType;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;

public class IntellectualPropertyConverter extends DocumentPublicationConverter {

    public static IntellectualPropertyDTO toDTO(IntellectualProperty intellectualProperty) {
        var intellectualPropertyDTO = new IntellectualPropertyDTO();

        setCommonFields(intellectualProperty, intellectualPropertyDTO);

        intellectualPropertyDTO.setNumber(intellectualProperty.getNumber());
        if (Objects.nonNull(intellectualProperty.getPublisher())) {
            intellectualPropertyDTO.setPublisherId(intellectualProperty.getPublisher().getId());
            intellectualPropertyDTO.setPublisherName(
                MultilingualContentConverter.getMultilingualContentDTO(
                    intellectualProperty.getPublisher().getName()));
        } else {
            intellectualPropertyDTO.setAuthorReprint(intellectualProperty.getAuthorReprint());
        }

        intellectualPropertyDTO.setType(intellectualProperty.getType());
        intellectualPropertyDTO.setApplicationStatus(intellectualProperty.getApplicationStatus());
        intellectualPropertyDTO.setDateRequested(
            FlexibleDateConverter.toDTO(intellectualProperty.getDateRequested()));
        intellectualPropertyDTO.setDateFilingPriority(
            FlexibleDateConverter.toDTO(intellectualProperty.getDateFilingPriority()));
        intellectualPropertyDTO.setDateTo(
            FlexibleDateConverter.toDTO(intellectualProperty.getDateTo()));

        return intellectualPropertyDTO;
    }

    public static BibTeXEntry toBibTexEntry(IntellectualProperty intellectualProperty,
                                            String defaultLanguageTag) {
        var entry = new BibTeXEntry(new Key("intellectualProperty"),
            new Key(IdentifierUtil.identifierPrefix + intellectualProperty.getId().toString()));

        setCommonFields(intellectualProperty, entry, defaultLanguageTag);

        if (StringUtil.valueExists(intellectualProperty.getNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(intellectualProperty.getNumber(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(intellectualProperty.getPublisher())) {
            setMCBibTexField(intellectualProperty.getPublisher().getName(), entry,
                BibTeXEntry.KEY_PUBLISHER,
                defaultLanguageTag);
        } else if (Objects.nonNull(intellectualProperty.getAuthorReprint()) &&
            intellectualProperty.getAuthorReprint()) {
            entry.addField(BibTeXEntry.KEY_PUBLISHER,
                new StringValue(getAuthorReprintString(defaultLanguageTag),
                    StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(IntellectualProperty intellectualProperty,
                                        String defaultLanguageTag, boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? (intellectualProperty.getType().equals(
            IntellectualPropertyType.PATENT) ? "PAT" : "GEN") :
            (intellectualProperty.getType().equals(
                IntellectualPropertyType.PATENT) ? "Patent" : "Generic")).append("\n");

        setCommonTaggedFields(intellectualProperty, sb, defaultLanguageTag, refMan);

        if (StringUtil.valueExists(intellectualProperty.getNumber())) {
            sb.append(refMan ? "C6  - " : "%N ").append(intellectualProperty.getNumber())
                .append("\n");
        }

        if (Objects.nonNull(intellectualProperty.getPublisher())) {
            setMCTaggedField(intellectualProperty.getPublisher().getName(), sb,
                refMan ? "PB" : "%I",
                defaultLanguageTag);
        } else if (Objects.nonNull(intellectualProperty.getAuthorReprint()) &&
            intellectualProperty.getAuthorReprint()) {
            sb.append(refMan ? "PB  - " : "%I ").append(getAuthorReprintString(defaultLanguageTag))
                .append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
