package rs.teslaris.importer.model.converter.load.publication;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.oaipmh.product.Product;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@RequiredArgsConstructor
@Transactional
public class ProductConverter implements RecordConverter<Product, SoftwareDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final PersonContributionConverter personContributionConverter;

    private final PublisherConverter publisherConverter;


    @Override
    public SoftwareDTO toDTO(Product record) {
        var dto = new SoftwareDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getName()));
        DocumentConverter.addUrlsWithoutCRISUNSLandingPages(record.getUrl(), dto);

        dto.setSubTitle(new ArrayList<>());
        dto.setDescription(new ArrayList<>());
        dto.setKeywords(new ArrayList<>());

        dto.setDocumentDate(String.valueOf(
            record.getPublicationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .getYear()));

        dto.setContributions(new ArrayList<>());
        personContributionConverter.addContributors(record.getCreators(),
            DocumentContributionType.AUTHOR, dto.getContributions());

        dto.setDoi(record.getDoi());
        dto.setInternalNumber(record.getInternalNumber());

        if (Objects.nonNull(record.getUrl())) {
            dto.setUris(new HashSet<>());
            record.getUrl().forEach(url -> {
                dto.getUris().add(url);
            });
        }

        if (Objects.nonNull(record.getPublisher())) {
            publisherConverter.setPublisherInformation(record.getPublisher(), dto);
        }

        return dto;
    }
}
