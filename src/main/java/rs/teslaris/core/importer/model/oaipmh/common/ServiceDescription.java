package rs.teslaris.core.importer.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Service", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServiceDescription {

    @XmlElement(name = "Compatibility", namespace = "https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Service_Compatibility")
    private String compatibility;

    @XmlElement(name = "Acronym")
    private String acronym;

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "Description")
    private String description;

    @XmlElement(name = "WebsiteURL")
    private String websiteURL;

    @XmlElement(name = "OAIPMHBaseURL")
    private String oaiPMHBaseURL;
}
