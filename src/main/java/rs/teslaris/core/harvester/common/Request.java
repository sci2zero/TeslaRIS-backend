package rs.teslaris.core.harvester.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "TRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "request")
@Getter
@Setter
public class Request {

    @XmlAttribute(name = "verb")
    private String verb;

    @XmlAttribute(name = "set")
    private String set;

    @XmlAttribute(name = "metadataPrefix")
    private String metadataPrefix;

    @XmlValue
    private String value;

    public Request() {
    }

    public Request(String verb, String set, String metadataPrefix, String value) {
        this.verb = verb;
        this.set = set;
        this.metadataPrefix = metadataPrefix;
        this.value = value;
    }
}
