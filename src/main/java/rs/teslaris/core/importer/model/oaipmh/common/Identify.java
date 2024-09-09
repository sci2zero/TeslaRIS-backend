package rs.teslaris.core.importer.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TIdentify", propOrder = {
    "repositoryName",
    "baseURL",
    "protocolVersion",
    "adminEmail",
    "earliestDatestamp",
    "deletedRecord",
    "granularity",
    "compression",
    "description"
})
@XmlRootElement(name = "Identify")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Identify {

    @XmlElement(required = true)
    private String repositoryName;

    @XmlElement(required = true)
    private String baseURL;

    @XmlElement(required = true)
    private String protocolVersion;

    @XmlElement(required = true)
    private String adminEmail;

    @XmlElement(required = true)
    private String earliestDatestamp;

    @XmlElement(required = true)
    private String deletedRecord;

    @XmlElement(required = true)
    private String granularity;

    @XmlElement(required = true)
    private List<String> compression;

    @XmlElement(name = "description")
    private List<Description> description = new ArrayList<>();

}
