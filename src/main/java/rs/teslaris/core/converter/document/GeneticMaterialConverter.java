package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.model.document.GeneticMaterial;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;

public class GeneticMaterialConverter extends DocumentPublicationConverter {

    public static GeneticMaterialDTO toDTO(GeneticMaterial geneticMaterial) {
        var geneticMaterialDTO = new GeneticMaterialDTO();

        setCommonFields(geneticMaterial, geneticMaterialDTO);

        geneticMaterialDTO.setInternalNumber(geneticMaterial.getInternalNumber());
        geneticMaterialDTO.setGeneticMaterialType(geneticMaterial.getGeneticMaterialType());

        if (Objects.nonNull(geneticMaterial.getPublisher())) {
            geneticMaterialDTO.setPublisherId(geneticMaterial.getPublisher().getId());
        } else {
            geneticMaterialDTO.setAuthorReprint(geneticMaterial.getAuthorReprint());
        }

        return geneticMaterialDTO;
    }

    public static BibTeXEntry toBibTexEntry(GeneticMaterial geneticMaterial,
                                            String defaultLanguageTag) {
        var entry = new BibTeXEntry(BibTeXEntry.TYPE_MISC,
            new Key(IdentifierUtil.identifierPrefix + geneticMaterial.getId().toString()));

        setCommonFields(geneticMaterial, entry, defaultLanguageTag);

        if (StringUtil.valueExists(geneticMaterial.getInternalNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(geneticMaterial.getInternalNumber(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(geneticMaterial.getGeneticMaterialType())) {
            entry.addField(BibTeXEntry.KEY_TYPE,
                new StringValue(geneticMaterial.getGeneticMaterialType().name(),
                    StringValue.Style.BRACED));
        }

        if (Objects.nonNull(geneticMaterial.getPublisher())) {
            setMCBibTexField(geneticMaterial.getPublisher().getName(), entry,
                BibTeXEntry.KEY_PUBLISHER,
                defaultLanguageTag);
        } else if (Objects.nonNull(geneticMaterial.getAuthorReprint()) &&
            geneticMaterial.getAuthorReprint()) {
            entry.addField(BibTeXEntry.KEY_PUBLISHER,
                new StringValue(getAuthorReprintString(defaultLanguageTag),
                    StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(GeneticMaterial geneticMaterial,
                                        String defaultLanguageTag,
                                        boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? "GEN" : "Generic")
            .append("\n");

        setCommonTaggedFields(geneticMaterial, sb, defaultLanguageTag, refMan);

        if (StringUtil.valueExists(geneticMaterial.getInternalNumber())) {
            sb.append(refMan ? "C6  - " : "%N ").append(geneticMaterial.getInternalNumber())
                .append("\n");
        }

        if (Objects.nonNull(geneticMaterial.getGeneticMaterialType())) {
            sb.append(refMan ? "KW  - " : "%K ")
                .append("Type: ")
                .append(geneticMaterial.getGeneticMaterialType().name())
                .append("\n");
        }

        if (Objects.nonNull(geneticMaterial.getPublisher())) {
            setMCTaggedField(geneticMaterial.getPublisher().getName(), sb, refMan ? "PB" : "%I",
                defaultLanguageTag);
        } else if (Objects.nonNull(geneticMaterial.getAuthorReprint()) &&
            geneticMaterial.getAuthorReprint()) {
            sb.append(refMan ? "PB  - " : "%I ").append(getAuthorReprintString(defaultLanguageTag))
                .append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
