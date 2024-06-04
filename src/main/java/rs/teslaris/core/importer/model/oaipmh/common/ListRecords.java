package rs.teslaris.core.importer.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TListRecords")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ListRecords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ListRecords {

    @XmlElement(name = "record")
    private List<Record> records;

    @XmlElement(name = "resumptionToken")
    private ResumptionToken resumptionToken;
}
