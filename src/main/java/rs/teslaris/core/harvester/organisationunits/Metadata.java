package rs.teslaris.core.harvester.organisationunits;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "TMetadata")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "metadata")
@Getter
@Setter
public class Metadata {

    @XmlElement(name = "OrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private OrgUnit orgUnit;


    public Metadata() {
    }

    public Metadata(OrgUnit orgUnit) {
        this.orgUnit = orgUnit;
    }
}
