package rs.teslaris.core.harvester.person;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TPersonName", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PersonName")
@Getter
@Setter
@ToString
public class PersonName {

    @XmlElement(name = "FamilyNames")
    private String familyNames;

    @XmlElement(name = "FirstNames")
    private String firstNames;


    public PersonName() {
    }

    public PersonName(String familyNames, String firstNames) {
        this.familyNames = familyNames;
        this.firstNames = firstNames;
    }
}
