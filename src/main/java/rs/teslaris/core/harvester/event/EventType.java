package rs.teslaris.core.harvester.event;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TType")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EventType {

    @XmlAttribute(name = "scheme")
    private String scheme;

    @XmlValue
    private String value;
}
