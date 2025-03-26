@XmlSchema(
    namespace = "http://www.openarchives.org/OAI/2.0/",
    elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns = {
        @jakarta.xml.bind.annotation.XmlNs(prefix = "xsi", namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "xml", namespaceURI = "http://www.w3.org/XML/1998/namespace"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "xs", namespaceURI = "http://www.w3.org/2001/XMLSchema"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "oai-identifier", namespaceURI = "http://www.openarchives.org/OAI/2.0/oai-identifier"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "oai_cerif_openaire", namespaceURI = "https://www.openaire.eu/cerif-profile/1.1/"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "", namespaceURI = "http://www.openarchives.org/OAI/2.0/"),
    },
    location = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd https://www.openaire.eu/cerif-profile/ https://www.openaire.eu/schema/oai_cerif_openaire.xsd"
)

package rs.teslaris.core.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
