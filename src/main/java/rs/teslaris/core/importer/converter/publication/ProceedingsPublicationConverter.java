package rs.teslaris.core.importer.converter.publication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.importer.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.publication.Publication;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.service.interfaces.document.EventService;

@Component
@RequiredArgsConstructor
public class ProceedingsPublicationConverter implements
    RecordConverter<Publication, ProceedingsPublicationDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final EventService eventService;

    @Override
    public ProceedingsPublicationDTO toDTO(Publication record) {
        var dto = new ProceedingsPublicationDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setSubTitle(multilingualContentConverter.toDTO(record.getSubtitle()));
        dto.setDocumentDate(record.getPublicationDate().toString());
        dto.setArticleNumber(record.getNumber());
        dto.setStartPage(record.getStartPage());
        dto.setEndPage(record.getEndPage());
        dto.setKeywords(
            multilingualContentConverter.toDTO(Strings.join(record.getKeywords(), ", ")));
        dto.setUris(new HashSet<>(record.getUrl()));
        dto.setDoi(record.getDoi());
        dto.setDescription(multilingualContentConverter.toDTO(record.get_abstract()));

        var event = eventService.findEventByOldId(
            OAIPMHParseUtility.parseBISISID(record.getOutputFrom().getEvent().getId()));
        if (!Objects.nonNull(event)) {
            return null;
        }
        dto.setEventId(event.getId());

        var contributions = new ArrayList<PersonDocumentContributionDTO>();
        // TODO: finish contributions
        dto.setContributions(contributions);

        return dto;
    }
}
