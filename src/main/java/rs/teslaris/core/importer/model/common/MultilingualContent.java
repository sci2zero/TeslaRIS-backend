package rs.teslaris.core.importer.model.common;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MultilingualContent {

    @Field("language_tag")
    private String languageTag;

    @Field("content")
    private String content;

    @Field("priority")
    private int priority;
}
