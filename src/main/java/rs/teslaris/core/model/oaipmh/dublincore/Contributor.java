package rs.teslaris.core.model.oaipmh.dublincore;

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

@XmlType(name = "TContributor", namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "contributor", namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Contributor {

    @XmlValue
    private String value;

    @XmlAttribute(name = "role")
    private String role;

    @XmlAttribute(name = "authority")
    private String authority;
}
