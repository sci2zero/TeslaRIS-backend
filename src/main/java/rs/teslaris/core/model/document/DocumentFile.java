package rs.teslaris.core.model.document;

import rs.teslaris.core.model.commontypes.MultiLingualContent;

import java.util.Set;

public class DocumentFile {
    String filename;
    String serverFilename;
    Set<MultiLingualContent> description;
    String mimeType;
    int fileSize;
    ResourceType resourceType;
    License license;
}
