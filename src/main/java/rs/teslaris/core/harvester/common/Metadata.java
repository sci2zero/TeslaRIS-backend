package rs.teslaris.core.harvester.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.harvester.event.Event;
import rs.teslaris.core.harvester.organisationunit.OrgUnit;
import rs.teslaris.core.harvester.person.Person;

@XmlType(name = "TMetadata")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "metadata")
@Getter
@Setter
@ToString
public class Metadata {

    @XmlElement(name = "OrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private OrgUnit orgUnit;

    @XmlElement(name = "Person", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Person person;

    @XmlElement(name = "Event", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Event event;

    public Metadata() {
    }

    public Metadata(OrgUnit orgUnit) {
        this.orgUnit = orgUnit;
    }
}
