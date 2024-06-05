package rs.teslaris.core.importer.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentLoadDTO {

    private List<MultilingualContentDTO> title;

    private List<MultilingualContentDTO> subTitle;

    private List<MultilingualContentDTO> description;

    private List<MultilingualContentDTO> keywords;

    private List<PersonDocumentContributionLoadDTO> contributions = new ArrayList<>();

    private Set<String> uris;

    private String documentDate;

    private String doi;

    private String scopusId;

    private Integer eventId;
}
