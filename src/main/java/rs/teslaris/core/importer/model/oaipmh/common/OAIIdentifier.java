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
@XmlRootElement(name = "oai-identifier", namespace = "http://www.openarchives.org/OAI/2.0/oai-identifier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OAIIdentifier {

    @XmlElement(name = "scheme", required = true, namespace = "http://www.openarchives.org/OAI/2.0/oai-identifier")
    private String scheme;

    @XmlElement(name = "repositoryIdentifier", required = true, namespace = "http://www.openarchives.org/OAI/2.0/oai-identifier")
    private String repositoryIdentifier;

    @XmlElement(name = "delimiter", required = true, namespace = "http://www.openarchives.org/OAI/2.0/oai-identifier")
    private String delimiter;

    @XmlElement(name = "sampleIdentifier", required = true, namespace = "http://www.openarchives.org/OAI/2.0/oai-identifier")
    private String sampleIdentifier;
}
