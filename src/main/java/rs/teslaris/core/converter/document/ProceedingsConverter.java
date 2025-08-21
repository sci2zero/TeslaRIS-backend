package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.model.document.Proceedings;

public class ProceedingsConverter extends DocumentPublicationConverter {

    public static ProceedingsResponseDTO toDTO(Proceedings proceedings) {
        var response = new ProceedingsResponseDTO();

        setCommonFields(proceedings, response);
        setProceedingsAffiliatedFields(proceedings, response);

        return response;
    }

    private static void setProceedingsAffiliatedFields(Proceedings proceedings,
                                                       ProceedingsResponseDTO proceedingsResponseDTO) {
        proceedingsResponseDTO.setEISBN(proceedings.getEISBN());
        proceedingsResponseDTO.setPrintISBN(proceedings.getPrintISBN());
        proceedingsResponseDTO.setNumberOfPages(proceedings.getNumberOfPages());
        proceedingsResponseDTO.setPublicationSeriesIssue(proceedings.getPublicationSeriesIssue());
        proceedingsResponseDTO.setPublicationSeriesVolume(proceedings.getPublicationSeriesVolume());

        proceedingsResponseDTO.setLanguageTagIds(new ArrayList<>());
        setLanguageInfo(proceedings, proceedingsResponseDTO);
        setPublicationSeriesInfo(proceedings, proceedingsResponseDTO);
        setEventInfo(proceedings, proceedingsResponseDTO);
        setPublisherInfo(proceedings, proceedingsResponseDTO);
    }

    private static void setLanguageInfo(Proceedings proceedings,
                                        ProceedingsResponseDTO proceedingsResponseDTO) {
        proceedings.getLanguages().forEach(languageTag -> {
            proceedingsResponseDTO.getLanguageTagIds().add(languageTag.getId());
        });
    }

    private static void setPublicationSeriesInfo(Proceedings proceedings,
                                                 ProceedingsResponseDTO proceedingsResponseDTO) {
        var publicationSeries = proceedings.getPublicationSeries();
        proceedingsResponseDTO.setPublicationSeriesId(
            publicationSeries != null ? publicationSeries.getId() : 0);
    }

    private static void setEventInfo(Proceedings proceedings,
                                     ProceedingsResponseDTO proceedingsResponseDTO) {
        var event = proceedings.getEvent();
        proceedingsResponseDTO.setEventId(event.getId());
        proceedingsResponseDTO.setEventName(
            MultilingualContentConverter.getMultilingualContentDTO(event.getName()));
    }

    private static void setPublisherInfo(Proceedings proceedings,
                                         ProceedingsResponseDTO proceedingsResponseDTO) {
        var publisher = proceedings.getPublisher();
        if (publisher == null) {
            return;
        }

        proceedingsResponseDTO.setPublisherId(publisher.getId());
        proceedingsResponseDTO.setPublisherName(
            MultilingualContentConverter.getMultilingualContentDTO(publisher.getName()));
    }

    public static BibTeXEntry toBibTexEntry(Proceedings proceedings) {
        var entry = new BibTeXEntry(new Key("collection"),
            new Key("(TESLARIS)" + proceedings.getId().toString()));

        setCommonFields(proceedings, entry);

        if (valueExists(proceedings.getPublicationSeriesIssue())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(proceedings.getPublicationSeriesIssue(), StringValue.Style.BRACED));
        }

        if (valueExists(proceedings.getPublicationSeriesVolume())) {
            entry.addField(BibTeXEntry.KEY_VOLUME,
                new StringValue(proceedings.getPublicationSeriesVolume(),
                    StringValue.Style.BRACED));
        }

        if (Objects.nonNull(proceedings.getNumberOfPages())) {
            entry.addField(new Key("pageNumber"),
                new StringValue(String.valueOf(proceedings.getNumberOfPages()),
                    StringValue.Style.BRACED));
        }

        if (Objects.nonNull(proceedings.getPublicationSeries())) {
            setMCBibTexField(proceedings.getPublicationSeries().getTitle(), entry,
                new Key("publicationSeries"));

            if (valueExists(proceedings.getPublicationSeries().getEISSN())) {
                entry.addField(new Key("eIssn"),
                    new StringValue(proceedings.getPublicationSeries().getEISSN(),
                        StringValue.Style.BRACED));
            }

            if (valueExists(proceedings.getPublicationSeries().getPrintISSN())) {
                entry.addField(new Key("printIssn"),
                    new StringValue(proceedings.getPublicationSeries().getPrintISSN(),
                        StringValue.Style.BRACED));
            }
        }

        if (Objects.nonNull(proceedings.getPublisher())) {
            setMCBibTexField(proceedings.getPublisher().getName(), entry,
                BibTeXEntry.KEY_PUBLISHER);
        }

        if (valueExists(proceedings.getEISBN())) {
            entry.addField(new Key("eIsbn"),
                new StringValue(proceedings.getEISBN(), StringValue.Style.BRACED));
        }

        if (valueExists(proceedings.getPrintISBN())) {
            entry.addField(new Key("printIsbn"),
                new StringValue(proceedings.getPrintISBN(), StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(Proceedings proceedings, boolean refMan) {
        var sb = new StringBuilder();
        sb.append("TY  - ").append("CONF").append("\n");

        setCommonTaggedFields(proceedings, sb, refMan);

        if (Objects.nonNull(proceedings.getNumberOfPages())) {
            sb.append(refMan ? "SP  - " : "%0P ").append(proceedings.getNumberOfPages())
                .append("\n");
        }

        if (Objects.nonNull(proceedings.getPublisher())) {
            setMCTaggedField(proceedings.getPublisher().getName(), sb, refMan ? "PB" : "%I");
        }

        if (valueExists(proceedings.getPublicationSeriesIssue())) {
            sb.append(refMan ? "M1  - " : "%N ").append(proceedings.getPublicationSeriesIssue())
                .append("\n");
        }

        if (valueExists(proceedings.getPublicationSeriesVolume())) {
            sb.append(refMan ? "SV  - " : "%V ").append(proceedings.getPublicationSeriesVolume())
                .append("\n");
        }

        if (Objects.nonNull(proceedings.getPublicationSeries())) {
            setMCTaggedField(proceedings.getPublicationSeries().getTitle(), sb,
                refMan ? "JA" : "%J");

            if (valueExists(proceedings.getPublicationSeries().getEISSN())) {
                sb.append(refMan ? "SN  - " : "%0S ").append("e:")
                    .append(proceedings.getPublicationSeries().getEISSN())
                    .append("\n");
            }

            if (valueExists(proceedings.getPublicationSeries().getPrintISSN())) {
                sb.append(refMan ? "SN  - " : "%0S ").append("print:")
                    .append(proceedings.getPublicationSeries().getPrintISSN())
                    .append("\n");
            }
        }

        if (valueExists(proceedings.getEISBN())) {
            sb.append(refMan ? "SN  - " : "%0S ").append("e:").append(proceedings.getEISBN())
                .append("\n");
        }

        if (valueExists(proceedings.getPrintISBN())) {
            sb.append(refMan ? "SN  - " : "%0S ").append("print:")
                .append(proceedings.getPrintISBN()).append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
