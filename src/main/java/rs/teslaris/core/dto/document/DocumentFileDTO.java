package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.CCLicense;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.ResourceType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DocumentFileDTO {

    private Integer id;

    @NotNull(message = "You must provide a valid document file.")
    private MultipartFile file;

    private List<MultilingualContentDTO> description;

    @NotNull(message = "You must provide a valid resource type.")
    private ResourceType resourceType;

    @NotNull(message = "You must provide a valid license.")
    private License license;

    private CCLicense ccLicense;
}
