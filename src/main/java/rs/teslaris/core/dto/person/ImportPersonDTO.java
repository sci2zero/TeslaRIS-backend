package rs.teslaris.core.dto.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportPersonDTO extends BasicPersonDTO {

    private String placeOfBirth;

    private List<MultilingualContentDTO> addressLine = new ArrayList<>();

    private List<MultilingualContentDTO> addressCity = new ArrayList<>();

    private List<MultilingualContentDTO> addressState = new ArrayList<>();

    private List<MultilingualContentDTO> biography = new ArrayList<>();

    private List<MultilingualContentDTO> keywords = new ArrayList<>();

    private String postalNumber;

    /**
     * ISO 3166-1 alpha-2 code of the professional address country.
     */
    private String countryCode;

    private List<PersonNameDTO> otherNames = new ArrayList<>();

    private String privateContactEmail;

    private String privatePhoneNumber;

    private String privateFaxNumber;

    private String privateMobilePhoneNumber;

    private List<MultilingualContentDTO> privateAddressLine = new ArrayList<>();

    private List<MultilingualContentDTO> privateAddressCity = new ArrayList<>();

    private List<MultilingualContentDTO> privateAddressState = new ArrayList<>();

    private String privatePostalNumber;

    private String privateCountryCode;

    private Set<String> uris = new HashSet<>();

    private String importSource;

    private List<LanguageKnowledgeDTO> languageKnowledges = new ArrayList<>();
}
