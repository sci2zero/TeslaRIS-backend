package rs.teslaris.core.harvester.organisationunits;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

@XmlType(name = "TOAI-PMH", propOrder = {"responseDate", "request", "listRecords"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "OAI-PMH")
@Getter
@Setter
public class OAIPMHOrgUnitResponse {

    @XmlElement(name = "ListRecords")
    private ListRecords listRecords;

    @XmlElement(name = "responseDate")
    private String responseDate;

    @XmlElement(name = "request")
    private Request request;


    public OAIPMHOrgUnitResponse() {
    }

    public OAIPMHOrgUnitResponse(ListRecords listRecords, String responseDate, Request request) {
        this.listRecords = listRecords;
        this.responseDate = responseDate;
        this.request = request;
    }
}
