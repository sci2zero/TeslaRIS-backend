package rs.teslaris.core.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "toolkit", namespace = "http://oai.dlib.vt.edu/OAI/metadata/toolkit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Toolkit {

    @XmlElement(name = "title", required = true)
    private String title;

    @XmlElement(name = "author", required = true)
    private Author author;

    @XmlElement(name = "version", required = true)
    private String version;

    @XmlElement(name = "toolkitIcon", required = true)
    private String toolkitIcon;

    @XmlElement(name = "URL", required = true)
    private String url;

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "Author", propOrder = {
        "name",
        "email",
        "institution"
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Author {

        @XmlElement(name = "name", required = true)
        private String name;

        @XmlElement(name = "email", required = true)
        private String email;

        @XmlElement(name = "institution", required = true)
        private String institution;
    }
}
