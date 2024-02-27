package rs.teslaris.core.importer.model.common;

import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TResumptionToken")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ResumptionToken")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ResumptionToken {

    @XmlValue
    private String value;

    @XmlAttribute(name = "expirationDate")
    private Date expirationDate;

    @XmlAttribute(name = "cursor")
    private String cursor;
}
