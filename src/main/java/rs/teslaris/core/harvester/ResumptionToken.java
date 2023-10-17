package rs.teslaris.core.harvester;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "TResumptionToken")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ResumptionToken")
@Getter
@Setter
public class ResumptionToken {

    @XmlValue
    private String value;

    @XmlAttribute(name = "expirationDate")
    private String expirationDate;

    @XmlAttribute(name = "cursor")
    private String cursor;


    public ResumptionToken() {
    }

    public ResumptionToken(String value, String expirationDate, String cursor) {
        this.value = value;
        this.expirationDate = expirationDate;
        this.cursor = cursor;
    }
}
