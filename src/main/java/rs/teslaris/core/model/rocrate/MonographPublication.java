package rs.teslaris.core.model.rocrate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MonographPublication extends PublicationBase {

    private String additionalType;

    private String monographTitle;

    private String chapterNumber;

    private String pageStart;

    private String pageEnd;


    public MonographPublication() {
        this.setType("Chapter");
        this.additionalType = "ScholarlyArticle";
    }
}
