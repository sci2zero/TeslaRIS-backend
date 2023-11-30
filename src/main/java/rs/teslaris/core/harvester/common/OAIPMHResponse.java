package rs.teslaris.core.harvester.common;

import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TOAI-PMH", propOrder = {"responseDate", "request", "listRecords"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "OAI-PMH")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OAIPMHResponse {

    @XmlElement(name = "ListRecords")
    private ListRecords listRecords;

    @XmlElement(name = "responseDate")
    private Date responseDate;

    @XmlElement(name = "request")
    private Request request;
}
