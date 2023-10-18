package rs.teslaris.core.harvester.event;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.harvester.common.Name;

@XmlType(name = "TEvent", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Event")
@Getter
@Setter
@ToString
public class Event {

    @XmlElement(name = "Type")
    private Type type;

    @XmlElement(name = "Name")
    private Name name;

    @XmlElement(name = "Place")
    private String place;

    @XmlElement(name = "Country")
    private String country;

    @XmlElement(name = "StartDate")
    private String startDate;

    @XmlElement(name = "EndDate")
    private String endDate;

    @XmlElement(name = "Description")
    private String description;


    public Event() {
    }

    public Event(Type type, Name name, String place, String country, String startDate,
                 String endDate,
                 String description) {
        this.type = type;
        this.name = name;
        this.place = place;
        this.country = country;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }
}
