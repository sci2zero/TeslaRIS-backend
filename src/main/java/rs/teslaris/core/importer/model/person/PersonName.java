package rs.teslaris.core.importer.model.person;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
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
