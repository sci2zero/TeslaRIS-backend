package rs.teslaris.core.importer.model.converter.publication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.importer.model.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.model.publication.Publication;
import rs.teslaris.core.model.document.DocumentContributionType;

@Component
@RequiredArgsConstructor
public abstract class DocumentConverter {

    protected final MultilingualContentConverter multilingualContentConverter;

    private final PersonContributionConverter personContributionConverter;


    protected void setCommonFields(Publication record, DocumentDTO dto) {
        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setSubTitle(multilingualContentConverter.toDTO(record.getSubtitle()));
        if (Objects.nonNull(record.getPublicationDate())) {
            dto.setDocumentDate(record.getPublicationDate().toString());
        }

        if (Objects.nonNull(record.getKeywords())) {
            dto.setKeywords(
                multilingualContentConverter.toDTO(Strings.join(record.getKeywords(), ", ")));
        } else {
            dto.setKeywords(new ArrayList<>());
        }

        if (Objects.nonNull(record.getUrl())) {
            dto.setUris(new HashSet<>(record.getUrl()));
        }

        dto.setDoi(record.getDoi());
        dto.setDescription(multilingualContentConverter.toDTO(record.get_abstract()));

        setContributionInformation(record, dto);
    }

    private void setContributionInformation(Publication record, DocumentDTO dto) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();

        personContributionConverter.addContributors(record.getAuthors(),
            DocumentContributionType.AUTHOR, contributions);
        personContributionConverter.addContributors(record.getEditors(),
            DocumentContributionType.EDITOR, contributions);

        dto.setContributions(contributions);
    }
}
