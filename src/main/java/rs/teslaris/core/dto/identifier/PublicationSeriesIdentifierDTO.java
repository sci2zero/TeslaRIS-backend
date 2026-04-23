package rs.teslaris.core.dto.identifier;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicationSeriesIdentifierDTO extends EntityIdentifierDTO {

    @NotNull(message = "You have to provide publication series ID.")
    @Positive(message = "Publication series ID must be a positive number.")
    private Integer publicationSeriesId;


    public PublicationSeriesIdentifierDTO(String value, Integer identifierId,
                                          Integer publicationSeriesId) {
        super(value, identifierId);
        this.publicationSeriesId = publicationSeriesId;
    }
}
