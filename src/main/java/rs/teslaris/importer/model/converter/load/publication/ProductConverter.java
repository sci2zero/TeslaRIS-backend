package rs.teslaris.importer.model.converter.load.publication;

import java.util.ArrayList;
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


    @Override
    public SoftwareDTO toDTO(Product record) {
        var dto = new SoftwareDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getName()));
        DocumentConverter.addUrlsWithoutCRISUNSLandingPages(record.getUrl(), dto);

        dto.setSubTitle(new ArrayList<>());
        dto.setDescription(new ArrayList<>());
        dto.setKeywords(new ArrayList<>());
        dto.setDocumentDate("");

        dto.setContributions(new ArrayList<>());
        personContributionConverter.addContributors(record.getCreators(),
            DocumentContributionType.AUTHOR, dto.getContributions());

        return dto;
    }
}
