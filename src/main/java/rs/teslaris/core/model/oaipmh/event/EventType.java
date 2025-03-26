package rs.teslaris.core.model.oaipmh.event;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
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
