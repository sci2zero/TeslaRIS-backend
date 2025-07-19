package rs.teslaris.core.model.oaipmh.person;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.model.oaipmh.common.HasOldId;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;

@XmlType(name = "TPerson", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Person implements PersonConvertable, HasOldId {

    private String id;

    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "PersonName")
    private PersonName personName;

    @XmlElement(name = "ORCID")
    private String orcid;

    @XmlElement(name = "ScopusAuthorID")
    private String scopusAuthorId;

    @XmlElement(name = "Gender")
    private String gender;

    @XmlElement(name = "ElectronicAddress")
    private List<String> electronicAddresses;

    @XmlElement(name = "Affiliation")
    private Affiliation affiliation;

    private List<Integer> importUserId;

    private Boolean loaded;

    // Additional Migration fields - not part of the OAI-PMH specification

    @XmlElement(name = "DateOfBirth")
    private Date birthDate;

    @XmlElement(name = "PlaceOfBirth")
    private String placeOfBirth;

    @XmlElement(name = "CountryOfBirth")
    private String countryOfBirth;

    @XmlElement(name = "YearOfBirth")
    private Integer yearOfBirth;

    @XmlElement(name = "Title")
    private String title;

    @XmlElement(name = "AddressLine")
    private String addressLine;

    @XmlElement(name = "Place")
    private String place;

    @XmlElement(name = "CV")
    private List<MultilingualContent> cv;

    @XmlElement(name = "ResearchArea")
    private List<String> researchArea;

    @XmlElement(name = "Keyword")
    private List<MultilingualContent> keywords;

    @XmlElementWrapper(name = "Positions")
    @XmlElement(name = "Position")
    private List<Position> positions;
}
