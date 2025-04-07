package rs.teslaris.thesislibrary.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDTO {

    Integer id;

    LocalDate promotionDate;

    LocalTime promotionTime;

    String placeOrVenue;

    List<MultilingualContentDTO> description;
}
