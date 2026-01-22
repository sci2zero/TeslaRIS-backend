package rs.teslaris.exporter.model.converter;

import com.google.common.hash.Hashing;
import java.time.LocalDate;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.GeneticMaterial;
import rs.teslaris.core.model.document.IntangibleProduct;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.document.PublisherPublishable;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.rocrate.ContextualEntity;
import rs.teslaris.core.model.rocrate.MediaObject;
import rs.teslaris.core.model.rocrate.Organization;
import rs.teslaris.core.model.rocrate.Periodical;
import rs.teslaris.core.model.rocrate.RoCrate;
import rs.teslaris.core.model.rocrate.RoCrateDataset;
import rs.teslaris.core.model.rocrate.RoCrateEvent;
import rs.teslaris.core.model.rocrate.RoCrateGeneticMaterial;
import rs.teslaris.core.model.rocrate.RoCrateIntangibleProduct;
import rs.teslaris.core.model.rocrate.RoCrateJournalPublication;
import rs.teslaris.core.model.rocrate.RoCrateMaterialProduct;
import rs.teslaris.core.model.rocrate.RoCrateMonograph;
import rs.teslaris.core.model.rocrate.RoCrateMonographPublication;
import rs.teslaris.core.model.rocrate.RoCratePatent;
import rs.teslaris.core.model.rocrate.RoCratePerson;
import rs.teslaris.core.model.rocrate.RoCrateProceedings;
import rs.teslaris.core.model.rocrate.RoCrateProceedingsPublication;
import rs.teslaris.core.model.rocrate.RoCratePublicationBase;
import rs.teslaris.core.model.rocrate.RoCratePublishable;
import rs.teslaris.core.model.rocrate.RoCrateThesis;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;

@Component
@Slf4j
public class RoCrateConverter {

    private static final String DEFAULT_RO_CRATE_LANGUAGE = LanguageAbbreviations.ENGLISH;
    public static String baseUrl;
    private static EventsRelationRepository eventsRelationRepository;

    private static FileService fileService;

    private static boolean includeFileHashes;


    public static RoCrateDataset toRoCrateModel(Dataset document, String documentIdentifier,
                                                RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "dataset");

        var metadata = new RoCrateDataset();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setIdentifier(document.getInternalNumber());
        metadata.setDistribution(
            baseUrl + "en/scientific-results/dataset/" + document.getId());

        return metadata;
    }

    public static RoCratePatent toRoCrateModel(Patent document, String documentIdentifier,
                                               RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "patent");

        var metadata = new RoCratePatent();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setPatentNumber(document.getNumber());

        return metadata;
    }

    public static RoCrateIntangibleProduct toRoCrateModel(IntangibleProduct document,
                                                          String documentIdentifier,
                                                          RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "product");

        var metadata = new RoCrateIntangibleProduct();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setIdentifier(document.getInternalNumber());

        return metadata;
    }

    public static RoCrateMaterialProduct toRoCrateModel(MaterialProduct document,
                                                        String documentIdentifier,
                                                        RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "material-product");

        var metadata = new RoCrateMaterialProduct();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);

        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setBrand(metadata.getPublisher());
        metadata.setPublisher(null);

        metadata.setIdentifier(document.getInternalNumber());
        metadata.setCategory(document.getMaterialProductType().name());
        metadata.setAudience(new ContextualEntity(null, "audience",
            StringUtil.getStringContent(document.getProductUsers(), DEFAULT_RO_CRATE_LANGUAGE),
            null, null, null, null));

        return metadata;
    }

    public static RoCrateGeneticMaterial toRoCrateModel(GeneticMaterial document,
                                                        String documentIdentifier,
                                                        RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "genetic-material");

        var metadata = new RoCrateGeneticMaterial();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setIdentifier(document.getInternalNumber());

        metadata.setAdditionalType(document.getGeneticMaterialType().name());

        return metadata;
    }

    public static RoCrateMonograph toRoCrateModel(Monograph document, String documentIdentifier,
                                                  RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "monograph");

        var metadata = new RoCrateMonograph();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setNumberOfPages(document.getNumberOfPages());
        metadata.setBookEdition(document.getNumber());

        if (Objects.nonNull(document.getPublicationSeries())) {
            var issn = getIssn(document.getPublicationSeries());
            var pubSeriesIdentifier = StringUtil.valueExists(issn) ? ("https://issn.org/" + issn) :
                (baseUrl + "en/journals/" + document.getPublicationSeries().getId());

            metadata.setIsPartOf(new ContextualEntity(pubSeriesIdentifier));

            var periodical = new Periodical(
                StringUtil.getStringContent(document.getPublicationSeries().getTitle(),
                    DEFAULT_RO_CRATE_LANGUAGE),
                issn,
                null
            );
            periodical.setId(pubSeriesIdentifier);
            periodical.setType("Periodical");
            metadataInfo.getGraph().add(periodical);

            setPublisherInfo(metadataInfo, periodical, document);
        }

        return metadata;
    }

    public static RoCrateMonographPublication toRoCrateModel(MonographPublication document,
                                                             String documentIdentifier,
                                                             RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "monograph-publication");

        var metadata = new RoCrateMonographPublication();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        setPublisherInfo(metadataInfo, metadata, document.getMonograph());
        metadata.setMonographTitle(StringUtil.getStringContent(document.getMonograph().getTitle(),
            DEFAULT_RO_CRATE_LANGUAGE));
        metadata.setChapterNumber(document.getArticleNumber());
        metadata.setPageStart(document.getStartPage());
        metadata.setPageEnd(document.getEndPage());

        return metadata;
    }

    public static RoCrateProceedings toRoCrateModel(Proceedings document, String documentIdentifier,
                                                    RoCrate metadataInfo) {
        var metadata = new RoCrateProceedings();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        setPublisherInfo(metadataInfo, metadata, document);

        if (Objects.nonNull(document.getEvent())) {
            metadata.setConferenceName(StringUtil.getStringContent(document.getEvent().getName(),
                DEFAULT_RO_CRATE_LANGUAGE));

            var event = Hibernate.unproxy(document.getEvent(), Event.class);
            metadata.setPartOf((new ContextualEntity(getEventIdentifier((Conference) event))));

            metadataInfo.getGraph()
                .add(getEventMetadata((Conference) event, metadataInfo, true, true));
        }

        return metadata;
    }

    public static RoCrateProceedingsPublication toRoCrateModel(ProceedingsPublication document,
                                                               String documentIdentifier,
                                                               RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "proceedings-publication");

        var metadata = new RoCrateProceedingsPublication();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        setPublisherInfo(metadataInfo, metadata, document.getProceedings());

        metadata.setInProceedingsTitle(
            StringUtil.getStringContent(document.getProceedings().getTitle(),
                DEFAULT_RO_CRATE_LANGUAGE));

        var isbn = getIsbn(document.getProceedings());
        var proceedingsIdentifier = StringUtil.valueExists(isbn) ? ("https://isbn.org/" + isbn) :
            (baseUrl + "en/proceedings/" + document.getProceedings().getId());

        metadata.setProceedings(new ContextualEntity(proceedingsIdentifier));
        toRoCrateModel(document.getProceedings(), proceedingsIdentifier, metadataInfo);

        return metadata;
    }

    public static RoCrateJournalPublication toRoCrateModel(JournalPublication document,
                                                           String documentIdentifier,
                                                           RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "journal-publication");

        var metadata = new RoCrateJournalPublication();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        metadata.setVolume(document.getVolume());
        metadata.setIssue(document.getIssue());
        metadata.setPageStart(document.getStartPage());
        metadata.setPageEnd(document.getEndPage());

        metadata.setPeriodicalName(StringUtil.getStringContent(document.getJournal().getTitle(),
            DEFAULT_RO_CRATE_LANGUAGE));

        var issn = getIssn(document.getJournal());
        var journalIdentifier = StringUtil.valueExists(issn) ? ("https://issn.org/" + issn) :
            (baseUrl + "en/journals/" + document.getJournal().getId());

        metadata.setIsPartOf(new ContextualEntity(journalIdentifier));

        var periodical = new Periodical(
            StringUtil.getStringContent(document.getJournal().getTitle(),
                DEFAULT_RO_CRATE_LANGUAGE),
            issn,
            null
        );
        periodical.setId(journalIdentifier);
        periodical.setType("Periodical");
        metadataInfo.getGraph().add(periodical);

        return metadata;
    }

    public static RoCrateThesis toRoCrateModel(Thesis document, String documentIdentifier,
                                               RoCrate metadataInfo) {
        documentIdentifier = documentIdentifier.replace("DOC_TYPE", "thesis");

        var metadata = new RoCrateThesis();
        setCommonFields(metadata, document, documentIdentifier, metadataInfo);
        setPublisherInfo(metadataInfo, metadata, document);

        if (Objects.nonNull(document.getOrganisationUnit())) {
            var ouIdentifier = getOrganizationUnitIdentifier(document.getOrganisationUnit());
            metadata.setSourceOrganization(new ContextualEntity(ouIdentifier, "Organization"));

            metadataInfo.getGraph()
                .add(getInstitutionMetadata(document.getOrganisationUnit(), ouIdentifier));
        } else {
            metadata.setSourceOrganization(new ContextualEntity(
                StringUtil.getStringContent(document.getExternalOrganisationUnitName(),
                    DEFAULT_RO_CRATE_LANGUAGE),
                "Organization")
            );
        }

        metadata.setDisplayLocation(
            StringUtil.getStringContent(document.getPlaceOfKeeping(), DEFAULT_RO_CRATE_LANGUAGE));

        document.getFileItems().stream().filter(
                file -> file.getResourceType().equals(ResourceType.OFFICIAL_PUBLICATION) &&
                    file.getAccessRights().equals(AccessRights.OPEN_ACCESS)).findFirst()
            .ifPresent(officialPublication -> {
                metadata.setArchivedAt(
                    baseUrl + "/api/file/" + officialPublication.getServerFilename());

                metadata.setLicense(
                    new ContextualEntity(getLicenseUrl(officialPublication.getLicense()), "URL"));

                metadata.setIsAccessibleForFree(true);
            });

        metadata.setEducationalLevel(document.getThesisType().name());
        metadata.setInSupportOf(
            StringUtil.getStringContent(document.getTypeOfTitle(), DEFAULT_RO_CRATE_LANGUAGE)
        );

        if (Objects.nonNull(document.getLanguage())) {
            metadata.setInLanguage(document.getLanguage().getLanguageCode().toLowerCase());
        }

        return metadata;
    }

    private static void setCommonFields(RoCratePublicationBase metadata, Document document,
                                        String primaryIdentifier, RoCrate metadataInfo) {
        metadata.setId(primaryIdentifier);
        metadata.setTitle(StringUtil.getStringContent(document.getTitle(),
            DEFAULT_RO_CRATE_LANGUAGE));
        metadata.setAbstractText(StringUtil.getStringContent(document.getDescription(),
            DEFAULT_RO_CRATE_LANGUAGE));
        metadata.setPublicationYear(document.getDocumentDate());

        metadata.setDoi(document.getDoi());

        if (!primaryIdentifier.startsWith(baseUrl)) {
            metadata.setCiteAs(primaryIdentifier);
        }

        setAuthorshipInfo(metadata, document, metadataInfo);
        setDocumentFileInfo(metadata, document, metadataInfo);
    }

    private static void setAuthorshipInfo(RoCratePublicationBase metadata, Document document,
                                          RoCrate metadataInfo) {
        document.getContributors().stream()
            .filter(c -> c.getContributionType().equals(DocumentContributionType.AUTHOR))
            .forEach(author -> {
                if (Objects.isNull(author.getPerson())) {
                    metadata.getAuthors().add(new ContextualEntity(
                        author.getAffiliationStatement().getDisplayPersonName().toText(),
                        "Person"));
                    return;
                }

                var identifier = getPersonIdentifier(author.getPerson());
                metadata.getAuthors().add(new ContextualEntity(identifier, "Person"));


                metadataInfo.getGraph()
                    .add(getPersonMetadata(author.getPerson(), identifier, metadataInfo));
            });
    }

    private static void setDocumentFileInfo(RoCratePublicationBase metadata, Document document,
                                            RoCrate metadataInfo) {
        document.getFileItems().stream()
            .filter(c -> c.getAccessRights().equals(AccessRights.OPEN_ACCESS) &&
                c.getApproveStatus().equals(ApproveStatus.APPROVED))
            .forEach(file -> {
                var identifier = getFileIdentifier(file);
                metadata.getFiles().add(new ContextualEntity(identifier));

                metadataInfo.getGraph().add(getDocumentFileMetadata(file, identifier));
            });
    }

    private static void setAffiliationInfo(Person person, RoCratePerson personMetadata,
                                           RoCrate metadataInfo) {
        person.getInvolvements().stream().filter(i ->
                (i.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                    i.getInvolvementType().equals(InvolvementType.HIRED_BY)) &&
                    (Objects.isNull(i.getDateTo()) || i.getDateTo().isAfter(LocalDate.now())))
            .forEach(employment -> {
                if (Objects.isNull(employment.getOrganisationUnit())) {
                    personMetadata.getAffiliations().add(new ContextualEntity(
                        StringUtil.getStringContent(employment.getAffiliationStatement(),
                            DEFAULT_RO_CRATE_LANGUAGE),
                        "Organization")
                    );
                    return;
                }

                var identifier = getOrganizationUnitIdentifier(employment.getOrganisationUnit());

                personMetadata.getAffiliations()
                    .add(new ContextualEntity(identifier, "Organization"));

                metadataInfo.getGraph()
                    .add(getInstitutionMetadata(employment.getOrganisationUnit(), identifier));
            });
    }

    private static MediaObject getDocumentFileMetadata(DocumentFile file, String identifier) {
        var metadata = new MediaObject();

        metadata.setName(file.getFilename());
        metadata.setEncodingFormat(file.getMimeType());
        metadata.setUrl(identifier);
        metadata.setContentSize(String.valueOf(file.getFileSize()));
        metadata.setLicense(new ContextualEntity(getLicenseUrl(file.getLicense()), "URL"));

        if (includeFileHashes) {
            try {
                metadata.setSha256(Hashing.sha256()
                    .hashBytes(fileService.loadAsResource(file.getServerFilename()).readAllBytes())
                    .toString());
            } catch (Exception e) {
                log.error("Unable to extract SHA256 hash for file: {}. Reason: {}",
                    file.getServerFilename(), e.getMessage());
            }
        }

        return metadata;
    }

    private static RoCratePerson getPersonMetadata(Person person, String identifier,
                                                   RoCrate metadataInfo) {
        var personMetadata = new RoCratePerson();
        personMetadata.setId(identifier);

        var personName = person.getName();
        personMetadata.setGivenName(personName.getFirstname());
        personMetadata.setFamilyName(personName.getLastname());
        personMetadata.setAdditionalName(personName.getOtherName());
        personMetadata.setDescription(StringUtil.getStringContent(person.getBiography(),
            DEFAULT_RO_CRATE_LANGUAGE));

        if (Objects.nonNull(person.getPersonalInfo()) &&
            Objects.nonNull(person.getPersonalInfo().getContact())) {
            personMetadata.setEmail(person.getPersonalInfo().getContact().getContactEmail());
        }

        setAffiliationInfo(person, personMetadata, metadataInfo);

        return personMetadata;
    }

    private static Organization getInstitutionMetadata(OrganisationUnit institution,
                                                       String identifier) {
        var organization = new Organization();
        organization.setId(identifier);
        organization.setName(StringUtil.getStringContent(institution.getName(),
            DEFAULT_RO_CRATE_LANGUAGE));
        organization.setAlternateName(institution.getNameAbbreviation());

        organization.setUrl(institution.getUris().stream().findFirst().orElse(null));
        organization.setFoundingLocation(
            Objects.nonNull(institution.getLocation()) ?
                institution.getLocation().getAddress() : null
        );

        return organization;
    }

    private static void setPublisherInfo(RoCrate metadataInfo,
                                         RoCratePublishable publicationMetadata,
                                         PublisherPublishable document) {
        if (Objects.nonNull(document.getPublisher())) {
            var publisherIdentifier = baseUrl + "en/publishers/" + document.getPublisher().getId();
            publicationMetadata.setPublisher(new ContextualEntity(publisherIdentifier));

            var publisherMetadata = new Organization(
                StringUtil.getStringContent(document.getPublisher().getName(),
                    DEFAULT_RO_CRATE_LANGUAGE),
                null,
                null,
                StringUtil.getStringContent(document.getPublisher().getPlace(),
                    DEFAULT_RO_CRATE_LANGUAGE)
            );
            publisherMetadata.setId(publisherIdentifier);

            metadataInfo.getGraph().add(publisherMetadata);
        }
    }

    private static String getIssn(PublicationSeries publicationSeries) {
        return StringUtil.valueExists(publicationSeries.getEISSN()) ?
            publicationSeries.getEISSN() : publicationSeries.getPrintISSN();
    }

    private static String getIsbn(Proceedings proceedings) {
        return StringUtil.valueExists(proceedings.getEISBN()) ?
            proceedings.getEISBN() : proceedings.getPrintISBN();
    }

    private static RoCrateEvent getEventMetadata(Conference conference, RoCrate metadataInfo,
                                                 boolean checkSuperEvents, boolean checkSubEvents) {
        var event = new RoCrateEvent();

        event.setId(getEventIdentifier(conference));
        if (StringUtil.valueExists(conference.getConfId())) {
            event.setIdentifier(conference.getConfId());
        }

        event.setName(
            StringUtil.getStringContent(conference.getName(), DEFAULT_RO_CRATE_LANGUAGE));
        event.setStartDate(conference.getDateFrom());
        event.setEndDate(conference.getDateTo());

        if (Objects.nonNull(conference.getCountry())) {
            event.setLocation(
                StringUtil.getStringContent(conference.getCountry().getName(),
                    DEFAULT_RO_CRATE_LANGUAGE));
        }

        if (Objects.nonNull(conference.getKeywords())) {
            event.setKeywords(StringUtil.getStringContent(conference.getKeywords(),
                DEFAULT_RO_CRATE_LANGUAGE));
        }

        if (Objects.nonNull(conference.getUris()) && !conference.getUris().isEmpty()) {
            event.setUrl(conference.getUris().stream().findFirst().get());
        }

        if (!conference.getSerialEvent()) {
            if (checkSuperEvents) {
                eventsRelationRepository.getSuperEventsForEvent(conference.getId())
                    .forEach(superEvent -> {
                        var eventUnproxied = Hibernate.unproxy(superEvent, Event.class);

                        event.getSuperEvents().add(
                            new ContextualEntity(getEventIdentifier((Conference) eventUnproxied)));
                        metadataInfo.getGraph()
                            .add(getEventMetadata((Conference) eventUnproxied, metadataInfo, true,
                                false));
                    });
            }

            if (checkSubEvents) {
                eventsRelationRepository.getSubEventsForEvent(conference.getId())
                    .forEach(subEvent -> {
                        var eventUnproxied = Hibernate.unproxy(subEvent, Event.class);

                        event.getSubEvents().add(
                            new ContextualEntity(getEventIdentifier((Conference) eventUnproxied)));
                        metadataInfo.getGraph()
                            .add(getEventMetadata((Conference) eventUnproxied, metadataInfo, false,
                                true));
                    });
            }
        }

        return event;
    }

    private static String getEventIdentifier(Conference conference) {
        if (StringUtil.valueExists(conference.getConfId())) {
            return "https://confid.org/conf/" + conference.getConfId();
        } else {
            return baseUrl + "en/events/conference/" + conference.getId();
        }
    }

    private static String getOrganizationUnitIdentifier(OrganisationUnit organisationUnit) {
        if (StringUtil.valueExists(organisationUnit.getRor())) {
            return "https://ror.org/" + organisationUnit.getRor();
        } else {
            return baseUrl + "en/organisation-units/" + organisationUnit.getId();
        }
    }

    public static String getPersonIdentifier(Person person) {
        if (StringUtil.valueExists(person.getOrcid())) {
            return "https://orcid.org/" + person.getOrcid();
        } else if (StringUtil.valueExists(person.getScopusAuthorId())) {
            return "https://www.scopus.com/authid/detail.uri?authorId=" +
                person.getScopusAuthorId();
        } else if (StringUtil.valueExists(person.getOpenAlexId())) {
            return "https://openalex.org/" + person.getOpenAlexId();
        } else if (StringUtil.valueExists(person.getWebOfScienceResearcherId())) {
            return "http://www.researcherid.com/rid/" + person.getWebOfScienceResearcherId();
        } else if (StringUtil.valueExists(person.getECrisId())) {
            return "https://bib.cobiss.net/biblioweb/biblio/sr/scr/cris/" + person.getECrisId();
        } else {
            return baseUrl + "en/persons/" + person.getId();
        }
    }

    private static String getFileIdentifier(DocumentFile documentFile) {
        return baseUrl + "api/file/" + documentFile.getServerFilename();
    }

    private static String getLicenseUrl(License license) {
        return "https://spdx.org/licenses/CC-" + license.name().replace("_", "-") + "-4.0";
    }

    @Value("${export.base.url}")
    public void setBaseUrl(String baseUrl) {
        var hasEndSlash = baseUrl.endsWith("/");
        RoCrateConverter.baseUrl = baseUrl + (hasEndSlash ? "" : "/");
    }

    @Value("${export.include-rocrate-file-hashes}")
    public void setIncludeFileHashes(Boolean includeFileHashes) {
        RoCrateConverter.includeFileHashes = includeFileHashes;
    }

    @Autowired
    public void setFileService(FileService fileService) {
        RoCrateConverter.fileService = fileService;
    }

    @Autowired
    public void setEventsRelationRepository(EventsRelationRepository eventsRelationRepository) {
        RoCrateConverter.eventsRelationRepository = eventsRelationRepository;
    }
}
