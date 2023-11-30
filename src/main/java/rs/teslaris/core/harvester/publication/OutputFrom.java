package rs.teslaris.core.harvester.publication;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.harvester.event.Event;

@XmlType(name = "TPublication")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Publication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OutputFrom {

    @XmlElement(name = "Event", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Event event;
}
