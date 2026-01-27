package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.model.document.MaterialProductType;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;

public class MaterialProductConverter extends DocumentPublicationConverter {

    public static MaterialProductDTO toDTO(MaterialProduct materialProduct) {
        var materialProductDTO = new MaterialProductDTO();

        setCommonFields(materialProduct, materialProductDTO);

        materialProductDTO.setInternalNumber(materialProduct.getInternalNumber());
        materialProductDTO.setNumberProduced(materialProduct.getNumberProduced());
        materialProductDTO.setMaterialProductType(materialProduct.getMaterialProductType());

        materialProductDTO.setProductUsers(MultilingualContentConverter.getMultilingualContentDTO(
            materialProduct.getProductUsers()));

        materialProduct.getResearchAreas().forEach(researchArea -> {
            materialProductDTO.getResearchAreasId().add(researchArea.getId());
            materialProductDTO.getResearchAreas().add(ResearchAreaConverter.toDTO(researchArea));
        });

        if (Objects.nonNull(materialProduct.getPublisher())) {
            materialProductDTO.setPublisherId(materialProduct.getPublisher().getId());
        } else {
            materialProductDTO.setAuthorReprint(materialProduct.getAuthorReprint());
        }

        return materialProductDTO;
    }

    public static BibTeXEntry toBibTexEntry(MaterialProduct materialProduct,
                                            String defaultLanguageTag) {
        var entry = new BibTeXEntry(materialProduct.getMaterialProductType().equals(
            MaterialProductType.INDUSTRIAL_PRODUCT) ? BibTeXEntry.TYPE_TECHREPORT :
            BibTeXEntry.TYPE_MANUAL,
            new Key(IdentifierUtil.identifierPrefix + materialProduct.getId().toString()));

        setCommonFields(materialProduct, entry, defaultLanguageTag);

        if (StringUtil.valueExists(materialProduct.getInternalNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(materialProduct.getInternalNumber(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(materialProduct.getMaterialProductType())) {
            entry.addField(BibTeXEntry.KEY_TYPE,
                new StringValue(materialProduct.getMaterialProductType().name(),
                    StringValue.Style.BRACED));
        }

        if (Objects.nonNull(materialProduct.getPublisher())) {
            setMCBibTexField(materialProduct.getPublisher().getName(), entry,
                BibTeXEntry.KEY_PUBLISHER,
                defaultLanguageTag);
        } else if (Objects.nonNull(materialProduct.getAuthorReprint()) &&
            materialProduct.getAuthorReprint()) {
            entry.addField(BibTeXEntry.KEY_PUBLISHER,
                new StringValue(getAuthorReprintString(defaultLanguageTag),
                    StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(MaterialProduct materialProduct,
                                        String defaultLanguageTag,
                                        boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? "GEN" : "Generic")
            .append("\n");

        setCommonTaggedFields(materialProduct, sb, defaultLanguageTag, refMan);

        if (StringUtil.valueExists(materialProduct.getInternalNumber())) {
            sb.append(refMan ? "C6  - " : "%N ").append(materialProduct.getInternalNumber())
                .append("\n");
        }

        if (Objects.nonNull(materialProduct.getMaterialProductType())) {
            sb.append(refMan ? "KW  - " : "%K ")
                .append("Type: ")
                .append(materialProduct.getMaterialProductType().name())
                .append("\n");
        }

        if (Objects.nonNull(materialProduct.getPublisher())) {
            setMCTaggedField(materialProduct.getPublisher().getName(), sb, refMan ? "PB" : "%I",
                defaultLanguageTag);
        } else if (Objects.nonNull(materialProduct.getAuthorReprint()) &&
            materialProduct.getAuthorReprint()) {
            sb.append(refMan ? "PB  - " : "%I ").append(getAuthorReprintString(defaultLanguageTag))
                .append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
