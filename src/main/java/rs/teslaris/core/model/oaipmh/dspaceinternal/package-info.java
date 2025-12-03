@XmlSchema(
    namespace = "http://www.openarchives.org/OAI/2.0/",
    elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns = {
        @jakarta.xml.bind.annotation.XmlNs(prefix = "dim", namespaceURI = "http://www.dspace.org/xmlns/dspace/dim")
    },
    location = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd https://www.openaire.eu/cerif-profile/ https://www.openaire.eu/schema/oai_cerif_openaire.xsd"
)

package rs.teslaris.core.model.oaipmh.dspaceinternal;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
