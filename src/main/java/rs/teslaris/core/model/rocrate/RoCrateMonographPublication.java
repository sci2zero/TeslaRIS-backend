package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RoCrateMonographPublication extends RoCratePublicationBase {

    private String additionalType;

    private String monographTitle;

    private String chapterNumber;

    private String pageStart;

    private String pageEnd;


    public RoCrateMonographPublication() {
        this.setType("Chapter");
        this.additionalType = "ScholarlyArticle";
    }
}
