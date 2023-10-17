package rs.teslaris.core.harvester.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "THeader")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "header")
@Getter
@Setter
public class Header {

    @XmlElement(name = "identifier")
    private String identifier;

    @XmlElement(name = "datestamp")
    private String datestamp;

    @XmlElement(name = "setSpec")
    private String setSpec;


    public Header() {
    }

    public Header(String identifier, String datestamp, String setSpec) {
        this.identifier = identifier;
        this.datestamp = datestamp;
        this.setSpec = setSpec;
    }
}
