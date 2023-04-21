package rs.teslaris.core.dto.commontypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MultilingualContentDTO {

    private Integer languageTagId;

    private String content;

    private int priority;
}
