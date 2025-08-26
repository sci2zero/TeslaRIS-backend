package rs.teslaris.core.model.oaipmh.publication;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.model.oaipmh.common.HasOldId;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;
import rs.teslaris.core.model.oaipmh.common.PersonAttributes;

@XmlType(name = "TPublication", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Publication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Publication implements PublicationConvertable, HasOldId {

    private String id;

    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "Type", namespace = "https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types")
    private String type;

    @XmlElement(name = "Language")
    private String language;

    @XmlElement(name = "Title")
    private List<MultilingualContent> title;

    @XmlElement(name = "PublishedIn")
    private PublishedIn publishedIn;

    @XmlElement(name = "Subtitle")
    private List<MultilingualContent> subtitle;

    @XmlElement(name = "PublicationDate")
    private Date publicationDate;

    @XmlElement(name = "OutputFrom")
    private OutputFrom outputFrom;

    @XmlElement(name = "Number")
    private String number;

    @XmlElement(name = "Volume")
    private String volume;

    @XmlElement(name = "Issue")
    private String issue;

    @XmlElement(name = "Edition")
    private String edition;

    @XmlElement(name = "StartPage")
    private String startPage;

    @XmlElement(name = "EndPage")
    private String endPage;

    @XmlElement(name = "DOI")
    private String doi;

    @XmlElement(name = "SCP-Number")
    private String scpNumber;

    @XmlElement(name = "ISSN")
    private String issn;

    @XmlElement(name = "ISBN")
    private String isbn;

    @XmlElement(name = "URL")
    private List<String> url;

    @XmlElementWrapper(name = "Authors")
    @XmlElement(name = "Author")
    private List<PersonAttributes> authors;

    @XmlElementWrapper(name = "Editors")
    @XmlElement(name = "Editor")
    private List<PersonAttributes> editors;

    @XmlElementWrapper(name = "Institutions")
    @XmlElement(name = "Institution")
    private List<Institution> institutions;

    @XmlElement(name = "Keyword")
    private List<MultilingualContent> keywords;

    @XmlElement(name = "Abstract")
    private List<MultilingualContent> _abstract;

    @XmlElement(name = "PartOf")
    private PartOf partOf;

    @XmlElement(name = "Access", namespace = "http://purl.org/coar/access_right")
    private String access;

    private List<Integer> importUserId;

    private Boolean loaded;

    // Additional Migration fields - not part of the OAI-PMH specification

    @XmlElementWrapper(name = "Advisors")
    @XmlElement(name = "Advisor")
    private List<PersonAttributes> advisors;

    @XmlElementWrapper(name = "BoardMembers")
    @XmlElement(name = "BoardMember")
    private List<PersonAttributes> boardMembers;

    @XmlElement(name = "AlternativeTitle")
    private List<MultilingualContent> alternativeTitle;

    @XmlElement(name = "AcceptedOnDate")
    private Date acceptedOnDate;

    @XmlElement(name = "DefendedOnDate")
    private Date defendedOnDate;

    @XmlElement(name = "PublicReviewStartDate")
    private Date publicReviewStartDate;

    @XmlElement(name = "NumberOfPages")
    private Integer numberOfPages;

    @XmlElement(name = "NumberOfChapters")
    private Integer numberOfChapters;

    @XmlElement(name = "NumberOfReferences")
    private Integer numberOfReferences;

    @XmlElement(name = "NumberOfTables")
    private Integer numberOfTables;

    @XmlElement(name = "NumberOfPictures")
    private Integer numberOfPictures;

    @XmlElement(name = "NumberOfGraphs")
    private Integer numberOfGraphs;

    @XmlElement(name = "NumberOfAppendixes")
    private Integer numberOfAppendixes;

    @XmlElement(name = "ExtendedAbstract")
    private List<MultilingualContent> extendedAbstract;

    @XmlElement(name = "Note")
    private List<MultilingualContent> note;

    @XmlElement(name = "ResearchArea")
    private String researchArea;

    @XmlElement(name = "HoldingData")
    private String holdingData;

    @XmlElement(name = "UDC")
    private String udc;

    @XmlElement(name = "LevelOfEducation")
    private String levelOfEducation;

    @XmlElement(name = "Publisher")
    private Publisher publisher;

    @XmlElement(name = "Acronym")
    private List<MultilingualContent> acronym;

    @XmlElement(name = "BookSeries")
    private BookSeries bookSeries;

    @XmlElement(name = "Invited")
    private Boolean invited;
}
