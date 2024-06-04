package rs.teslaris.core.importer.model.oaipmh.person;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.OrgUnit;

@XmlType(name = "TAffiliation")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Affiliation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Affiliation {

    @XmlElement(name = "OrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    List<OrgUnit> orgUnits;
}
