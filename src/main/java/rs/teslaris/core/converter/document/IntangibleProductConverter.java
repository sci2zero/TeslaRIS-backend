package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.model.document.IntangibleProduct;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;

public class IntangibleProductConverter extends DocumentPublicationConverter {

    public static IntangibleProductDTO toDTO(IntangibleProduct intangibleProduct) {
        var intangibleProductDTO = new IntangibleProductDTO();

        setCommonFields(intangibleProduct, intangibleProductDTO);

        intangibleProductDTO.setInternalNumber(intangibleProduct.getInternalNumber());
        intangibleProductDTO.setIntangibleProductType(intangibleProduct.getIntangibleProductType());

        intangibleProductDTO.setProductUsers(MultilingualContentConverter.getMultilingualContentDTO(
            intangibleProduct.getProductUsers()));

        intangibleProduct.getResearchAreas().forEach(researchArea -> {
            intangibleProductDTO.getResearchAreasId().add(researchArea.getId());
            intangibleProductDTO.getResearchAreas().add(ResearchAreaConverter.toDTO(researchArea));
        });

        if (Objects.nonNull(intangibleProduct.getPublisher())) {
            intangibleProductDTO.setPublisherId(intangibleProduct.getPublisher().getId());
        } else {
            intangibleProductDTO.setAuthorReprint(intangibleProduct.getAuthorReprint());
        }

        return intangibleProductDTO;
    }

    public static BibTeXEntry toBibTexEntry(IntangibleProduct intangibleProduct,
                                            String defaultLanguageTag) {
        var entry = new BibTeXEntry(new Key("software"),
            new Key(IdentifierUtil.identifierPrefix + intangibleProduct.getId().toString()));

        setCommonFields(intangibleProduct, entry, defaultLanguageTag);

        if (StringUtil.valueExists(intangibleProduct.getInternalNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(intangibleProduct.getInternalNumber(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(intangibleProduct.getPublisher())) {
            setMCBibTexField(intangibleProduct.getPublisher().getName(), entry,
                BibTeXEntry.KEY_PUBLISHER,
                defaultLanguageTag);
        } else if (Objects.nonNull(intangibleProduct.getAuthorReprint()) &&
            intangibleProduct.getAuthorReprint()) {
            entry.addField(BibTeXEntry.KEY_PUBLISHER,
                new StringValue(getAuthorReprintString(defaultLanguageTag),
                    StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(IntangibleProduct intangibleProduct,
                                        String defaultLanguageTag,
                                        boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? "GEN" : "Computer Program")
            .append("\n");

        setCommonTaggedFields(intangibleProduct, sb, defaultLanguageTag, refMan);

        if (StringUtil.valueExists(intangibleProduct.getInternalNumber())) {
            sb.append(refMan ? "C6  - " : "%N ").append(intangibleProduct.getInternalNumber())
                .append("\n");
        }

        if (Objects.nonNull(intangibleProduct.getPublisher())) {
            setMCTaggedField(intangibleProduct.getPublisher().getName(), sb, refMan ? "PB" : "%I",
                defaultLanguageTag);
        } else if (Objects.nonNull(intangibleProduct.getAuthorReprint()) &&
            intangibleProduct.getAuthorReprint()) {
            sb.append(refMan ? "PB  - " : "%I ").append(getAuthorReprintString(defaultLanguageTag))
                .append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
