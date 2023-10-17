package rs.teslaris.core.harvester;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "TOrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "OrgUnit")
@Getter
@Setter
class OrgUnit {

    @XmlAttribute(name = "id")
    private String id;

    @XmlElement(name = "Name")
    private Name name;

    @XmlElement(name = "PartOf")
    private PartOf partOf;


    public OrgUnit() {
    }

    public OrgUnit(String id, Name name, PartOf partOf) {
        this.id = id;
        this.name = name;
        this.partOf = partOf;
    }
}
