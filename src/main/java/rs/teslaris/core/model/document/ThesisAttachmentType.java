package rs.teslaris.core.model.document;

import lombok.Getter;

@Getter
public enum ThesisAttachmentType {

    FILE(ResourceType.PREPRINT),

    SUPPLEMENT(ResourceType.SUPPLEMENT),

    COMMISSION_REPORT(ResourceType.STATEMENT);


    private final ResourceType resourceType;

    ThesisAttachmentType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }
}
