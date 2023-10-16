package rs.teslaris.core.harvester;

import javax.xml.bind.annotation.XmlElement;

public class ListRecords {

    private ResumptionToken resumptionToken;


    @XmlElement(name = "resumptionToken")
    public ResumptionToken getResumptionToken() {
        return resumptionToken;
    }

    public void setResumptionToken(ResumptionToken resumptionToken) {
        this.resumptionToken = resumptionToken;
    }
}
