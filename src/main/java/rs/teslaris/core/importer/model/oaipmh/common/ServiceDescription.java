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

    @XmlElement(name = "Acronym", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private String acronym;

    @XmlElement(name = "Name", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Name name;

    @XmlElement(name = "Description", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private ServiceDescriptionContent description;

    @XmlElement(name = "WebsiteURL", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private String websiteURL;

    @XmlElement(name = "OAIPMHBaseURL", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private String oaiPMHBaseURL;
}
