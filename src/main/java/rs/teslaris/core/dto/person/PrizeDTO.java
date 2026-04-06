package rs.teslaris.core.dto.person;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.person.PrizeType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrizeDTO {

    private List<MultilingualContentDTO> title;

    private List<MultilingualContentDTO> description;

    private List<MultilingualContentDTO> keywords = new ArrayList<>();

    private LocalDate date;

    private LocalDate endDate;

    private PrizeType prizeType;

    private Boolean favorite;

    @NotNull(message = "You have to provide research area ids.")
    private Set<Integer> researchAreasId = new HashSet<>();
}
