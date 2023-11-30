package rs.teslaris.core.harvester.common;

import java.util.Date;
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

@XmlType(name = "THeader")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "header")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Header {

    @XmlElement(name = "identifier")
    private String identifier;

    @XmlElement(name = "datestamp")
    private Date datestamp;

    @XmlElement(name = "setSpec")
    private String setSpec;
}
