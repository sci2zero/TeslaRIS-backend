@XmlSchema(
    namespace = "http://www.openarchives.org/OAI/2.0/",
    elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns = {
        @jakarta.xml.bind.annotation.XmlNs(prefix = "xsi", namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "xml", namespaceURI = "http://www.w3.org/XML/1998/namespace"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "xs", namespaceURI = "http://www.w3.org/2001/XMLSchema"),

        // Main OAI-PMH namespaces
        @jakarta.xml.bind.annotation.XmlNs(prefix = "oai", namespaceURI = "http://www.openarchives.org/OAI/2.0/"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "oai-id", namespaceURI = "http://www.openarchives.org/OAI/2.0/oai-identifier"),

        // OpenAIRE CERIF namespaces
        @jakarta.xml.bind.annotation.XmlNs(prefix = "cerif", namespaceURI = "https://www.openaire.eu/cerif-profile/1.1/"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "oa-service", namespaceURI = "https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Service_Compatibility"),

        // VTDLib toolkit namespace
        @jakarta.xml.bind.annotation.XmlNs(prefix = "vt", namespaceURI = "http://oai.dlib.vt.edu/OAI/metadata/toolkit"),

        // COAR vocabularies
        @jakarta.xml.bind.annotation.XmlNs(prefix = "coar-pub", namespaceURI = "https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "coar-patent", namespaceURI = "https://www.openaire.eu/cerif-profile/vocab/COAR_Patent_Types"),
        @jakarta.xml.bind.annotation.XmlNs(prefix = "coar-product", namespaceURI = "https://www.openaire.eu/cerif-profile/vocab/COAR_Product_Types"),

        // Access rights namespace
        @jakarta.xml.bind.annotation.XmlNs(prefix = "access", namespaceURI = "http://purl.org/coar/access_right"),

        // Default namespace (OAI-PMH)
        @jakarta.xml.bind.annotation.XmlNs(prefix = "", namespaceURI = "http://www.openarchives.org/OAI/2.0/")
    },
    location = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd https://www.openaire.eu/cerif-profile/ https://www.openaire.eu/schema/oai_cerif_openaire.xsd"
)

package rs.teslaris.core.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
