@XmlSchema(
    namespace = "http://www.openarchives.org/OAI/2.0/",
    elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns = {
        @jakarta.xml.bind.annotation.XmlNs(prefix = "dc", namespaceURI = "http://purl.org/dc/elements/1.1/"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "oai_dc", namespaceURI = "http://www.openarchives.org/OAI/2.0/oai_dc/")
    },
    location = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd https://www.openaire.eu/cerif-profile/ https://www.openaire.eu/schema/oai_cerif_openaire.xsd"
)

package rs.teslaris.core.model.oaipmh.dublincore;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
