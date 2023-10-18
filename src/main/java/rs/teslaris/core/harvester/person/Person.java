package rs.teslaris.core.harvester.person;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TPerson", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Person")
@Getter
@Setter
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

    @XmlElement(name = "ElectronicAddress")
    private List<String> electronicAddresses;

    @XmlElement(name = "Affiliation")
    private Affiliation affiliation;


    public Person() {
    }

    public Person(String id, PersonName personName, String scopusAuthorId, String gender,
                  List<String> electronicAddresses, Affiliation affiliation) {
        this.id = id;
        this.personName = personName;
        this.scopusAuthorId = scopusAuthorId;
        this.gender = gender;
        this.electronicAddresses = electronicAddresses;
        this.affiliation = affiliation;
    }
}
