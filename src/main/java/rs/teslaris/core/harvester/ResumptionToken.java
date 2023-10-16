package rs.teslaris.core.harvester;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class ResumptionToken {

    private String value;

    private String expirationDate;

    private String cursor;

    @XmlValue
    public String getValue() {
        return value;
    }

    @XmlAttribute(name = "expirationDate")
    public String getExpirationDate() {
        return expirationDate;
    }

    @XmlAttribute(name = "cursor")
    public String getCursor() {
        return cursor;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }
}
