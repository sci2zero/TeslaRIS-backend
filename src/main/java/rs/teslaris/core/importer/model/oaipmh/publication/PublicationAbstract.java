package rs.teslaris.core.importer.model.oaipmh.publication;

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

@XmlType(name = "TAbstract")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Abstract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PublicationAbstract {

    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    private String lang;

    @XmlValue
    private String value;
}
