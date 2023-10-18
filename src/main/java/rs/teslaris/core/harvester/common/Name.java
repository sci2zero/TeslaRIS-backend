package rs.teslaris.core.harvester.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TName")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Name")
@Getter
@Setter
@ToString
public class Name {

    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
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
