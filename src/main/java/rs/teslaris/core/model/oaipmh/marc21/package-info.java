@XmlSchema(
    namespace = "http://www.openarchives.org/OAI/2.0/",
    elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns = {
        @jakarta.xml.bind.annotation.XmlNs(prefix = "marc", namespaceURI = "http://www.loc.gov/MARC21/slim")
    },
    location = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd https://www.openaire.eu/cerif-profile/ https://www.openaire.eu/schema/oai_cerif_openaire.xsd"
)

package rs.teslaris.core.model.oaipmh.marc21;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
