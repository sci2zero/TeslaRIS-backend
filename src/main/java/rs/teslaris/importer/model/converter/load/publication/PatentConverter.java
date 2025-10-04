package rs.teslaris.importer.model.converter.load.publication;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.oaipmh.patent.Patent;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@RequiredArgsConstructor
public class PatentConverter implements RecordConverter<Patent, PatentDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final PersonContributionConverter personContributionConverter;

    private final PublisherConverter publisherConverter;


    @Override
    public PatentDTO toDTO(Patent record) {
        var dto = new PatentDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setNumber(record.getPatentNumber());

        dto.setSubTitle(new ArrayList<>());

        if (Objects.nonNull(record.getKeywords()) && !record.getKeywords().isEmpty()) {
            dto.setKeywords(multilingualContentConverter.toDTO(record.getKeywords()));
        } else {
            dto.setKeywords(new ArrayList<>());
        }

        if (Objects.nonNull(record.get_abstract()) && !record.get_abstract().isEmpty()) {
            dto.setDescription(multilingualContentConverter.toDTO(record.get_abstract()));
        } else {
            dto.setDescription(new ArrayList<>());
        }

        dto.setUris(new HashSet<>());

        dto.setDocumentDate(String.valueOf(
            record.getApprovalDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .getYear()));

        dto.setContributions(new ArrayList<>());
        personContributionConverter.addContributors(record.getInventor(),
            DocumentContributionType.AUTHOR, dto.getContributions());

        dto.setDoi(record.getDoi());

        if (Objects.nonNull(record.getUrl())) {
            record.getUrl().forEach(url -> {
                if (url.startsWith("https://www.cris.uns.ac.rs/record.jsf?recordId") ||
                    url.startsWith("https://www.cris.uns.ac.rs/DownloadFileServlet")) {
                    return;
                }

                dto.getUris().add(url);
            });
        }

        if (Objects.nonNull(record.getPublisher())) {
            publisherConverter.setPublisherInformation(record.getPublisher(), dto);
        }

        return dto;
    }
}
