package rs.teslaris.core.importer.model.person;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TPerson", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Person {

    private String id;

    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "PersonName")
    private PersonName personName;

    @XmlElement(name = "ScopusAuthorID")
    private String scopusAuthorId;

    @XmlElement(name = "Gender")
    private String gender;

    @XmlElement(name = "ORCID")
    private String orcid;

    @XmlElement(name = "ElectronicAddress")
    private List<String> electronicAddresses;

    @XmlElement(name = "Affiliation")
    private Affiliation affiliation;

    private List<Integer> importUserId;

    private Boolean loaded;
}
