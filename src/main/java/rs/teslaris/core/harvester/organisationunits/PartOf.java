package rs.teslaris.core.harvester.organisationunits;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "TPartOf")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PartOf")
@Getter
@Setter
class PartOf {

    @XmlElement(name = "OrgUnit")
    private OrgUnit orgUnit;


    public PartOf() {
    }

    public PartOf(OrgUnit orgUnit) {
        this.orgUnit = orgUnit;
    }
}
