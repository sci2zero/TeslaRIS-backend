package rs.teslaris.core.harvester.person;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.harvester.organisationunit.OrgUnit;

@XmlType(name = "TAffiliation")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Affiliation")
@Getter
@Setter
@ToString
public class Affiliation {

    @XmlElement(name = "OrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    List<OrgUnit> orgUnits;


    public Affiliation() {
    }

    public Affiliation(List<OrgUnit> orgUnits) {
        this.orgUnits = orgUnits;
    }
}
