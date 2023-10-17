package rs.teslaris.core.harvester.organisationunits;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "TListRecords")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ListRecords")
@Getter
@Setter
public class ListRecords {

    @XmlElement(name = "record")
    private List<Record> records;

    @XmlElement(name = "resumptionToken")
    private ResumptionToken resumptionToken;


    public ListRecords() {
    }

    public ListRecords(List<Record> records, ResumptionToken resumptionToken) {
        this.records = records;
        this.resumptionToken = resumptionToken;
    }
}
