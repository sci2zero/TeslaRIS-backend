package rs.teslaris.core.importer.model.converter.publication;

import java.util.ArrayList;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.importer.model.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.model.product.Product;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.model.document.DocumentContributionType;

@Component
@RequiredArgsConstructor
public class ProductConverter implements RecordConverter<Product, SoftwareDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final PersonContributionConverter personContributionConverter;


    @Override
    public SoftwareDTO toDTO(Product record) {
        var dto = new SoftwareDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getName()));
        dto.setUris(new HashSet<>(record.getUrl()));

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
