package rs.teslaris.core.model.oaipmh.common;

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

@XmlType(name = "TRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Request {

    @XmlAttribute(name = "verb")
    private String verb;

    @XmlAttribute(name = "set")
    private String set;

    @XmlAttribute(name = "metadataPrefix")
    private String metadataPrefix;

    @XmlValue
    private String value;
}
