package rs.teslaris.core.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import java.util.Date;
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
    private Integer cursor;

    @XmlAttribute(name = "completeListSize")
    private Long completeListSize;
}
