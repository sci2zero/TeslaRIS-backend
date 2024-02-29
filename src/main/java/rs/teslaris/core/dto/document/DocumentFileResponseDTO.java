package rs.teslaris.core.dto.document;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.ResourceType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFileResponseDTO {

    private Integer id;

    private String fileName;

    private String serverFilename;

    private List<MultilingualContentDTO> description;

    private ResourceType resourceType;

    private License license;

    private Long sizeInMb;
}
