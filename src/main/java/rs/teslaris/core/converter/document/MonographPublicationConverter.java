package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.model.document.MonographPublication;

public class MonographPublicationConverter extends DocumentPublicationConverter {

    public static MonographPublicationDTO toDTO(MonographPublication monographPublication) {
        var response = new MonographPublicationDTO();

        setCommonFields(monographPublication, response);
        setMonographPublicationAffiliatedFields(monographPublication, response);

        return response;
    }

    private static void setMonographPublicationAffiliatedFields(
        MonographPublication monographPublication,
        MonographPublicationDTO monographPublicationDTO) {
        monographPublicationDTO.setMonographPublicationType(
            monographPublication.getMonographPublicationType());
        monographPublicationDTO.setStartPage(monographPublication.getStartPage());
        monographPublicationDTO.setEndPage(monographPublication.getEndPage());
        monographPublicationDTO.setNumberOfPages(monographPublication.getNumberOfPages());
        monographPublicationDTO.setArticleNumber(monographPublication.getArticleNumber());
        monographPublicationDTO.setMonographId(monographPublication.getMonograph().getId());
    }

    public static BibTeXEntry toBibTexEntry(MonographPublication monographPublication,
                                            String defaultLanguageTag) {
        var entry = new BibTeXEntry(BibTeXEntry.TYPE_INBOOK,
            new Key("(TESLARIS)" + monographPublication.getId().toString()));

        setCommonFields(monographPublication, entry, defaultLanguageTag);

        if (valueExists(monographPublication.getStartPage()) &&
            valueExists((monographPublication.getEndPage()))) {
            entry.addField(BibTeXEntry.KEY_PAGES,
                new StringValue(monographPublication.getStartPage() + "-" +
                    monographPublication.getEndPage(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(monographPublication.getNumberOfPages())) {
            entry.addField(new Key("pageNumber"),
                new StringValue(String.valueOf(monographPublication.getNumberOfPages()),
                    StringValue.Style.BRACED));
        }

        if (valueExists(monographPublication.getArticleNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(monographPublication.getArticleNumber(),
                    StringValue.Style.BRACED));
        }

        if (Objects.nonNull(monographPublication.getMonograph())) {
            setMCBibTexField(monographPublication.getMonograph().getTitle(), entry,
                BibTeXEntry.KEY_PUBLISHER, defaultLanguageTag);

            if (valueExists(monographPublication.getMonograph().getEISBN())) {
                entry.addField(new Key("eIsbn"),
                    new StringValue(monographPublication.getMonograph().getEISBN(),
                        StringValue.Style.BRACED));
            }

            if (valueExists(monographPublication.getMonograph().getPrintISBN())) {
                entry.addField(new Key("printIsbn"),
                    new StringValue(monographPublication.getMonograph().getPrintISBN(),
                        StringValue.Style.BRACED));
            }
        }

        return entry;
    }

    public static String toTaggedFormat(MonographPublication monographPublication,
                                        String defaultLanguageTag, boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? "CHAP" : "Book Section").append("\n");

        setCommonTaggedFields(monographPublication, sb, defaultLanguageTag, refMan);

        if (valueExists(monographPublication.getStartPage()) &&
            valueExists((monographPublication.getEndPage()))) {
            sb.append(refMan ? "SE  - " : "%P ").append(monographPublication.getStartPage())
                .append("-")
                .append(monographPublication.getEndPage())
                .append("\n");
        }

        if (Objects.nonNull(monographPublication.getNumberOfPages())) {
            sb.append(refMan ? "SP  - " : "%7 ").append(monographPublication.getNumberOfPages())
                .append("\n");
        }

        if (valueExists(monographPublication.getArticleNumber())) {
            sb.append(refMan ? "RI  - " : "%N ").append(monographPublication.getArticleNumber())
                .append("\n");
        }

        if (Objects.nonNull(monographPublication.getMonograph())) {
            setMCTaggedField(monographPublication.getMonograph().getTitle(), sb,
                refMan ? "T2" : "%0T",
                defaultLanguageTag);

            if (valueExists(monographPublication.getMonograph().getEISBN())) {
                sb.append(refMan ? "SN  - " : "%@ ").append("e:")
                    .append(monographPublication.getMonograph().getEISBN()).append("\n");
            }

            if (valueExists(monographPublication.getMonograph().getPrintISBN())) {
                sb.append(refMan ? "SN  - " : "%@ ").append("print:")
                    .append(monographPublication.getMonograph().getPrintISBN())
                    .append("\n");
            }
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}