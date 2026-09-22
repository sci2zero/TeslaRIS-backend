package rs.teslaris.core.dto.document;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.AccessRights;
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

    private AccessRights accessRights;

    private License license;

    private Long sizeInMb;

    private String mimeType;

    private Boolean isArchived;


    public DocumentFileResponseDTO(DocumentFileResponseDTO other) {
        this.id = other.id;
        this.fileName = other.fileName;
        this.serverFilename = other.serverFilename;
        this.description = Objects.nonNull(other.description)
            ?
            other.description.stream().map(MultilingualContentDTO::new).collect(Collectors.toList())
            : null;
        this.resourceType = other.resourceType;
        this.accessRights = other.accessRights;
        this.license = other.license;
        this.sizeInMb = other.sizeInMb;
        this.mimeType = other.mimeType;
        this.isArchived = other.isArchived;
    }
}
