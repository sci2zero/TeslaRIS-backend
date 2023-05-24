package rs.teslaris.core.dto.document;

import java.util.Set;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.ResourceType;

public class DocumentFileDTO {
    private String filename;

    private String serverFilename;

    private Set<MultilingualContentDTO> description;

    private String mimeType;

    private int fileSize;

    private ResourceType resourceType;

    private License license;
}
