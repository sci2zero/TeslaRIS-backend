package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MonographPublication extends PublicationBase {

    private String additionalType = "Chapter";

    private String monographTitle;

    private String chapterNumber;

    private String pageStart;

    private String pageEnd;
}
