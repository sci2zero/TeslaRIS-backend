package rs.teslaris.core.dto.document;

import rs.teslaris.core.model.document.MonographPublicationType;

public class MonographPublicationDTO extends DocumentDTO {

    private MonographPublicationType monographPublicationType;

    private String startPage;

    private String endPage;

    private Integer numberOfPages;

    private String articleNumber;

    private Integer monographId;
}
