package rs.teslaris.core.model.commontypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ProfilePhotoOrLogo {

    @Column(name = "profile_image_server_name")
    private String imageServerName;

    @Column(name = "top_offset")
    private Integer topOffset;

    @Column(name = "left_offset")
    private Integer leftOffset;

    @Column(name = "height")
    private Integer height;

    @Column(name = "width")
    private Integer width;

    @Column(name = "background_hex")
    private String backgroundHex;
}
