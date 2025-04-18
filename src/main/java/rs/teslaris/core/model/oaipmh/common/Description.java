package rs.teslaris.core.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TDescription")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Description {

    @XmlElement(name = "Service", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private ServiceDescription service;

    @XmlElement(name = "oai-identifier", namespace = "http://www.openarchives.org/OAI/2.0/oai-identifier")
    private OAIIdentifier oaiIdentifier;

    @XmlElement(name = "toolkit", namespace = "http://oai.dlib.vt.edu/OAI/metadata/toolkit")
    private Toolkit toolkit;
}
