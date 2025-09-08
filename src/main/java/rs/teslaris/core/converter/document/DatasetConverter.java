package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.model.document.Dataset;

public class DatasetConverter extends DocumentPublicationConverter {

    public static DatasetDTO toDTO(Dataset dataset) {
        var datasetDTO = new DatasetDTO();

        setCommonFields(dataset, datasetDTO);

        datasetDTO.setInternalNumber(dataset.getInternalNumber());
        if (Objects.nonNull(dataset.getPublisher())) {
            datasetDTO.setPublisherId(dataset.getPublisher().getId());
        } else {
            datasetDTO.setAuthorReprint(dataset.getAuthorReprint());
        }

        return datasetDTO;
    }

    public static BibTeXEntry toBibTexEntry(Dataset dataset, String defaultLanguageTag) {
        var entry = new BibTeXEntry(new Key("dataset"),
            new Key("(TESLARIS)" + dataset.getId().toString()));

        setCommonFields(dataset, entry, defaultLanguageTag);

        if (valueExists(dataset.getInternalNumber())) {
            entry.addField(BibTeXEntry.KEY_NUMBER,
                new StringValue(dataset.getInternalNumber(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(dataset.getPublisher())) {
            setMCBibTexField(dataset.getPublisher().getName(), entry, BibTeXEntry.KEY_PUBLISHER,
                defaultLanguageTag);
        } else if (Objects.nonNull(dataset.getAuthorReprint()) && dataset.getAuthorReprint()) {
            entry.addField(BibTeXEntry.KEY_PUBLISHER,
                new StringValue(getAuthorReprintString(defaultLanguageTag),
                    StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(Dataset dataset, String defaultLanguageTag,
                                        boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append("DATA").append("\n");

        setCommonTaggedFields(dataset, sb, defaultLanguageTag, refMan);

        if (valueExists(dataset.getInternalNumber())) {
            sb.append(refMan ? "C6  - " : "%N ").append(dataset.getInternalNumber()).append("\n");
        }

        if (Objects.nonNull(dataset.getPublisher())) {
            setMCTaggedField(dataset.getPublisher().getName(), sb, refMan ? "PB" : "%I",
                defaultLanguageTag);
        } else if (Objects.nonNull(dataset.getAuthorReprint()) && dataset.getAuthorReprint()) {
            sb.append(refMan ? "PB  - " : "%I ").append(getAuthorReprintString(defaultLanguageTag))
                .append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
