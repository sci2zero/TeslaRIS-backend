package rs.teslaris.core.importer.model.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Date;
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

    @XmlAttribute(name = "status")
    private String status;

    @XmlElement(name = "identifier")
    private String identifier;

    @XmlElement(name = "datestamp")
    private Date datestamp;

    @XmlElement(name = "setSpec")
    private String setSpec;
}
