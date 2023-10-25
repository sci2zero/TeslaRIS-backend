package rs.teslaris.core.harvester.event;

import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.harvester.common.MultilingualContent;

@XmlType(name = "TEvent", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Event {

    @XmlAttribute(name = "id")
    private String id;

    @XmlElement(name = "Type")
    private EventType eventType;

    @XmlElement(name = "Name")
    private MultilingualContent multilingualContent;

    @XmlElement(name = "Place")
    private String place;

    @XmlElement(name = "Country")
    private String country;

    @XmlElement(name = "StartDate")
    private Date startDate;

    @XmlElement(name = "EndDate")
    private Date endDate;

    @XmlElement(name = "Description")
    private String description;
}
