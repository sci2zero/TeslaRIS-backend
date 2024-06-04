package rs.teslaris.core.importer.model.oaipmh.event;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.model.oaipmh.common.MultilingualContent;

@XmlType(name = "TEvent", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Event {

    private String id;

    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "Type")
    private EventType eventType;

    @XmlElement(name = "Name")
    private List<MultilingualContent> eventName;

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

    @XmlElement(name = "Keyword")
    private List<String> keywords;

    private List<Integer> importUserId;

    private Boolean loaded;
}
