package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.util.search.StringUtil;

public class JournalPublicationConverter extends DocumentPublicationConverter {

    public static JournalPublicationResponseDTO toDTO(JournalPublication publication) {
        var publicationDTO = new JournalPublicationResponseDTO();

        setCommonFields(publication, publicationDTO);
        setJournalAffiliatedFields(publication, publicationDTO);

        return publicationDTO;
    }

    private static void setJournalAffiliatedFields(JournalPublication publication,
                                                   JournalPublicationResponseDTO publicationDTO) {
        publicationDTO.setJournalPublicationType(publication.getJournalPublicationType());
        publicationDTO.setStartPage(publication.getStartPage());
        publicationDTO.setEndPage(publication.getEndPage());
        publicationDTO.setNumberOfPages(publication.getNumberOfPages());
        publicationDTO.setArticleNumber(publication.getArticleNumber());
        publicationDTO.setVolume(publication.getVolume());
        publicationDTO.setIssue(publication.getIssue());
        publicationDTO.setJournalId(publication.getJournal().getId());
        publicationDTO.setJournalName(
            MultilingualContentConverter.getMultilingualContentDTO(
                publication.getJournal().getTitle()));
    }

    public static BibTeXEntry toBibTexEntry(JournalPublication journalPublication,
                                            String defaultLanguageTag) {
        var entry = new BibTeXEntry(BibTeXEntry.TYPE_ARTICLE,
            new Key("(TESLARIS)" + journalPublication.getId().toString()));

        setCommonFields(journalPublication, entry, defaultLanguageTag);

        if (StringUtil.valueExists(journalPublication.getStartPage()) &&
            StringUtil.valueExists((journalPublication.getEndPage()))) {
            entry.addField(BibTeXEntry.KEY_PAGES,
                new StringValue(journalPublication.getStartPage() + "-" +
                    journalPublication.getEndPage(), StringValue.Style.BRACED));
        }

        if (StringUtil.valueExists(journalPublication.getArticleNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(journalPublication.getArticleNumber(),
                    StringValue.Style.BRACED));
        }

        if (StringUtil.valueExists(journalPublication.getVolume())) {
            entry.addField(BibTeXEntry.KEY_VOLUME,
                new StringValue(journalPublication.getVolume(), StringValue.Style.BRACED));
        }

        if (StringUtil.valueExists(journalPublication.getIssue())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(journalPublication.getIssue(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(journalPublication.getNumberOfPages())) {
            entry.addField(new Key("pageNumber"),
                new StringValue(String.valueOf(journalPublication.getNumberOfPages()),
                    StringValue.Style.BRACED));
        }

        if (Objects.nonNull(journalPublication.getJournal())) {
            setMCBibTexField(journalPublication.getJournal().getTitle(), entry,
                BibTeXEntry.KEY_JOURNAL, defaultLanguageTag);

            if (StringUtil.valueExists(journalPublication.getJournal().getEISSN())) {
                entry.addField(new Key("e_issn"),
                    new StringValue(journalPublication.getJournal().getEISSN(),
                        StringValue.Style.BRACED));
            }

            if (StringUtil.valueExists(journalPublication.getJournal().getPrintISSN())) {
                entry.addField(new Key("print_issn"),
                    new StringValue(journalPublication.getJournal().getPrintISSN(),
                        StringValue.Style.BRACED));
            }
        }

        return entry;
    }

    public static String toTaggedFormat(JournalPublication journalPublication,
                                        String defaultLanguageTag, boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? "JOUR" : "Journal Article")
            .append("\n");

        setCommonTaggedFields(journalPublication, sb, defaultLanguageTag, refMan);

        if (StringUtil.valueExists(journalPublication.getStartPage()) &&
            StringUtil.valueExists((journalPublication.getEndPage()))) {
            sb.append(refMan ? "SE  - " : "%P ").append(journalPublication.getStartPage())
                .append("-")
                .append(journalPublication.getEndPage())
                .append("\n");
        }

        if (Objects.nonNull(journalPublication.getNumberOfPages())) {
            sb.append(refMan ? "SP  - " : "%7 ").append(journalPublication.getNumberOfPages())
                .append("\n");
        }

        if (StringUtil.valueExists(journalPublication.getArticleNumber())) {
            sb.append(refMan ? "RI  - " : "%N ").append("articleNumber:")
                .append(journalPublication.getArticleNumber()).append("\n");
        }

        if (StringUtil.valueExists(journalPublication.getVolume())) {
            sb.append(refMan ? "C6  - " : "%V ").append(journalPublication.getVolume())
                .append("\n");
        }

        if (StringUtil.valueExists(journalPublication.getIssue())) {
            sb.append(refMan ? "C2  - " : "%N ").append(journalPublication.getIssue()).append("\n");
        }

        if (Objects.nonNull(journalPublication.getJournal())) {
            setMCTaggedField(journalPublication.getJournal().getTitle(), sb, "JA",
                defaultLanguageTag);

            if (StringUtil.valueExists(journalPublication.getJournal().getEISSN())) {
                sb.append(refMan ? "SN  - " : "%@ ").append("e:")
                    .append(journalPublication.getJournal().getEISSN()).append("\n");
            }

            if (StringUtil.valueExists(journalPublication.getJournal().getPrintISSN())) {
                sb.append(refMan ? "SN  - " : "%@ ").append("print:")
                    .append(journalPublication.getJournal().getPrintISSN())
                    .append("\n");
            }
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
