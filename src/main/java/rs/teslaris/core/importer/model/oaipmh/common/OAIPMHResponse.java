package rs.teslaris.core.importer.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TOAI-PMH", propOrder = {
    "responseDate",
    "request",
    "identify",
    "listSets",
    "listMetadataFormats",
    "listRecords",
    "getRecord"
})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "OAI-PMH")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OAIPMHResponse {

    @XmlElement(name = "Identify")
    private Identify identify;

    @XmlElement(name = "ListSets")
    private ListSets listSets;

    @XmlElement(name = "ListMetadataFormats")
    private ListMetadataFormats listMetadataFormats;

    @XmlElement(name = "ListRecords")
    private ListRecords listRecords;

    @XmlElement(name = "GetRecord")
    private GetRecord getRecord;

    @XmlElement(name = "responseDate")
    private Date responseDate;

    @XmlElement(name = "request")
    private Request request;
}
