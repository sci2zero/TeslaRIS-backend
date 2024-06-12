package rs.teslaris.core.importer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RemainingRecordsCountResponseDTO {

    private Long personCount;

    private Long ouCount;

    private Long eventCount;

    private Long patentCount;

    private Long productCount;

    private Long proceedingsCount;

    private Long journalCount;

    private Long researchArticleCount;

    private Long proceedingsPublicationCount;
}
