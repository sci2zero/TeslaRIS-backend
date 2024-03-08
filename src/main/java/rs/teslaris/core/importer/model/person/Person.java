package rs.teslaris.core.importer.model.person;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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

    @XmlAttribute(name = "id")
    private String id;

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

    private Integer importUserId;
}
