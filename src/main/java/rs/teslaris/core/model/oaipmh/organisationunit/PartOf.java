package rs.teslaris.core.model.oaipmh.organisationunit;

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

@XmlType(name = "TPartOf")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PartOf")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PartOf {

    @XmlElement(name = "OrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private OrgUnit orgUnit;
}
