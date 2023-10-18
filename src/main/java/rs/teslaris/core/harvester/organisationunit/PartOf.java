package rs.teslaris.core.harvester.organisationunit;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TPartOf")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PartOf")
@Getter
@Setter
@ToString
public class PartOf {

    @XmlElement(name = "OrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private OrgUnit orgUnit;


    public PartOf() {
    }

    public PartOf(OrgUnit orgUnit) {
        this.orgUnit = orgUnit;
    }
}
