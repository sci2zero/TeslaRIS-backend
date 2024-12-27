package rs.teslaris.core.dto.person;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePhotoDTO {

    @NotNull(message = "You must provide a valid left offset.")
    private Integer left;

    @NotNull(message = "You must provide a valid top offset.")
    private Integer top;

    @NotNull(message = "You must provide a valid box width.")
    private Integer width;

    @NotNull(message = "You must provide a valid box height.")
    private Integer height;

    @NotNull(message = "You must provide a valid image file.")
    private MultipartFile file;
}
