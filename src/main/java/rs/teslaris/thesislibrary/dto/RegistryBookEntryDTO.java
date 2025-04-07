package rs.teslaris.thesislibrary.dto;

import java.time.LocalDate;
import java.util.Set;
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

    private RegistryBookPersonalInformationDTO personalInformation;

    private RegistryBookContactInformationDTO contactInformation;

    private PreviousTitleInformationDTO previousTitleInformation;

    private Integer promotionId;
}

