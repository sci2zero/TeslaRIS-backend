package rs.teslaris.core.model.oaipmh.dublincore;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TOai_dc_type")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DCType {

    @XmlValue
    private String value;

    @XmlAttribute(namespace = "http://www.w3.org/XML/1998/namespace")
    private String lang;

    @XmlAttribute(name = "scheme")
    private String scheme;
}
