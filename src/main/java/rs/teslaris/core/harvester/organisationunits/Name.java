package rs.teslaris.core.harvester.organisationunits;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "TName")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Name")
@Getter
@Setter
public class Name {

    @XmlAttribute(name = "xml:lang")
    private String lang;

    @XmlValue
    private String value;


    public Name() {
    }

    public Name(String lang, String value) {
        this.lang = lang;
        this.value = value;
    }
}
