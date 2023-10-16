package rs.teslaris.core.harvester;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "OAI-PMH", namespace = "http://www.openarchives.org/OAI/2.0/")
public class OAIPMHResponse {

    private ListRecords listRecords;

    private String responseDate;


    @XmlElement(name = "ListRecords")
    public ListRecords getListRecords() {
        return listRecords;
    }

    @XmlElement(name = "responseDate")
    public String getResponseDate() {
        return responseDate;
    }

    public void setListRecords(ListRecords listRecords) {
        this.listRecords = listRecords;
    }

    public void setResponseDate(String responseDate) {
        this.responseDate = responseDate;
    }
}
