package rs.teslaris.thesislibrary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistryBookEntryDTO {

    private Integer id;

    @Valid
    @NotNull(message = "You have to provide dissertation information.")
    private DissertationInformationDTO dissertationInformation;

    @Valid
    @NotNull(message = "You have to provide author's personal information.")
    private RegistryBookPersonalInformationDTO personalInformation;

    @Valid
    @NotNull(message = "You have to provide author's contact information.")
    private RegistryBookContactInformationDTO contactInformation;

    @Valid
    @NotNull(message = "You have to provide previous title information.")
    private PreviousTitleInformationDTO previousTitleInformation;

    // used in response
    private Boolean inPromotion = false;

    private Boolean promoted = false;
}

