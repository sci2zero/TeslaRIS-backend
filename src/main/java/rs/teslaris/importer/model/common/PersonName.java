package rs.teslaris.importer.model.common;

import java.text.MessageFormat;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonName {

    @Field("first_name")
    private String firstName;

    @Field("middle_name")
    private String middleName;

    @Field("last_name")
    private String lastName;


    @Override
    public String toString() {
        if (Objects.isNull(middleName) || middleName.isEmpty()) {
            return MessageFormat.format("{0} {1}", firstName, lastName);
        }

        return MessageFormat.format("{0} {1} {2}", firstName, middleName, lastName);
    }
}
