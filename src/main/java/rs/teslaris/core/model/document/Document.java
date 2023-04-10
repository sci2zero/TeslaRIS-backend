package rs.teslaris.core.model.document;

import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

import java.util.Set;

public class Document {
    Set<MultiLingualContent> title;
    Set<MultiLingualContent> subTitle;
    Set<MultiLingualContent> description;
    Set<MultiLingualContent> note;
    Set<PersonDocumentContribution> contributors;
    Set<String> uris;
    String documentDate;
    Set<DocumentFile> fileItems;
    Set<DocumentFile> proof;
    Set<MultiLingualContent> keywords;
    ApproveStatus approveStatus;
    String note;
    String doi;
    String scopusId;

    Set<PersonDocumentContribution> personDocumentContributions;
}
