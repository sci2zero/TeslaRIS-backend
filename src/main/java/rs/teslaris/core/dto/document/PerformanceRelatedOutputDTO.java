package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.LanguageTagResponseDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.PerformanceRelatedOutputType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceRelatedOutputDTO extends DocumentDTO {

    @NotNull(message = "You have to provide type.")
    private PerformanceRelatedOutputType type;

    private List<MultilingualContentDTO> producer = new ArrayList<>();

    private List<MultilingualContentDTO> distributor = new ArrayList<>();

    private List<MultilingualContentDTO> sourceTitle = new ArrayList<>();

    private List<MultilingualContentDTO> otherActors = new ArrayList<>();

    private Set<Integer> languageTagIds = new HashSet<>();

    // Used only for responses

    private List<LanguageTagResponseDTO> languageTags = new ArrayList<>();
}
