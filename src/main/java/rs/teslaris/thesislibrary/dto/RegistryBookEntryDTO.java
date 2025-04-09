package rs.teslaris.thesislibrary.dto;

import jakarta.validation.Valid;
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

    private DissertationInformationDTO dissertationInformation;

    @Valid
    private RegistryBookPersonalInformationDTO personalInformation;

    @Valid
    private RegistryBookContactInformationDTO contactInformation;

    private PreviousTitleInformationDTO previousTitleInformation;
}

