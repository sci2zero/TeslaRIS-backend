package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.model.document.ProceedingsPublication;

public class ProceedingsPublicationConverter extends DocumentPublicationConverter {

    public static ProceedingsPublicationDTO toDTO(ProceedingsPublication publication) {
        var publicationDTO = new ProceedingsPublicationDTO();

        setCommonFields(publication, publicationDTO);
        setProceedingsAffiliatedFields(publication, publicationDTO);

        return publicationDTO;
    }

    private static void setProceedingsAffiliatedFields(ProceedingsPublication publication,
                                                       ProceedingsPublicationDTO publicationDTO) {
        publicationDTO.setProceedingsPublicationType(publication.getProceedingsPublicationType());
        publicationDTO.setStartPage(publication.getStartPage());
        publicationDTO.setEndPage(publication.getEndPage());
        publicationDTO.setNumberOfPages(publication.getNumberOfPages());
        publicationDTO.setArticleNumber(publication.getArticleNumber());
        publicationDTO.setProceedingsId(publication.getProceedings().getId());
        publicationDTO.setEventId(publication.getEvent().getId());
    }

    public static BibTeXEntry toBibTexEntry(ProceedingsPublication proceedingsPublication,
                                            String defaultLanguageTag) {
        var entry = new BibTeXEntry(BibTeXEntry.TYPE_INPROCEEDINGS,
            new Key("(TESLARIS)" + proceedingsPublication.getId().toString()));

        setCommonFields(proceedingsPublication, entry, defaultLanguageTag);

        if (valueExists(proceedingsPublication.getStartPage()) &&
            valueExists((proceedingsPublication.getEndPage()))) {
            entry.addField(BibTeXEntry.KEY_PAGES,
                new StringValue(proceedingsPublication.getStartPage() + "-" +
                    proceedingsPublication.getEndPage(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(proceedingsPublication.getNumberOfPages())) {
            entry.addField(new Key("pageNumber"),
                new StringValue(String.valueOf(proceedingsPublication.getNumberOfPages()),
                    StringValue.Style.BRACED));
        }

        if (valueExists(proceedingsPublication.getArticleNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(proceedingsPublication.getArticleNumber(),
                    StringValue.Style.BRACED));
        }

        if (Objects.nonNull(proceedingsPublication.getProceedings())) {
            setMCBibTexField(proceedingsPublication.getProceedings().getTitle(), entry,
                BibTeXEntry.KEY_BOOKTITLE, defaultLanguageTag);

            if (valueExists(proceedingsPublication.getProceedings().getEISBN())) {
                entry.addField(new Key("eIsbn"),
                    new StringValue(proceedingsPublication.getProceedings().getEISBN(),
                        StringValue.Style.BRACED));
            }

            if (valueExists(proceedingsPublication.getProceedings().getPrintISBN())) {
                entry.addField(new Key("printIsbn"),
                    new StringValue(proceedingsPublication.getProceedings().getPrintISBN(),
                        StringValue.Style.BRACED));
            }
        }

        return entry;
    }

    public static String toTaggedFormat(ProceedingsPublication proceedingsPublication,
                                        String defaultLanguageTag, boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0").append("CPAPER").append("\n");

        setCommonTaggedFields(proceedingsPublication, sb, defaultLanguageTag, refMan);

        if (valueExists(proceedingsPublication.getStartPage()) &&
            valueExists((proceedingsPublication.getEndPage()))) {
            sb.append(refMan ? "SE  - " : "%P ").append(proceedingsPublication.getStartPage())
                .append("-")
                .append(proceedingsPublication.getEndPage())
                .append("\n");
        }

        if (Objects.nonNull(proceedingsPublication.getNumberOfPages())) {
            sb.append(refMan ? "SP  - " : "%7").append(proceedingsPublication.getNumberOfPages())
                .append("\n");
        }

        if (valueExists(proceedingsPublication.getArticleNumber())) {
            sb.append(refMan ? "RI  - " : "%N ").append(proceedingsPublication.getArticleNumber())
                .append("\n");
        }

        if (Objects.nonNull(proceedingsPublication.getProceedings())) {
            setMCTaggedField(proceedingsPublication.getProceedings().getTitle(), sb,
                refMan ? "C3" : "%J", defaultLanguageTag);

            if (valueExists(proceedingsPublication.getProceedings().getEISBN())) {
                sb.append(refMan ? "SN  - " : "%@ ").append("e:")
                    .append(proceedingsPublication.getProceedings().getEISBN())
                    .append("\n");
            }

            if (valueExists(proceedingsPublication.getProceedings().getPrintISBN())) {
                sb.append(refMan ? "SN  - " : "%@ ").append("print:")
                    .append(proceedingsPublication.getProceedings().getPrintISBN())
                    .append("\n");
            }
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
