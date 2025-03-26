package rs.teslaris.core.model.oaipmh.common;

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
import rs.teslaris.core.model.oaipmh.person.Person;

@XmlType(name = "TPersonAttributes", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PersonAttributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PersonAttributes {

    @XmlElement(name = "DisplayName")
    private String displayName;

    @XmlElement(name = "Person", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Person person;
}
