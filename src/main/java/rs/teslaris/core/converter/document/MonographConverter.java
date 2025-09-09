package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.model.document.Monograph;

public class MonographConverter extends DocumentPublicationConverter {

    public static MonographDTO toDTO(Monograph monograph) {
        var response = new MonographDTO();

        setCommonFields(monograph, response);
        setMonographAffiliatedFields(monograph, response);

        return response;
    }

    private static void setMonographAffiliatedFields(Monograph monograph,
                                                     MonographDTO monographResponseDTO) {
        monographResponseDTO.setEisbn(monograph.getEISBN());
        monographResponseDTO.setPrintISBN(monograph.getPrintISBN());
        monographResponseDTO.setNumberOfPages(monograph.getNumberOfPages());
        monographResponseDTO.setMonographType(monograph.getMonographType());
        monographResponseDTO.setNumber(monograph.getNumber());
        monographResponseDTO.setVolume(monograph.getVolume());

        monographResponseDTO.setLanguageTagIds(new ArrayList<>());

        if (Objects.nonNull(monograph.getResearchArea())) {
            monographResponseDTO.setResearchAreaId(monograph.getResearchArea().getId());
        }

        if (Objects.nonNull(monograph.getPublisher())) {
            monographResponseDTO.setPublisherId(monograph.getPublisher().getId());
        } else {
            monographResponseDTO.setAuthorReprint(monograph.getAuthorReprint());
        }

        setLanguageInfo(monograph, monographResponseDTO);
        setPublicationSeriesInfo(monograph, monographResponseDTO);
    }

    private static void setLanguageInfo(Monograph monograph,
                                        MonographDTO monographResponseDTO) {
        monograph.getLanguages().forEach(languageTag -> {
            monographResponseDTO.getLanguageTagIds().add(languageTag.getId());
        });
    }

    private static void setPublicationSeriesInfo(Monograph monograph,
                                                 MonographDTO monographResponseDTO) {
        var publicationSeries = monograph.getPublicationSeries();

        if (Objects.isNull(publicationSeries)) {
            return;
        }

        monographResponseDTO.setPublicationSeriesId(publicationSeries.getId());
    }

    public static BibTeXEntry toBibTexEntry(Monograph monograph, String defaultLanguageTag) {
        var entry = new BibTeXEntry(BibTeXEntry.TYPE_BOOK,
            new Key("(TESLARIS)" + monograph.getId().toString()));

        setCommonFields(monograph, entry, defaultLanguageTag);

        if (valueExists(monograph.getNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(monograph.getNumber(), StringValue.Style.BRACED));
        }

        if (valueExists(monograph.getVolume())) {
            entry.addField(BibTeXEntry.KEY_VOLUME,
                new StringValue(monograph.getVolume(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(monograph.getNumberOfPages())) {
            entry.addField(new Key("pageNumber"),
                new StringValue(String.valueOf(monograph.getNumberOfPages()),
                    StringValue.Style.BRACED));
        }

        if (Objects.nonNull(monograph.getPublicationSeries())) {
            setMCBibTexField(monograph.getPublicationSeries().getTitle(), entry,
                BibTeXEntry.KEY_SERIES, defaultLanguageTag);
        }

        if (Objects.nonNull(monograph.getPublisher())) {
            setMCBibTexField(monograph.getPublisher().getName(), entry,
                BibTeXEntry.KEY_PUBLISHER, defaultLanguageTag);
        } else if (Objects.nonNull(monograph.getAuthorReprint()) && monograph.getAuthorReprint()) {
            entry.addField(BibTeXEntry.KEY_PUBLISHER,
                new StringValue(getAuthorReprintString(defaultLanguageTag),
                    StringValue.Style.BRACED));
        }

        if (valueExists(monograph.getEISBN())) {
            entry.addField(new Key("eIsbn"),
                new StringValue(monograph.getEISBN(), StringValue.Style.BRACED));
        }

        if (valueExists(monograph.getPrintISBN())) {
            entry.addField(new Key("printIsbn"),
                new StringValue(monograph.getPrintISBN(), StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(Monograph monograph, String defaultLanguageTag,
                                        boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append("SER").append("\n");

        setCommonTaggedFields(monograph, sb, defaultLanguageTag, refMan);

        if (Objects.nonNull(monograph.getNumberOfPages())) {
            sb.append(refMan ? "SP  - " : "%0P ").append(monograph.getNumberOfPages()).append("\n");
        }

        if (valueExists(monograph.getVolume())) {
            sb.append(refMan ? "C6  - " : "%V ").append(monograph.getVolume()).append("\n");
        }

        if (valueExists(monograph.getNumber())) {
            sb.append(refMan ? "C7  - " : "%N ").append(monograph.getNumber()).append("\n");
        }

        if (Objects.nonNull(monograph.getPublisher())) {
            setMCTaggedField(monograph.getPublisher().getName(), sb, refMan ? "PB" : "%I",
                defaultLanguageTag);
        } else if (Objects.nonNull(monograph.getAuthorReprint()) && monograph.getAuthorReprint()) {
            sb.append(refMan ? "PB  - " : "%I ").append(getAuthorReprintString(defaultLanguageTag))
                .append("\n");
        }

        if (Objects.nonNull(monograph.getPublicationSeries())) {
            setMCTaggedField(monograph.getPublicationSeries().getTitle(), sb, "T2",
                defaultLanguageTag);

            if (valueExists(monograph.getPublicationSeries().getEISSN())) {
                sb.append(refMan ? "SN  - " : "%0S ").append("e:")
                    .append(monograph.getPublicationSeries().getEISSN())
                    .append("\n");
            }

            if (valueExists(monograph.getPublicationSeries().getPrintISSN())) {
                sb.append(refMan ? "SN  - " : "%0S ").append("print:")
                    .append(monograph.getPublicationSeries().getPrintISSN())
                    .append("\n");
            }
        }

        if (valueExists(monograph.getEISBN())) {
            sb.append(refMan ? "SN  - " : "%0S ").append("e:").append(monograph.getPrintISBN())
                .append("\n");
        }

        if (valueExists(monograph.getPrintISBN())) {
            sb.append(refMan ? "SN  - " : "%0S ").append("print:").append(monograph.getPrintISBN())
                .append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
