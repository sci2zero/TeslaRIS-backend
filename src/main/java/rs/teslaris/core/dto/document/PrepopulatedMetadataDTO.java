package rs.teslaris.core.dto.document;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepopulatedMetadataDTO {

    private List<MultilingualContentDTO> title = new ArrayList<>();

    private List<PersonDocumentContributionDTO> contributions = new ArrayList<>();

    private String publishedInName;

    private Integer publishEntityId;

    private DocumentPublicationType documentPublicationType;

    private String volume;

    private String issue;

    private Integer year;

    private String startPage;

    private String endPage;

    private String url;
}
