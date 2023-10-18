package rs.teslaris.core.harvester.person;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TPersonName", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PersonName")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PersonName {

    @XmlElement(name = "FamilyNames")
    private String familyNames;

    @XmlElement(name = "FirstNames")
    private String firstNames;
}
