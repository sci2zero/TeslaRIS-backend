package rs.teslaris.core.exporter.util;

import jakarta.persistence.Id;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resumptionTokenStash")
public class ResumptionTokenStash {

    @Id
    private String id;

    @Field("token_value")
    private String tokenValue;

    @Field("expiration_timestamp")
    @Indexed(name = "expiration_timestamp", expireAfterSeconds = 1)
    private Date expirationTimestamp;
}
