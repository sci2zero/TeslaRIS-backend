package rs.teslaris.core.harvester;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "TTestElement")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rootElement", namespace = "http://www.openarchives.org/OAI/2.0/")
@Getter
@Setter
public class TestElement {

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "surname")
    private String surname;


    public TestElement() {
    }

    public TestElement(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }
}
