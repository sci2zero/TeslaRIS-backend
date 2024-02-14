package rs.teslaris.core.importer.converter.publication;

import java.util.ArrayList;
import java.util.HashSet;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.importer.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.publication.Publication;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;

@Component
@RequiredArgsConstructor
public class JournalPublicationConverter
    implements RecordConverter<Publication, JournalPublicationDTO> {


    private final MultilingualContentConverter multilingualContentConverter;

    @Override
    public JournalPublicationDTO toDTO(Publication record) {
        var dto = new JournalPublicationDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setSubTitle(multilingualContentConverter.toDTO(record.getSubtitle()));
        dto.setDocumentDate(record.getPublicationDate().toString());
        dto.setArticleNumber(record.getNumber());
        dto.setVolume(record.getVolume());
        dto.setIssue(record.getIssue());
        dto.setStartPage(record.getStartPage());
        dto.setEndPage(record.getEndPage());
        dto.setKeywords(
            multilingualContentConverter.toDTO(Strings.join(record.getKeywords(), ", ")));
        dto.setUris(new HashSet<>(record.getUrl()));
        dto.setDoi(record.getDoi());
        dto.setDescription(multilingualContentConverter.toDTO(record.get_abstract()));

        var contributions = new ArrayList<PersonDocumentContributionDTO>();
        // TODO: finish contributions
        dto.setContributions(contributions);

        return dto;
    }
}
