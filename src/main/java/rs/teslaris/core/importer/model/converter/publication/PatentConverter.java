package rs.teslaris.core.importer.model.converter.publication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.importer.model.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.model.patent.Patent;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.model.document.DocumentContributionType;

@Component
@RequiredArgsConstructor
public class PatentConverter implements RecordConverter<Patent, PatentDTO> {

    private final MultilingualContentConverter multilingualContentConverter;

    private final PersonContributionConverter personContributionConverter;


    @Override
    public PatentDTO toDTO(Patent record) {
        var dto = new PatentDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setNumber(record.getPatentNumber());

        dto.setSubTitle(new ArrayList<>());
        dto.setDescription(new ArrayList<>());
        dto.setKeywords(new ArrayList<>());
        dto.setUris(new HashSet<>());

        var dateFormatter = new SimpleDateFormat("dd.MM.yyyy");
        dto.setDocumentDate(dateFormatter.format(record.getApprovalDate()));

        dto.setContributions(new ArrayList<>());
        personContributionConverter.addContributors(record.getInventor(),
            DocumentContributionType.AUTHOR, dto.getContributions());

        return dto;
    }
}
